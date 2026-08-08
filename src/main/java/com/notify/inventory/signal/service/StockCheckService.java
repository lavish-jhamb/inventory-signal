package com.notify.inventory.signal.service;

import com.notify.inventory.signal.notification.Notifier;
import com.notify.inventory.signal.provider.ConnectorProperties;
import com.notify.inventory.signal.provider.StockCheckResult;
import com.notify.inventory.signal.provider.StockProvider;
import com.notify.inventory.signal.tracking.TrackedProduct;
import com.notify.inventory.signal.tracking.TrackingProperties;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/** Polls every tracked product/pincode and notifies only when a product transitions to in stock. */
@Service
public class StockCheckService {

	private static final Logger log = LoggerFactory.getLogger(StockCheckService.class);

	private final TrackingProperties trackingProperties;
	private final SchedulerProperties schedulerProperties;
	private final ConnectorProperties connectorProperties;
	private final Map<String, StockProvider> providersBySite;
	private final List<Notifier> notifiers;

	// key = site:itemId:pincode -> last known availability
	private final Map<String, Boolean> lastKnownAvailability = new ConcurrentHashMap<>();
	// circuit breaker state, keyed the same way, so a blocked/broken product+pincode stops being hammered
	private final Map<String, Integer> consecutiveFails = new ConcurrentHashMap<>();
	private final Map<String, Instant> cooldownUntil = new ConcurrentHashMap<>();

	public StockCheckService(TrackingProperties trackingProperties, SchedulerProperties schedulerProperties,
			ConnectorProperties connectorProperties, List<StockProvider> providers, List<Notifier> notifiers) {
		this.trackingProperties = trackingProperties;
		this.schedulerProperties = schedulerProperties;
		this.connectorProperties = connectorProperties;
		this.providersBySite = providers.stream().collect(Collectors.toMap(StockProvider::siteName, p -> p));
		this.notifiers = notifiers;
	}

	@Scheduled(initialDelayString = "${app.scheduler.initial-delay-ms}", fixedDelayString = "${app.scheduler.fixed-delay-ms}")
	public void checkAll() {
		// random jitter so every cycle doesn't fire at an identical, bot-detectable offset
		if (!sleepQuietly(ThreadLocalRandom.current().nextLong(schedulerProperties.jitterMaxMs() + 1))) {
			return;
		}

		boolean first = true;
		for (TrackedProduct product : trackingProperties.products()) {
			List<String> pincodes = product.pincodes() != null ? product.pincodes() : trackingProperties.pincodes();
			for (String pincode : pincodes) {
				// space out requests within a cycle instead of firing them back-to-back
				if (!first && !sleepQuietly(connectorProperties.rateLimitDelayMs())) {
					return;
				}
				first = false;
				checkOne(product, pincode);
			}
		}
	}

	private void checkOne(TrackedProduct product, String pincode) {
		String key = product.site() + ":" + product.itemId() + ":" + pincode;

		Instant cooldown = cooldownUntil.get(key);
		if (cooldown != null) {
			if (Instant.now().isBefore(cooldown)) {
				log.debug("Skipping {} at {} - circuit breaker cooling down until {}", product.name(), pincode, cooldown);
				return;
			}
			cooldownUntil.remove(key);
		}

		StockProvider provider = providersBySite.get(product.site());
		if (provider == null) {
			log.warn("No StockProvider registered for site '{}' (product '{}')", product.site(), product.name());
			return;
		}

		try {
			StockCheckResult result = provider.checkAvailability(product, pincode);
			recordOutcome(key, product, pincode, result.error());
			handleResult(key, result);
		} catch (Exception e) {
			recordOutcome(key, product, pincode, true);
			log.error("Stock check failed for '{}' at pincode {}: {}", product.name(), pincode, e.getMessage());
		}
	}

	// trips a per-key cooldown after repeated failures so we stop hammering a blocked/broken endpoint
	private void recordOutcome(String key, TrackedProduct product, String pincode, boolean failed) {
		if (!failed) {
			consecutiveFails.remove(key);
			return;
		}
		int fails = consecutiveFails.merge(key, 1, Integer::sum);
		if (fails >= connectorProperties.maxConsecutiveFails()) {
			Instant until = Instant.now().plus(connectorProperties.cooldown());
			cooldownUntil.put(key, until);
			consecutiveFails.remove(key);
			log.warn("Circuit breaker tripped for {} at {} after {} consecutive failures - pausing until {}",
					product.name(), pincode, fails, until);
		}
	}

	private void handleResult(String key, StockCheckResult result) {
		if (result.error()) {
			// connector-level failure, not real inventory data - don't let it clobber known availability state
			return;
		}

		Boolean wasAvailable = lastKnownAvailability.put(key, result.available());

		log.debug("Checked {} at {} -> available={} ({})", result.product().name(), result.pincode(), result.available(), result.detail());

		boolean justBecameAvailable = result.available() && !Boolean.TRUE.equals(wasAvailable);
		if (justBecameAvailable) {
			for (Notifier notifier : notifiers) {
				try {
					notifier.notify(result);
				} catch (Exception e) {
					log.error("Notifier {} failed: {}", notifier.getClass().getSimpleName(), e.getMessage());
				}
			}
		}
	}

	// returns false (and preserves the interrupt flag) if interrupted, so callers can bail out of the cycle early
	private static boolean sleepQuietly(long millis) {
		if (millis <= 0) {
			return true;
		}
		try {
			Thread.sleep(millis);
			return true;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		}
	}
}

