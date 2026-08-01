package com.notify.inventory.signal.service;

import com.notify.inventory.signal.notification.Notifier;
import com.notify.inventory.signal.provider.StockCheckResult;
import com.notify.inventory.signal.provider.StockProvider;
import com.notify.inventory.signal.tracking.TrackedProduct;
import com.notify.inventory.signal.tracking.TrackingProperties;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
	private final Map<String, StockProvider> providersBySite;
	private final Notifier notifier;

	// key = site:itemId:pincode -> last known availability
	private final Map<String, Boolean> lastKnownAvailability = new ConcurrentHashMap<>();

	public StockCheckService(TrackingProperties trackingProperties, List<StockProvider> providers, Notifier notifier) {
		this.trackingProperties = trackingProperties;
		this.providersBySite = providers.stream().collect(Collectors.toMap(StockProvider::siteName, p -> p));
		this.notifier = notifier;
	}

	@Scheduled(initialDelayString = "${app.scheduler.initial-delay-ms}", fixedDelayString = "${app.scheduler.fixed-delay-ms}")
	public void checkAll() {
		for (TrackedProduct product : trackingProperties.products()) {
			List<String> pincodes = product.pincodes() != null ? product.pincodes() : trackingProperties.pincodes();
			for (String pincode : pincodes) {
				checkOne(product, pincode);
			}
		}
	}

	private void checkOne(TrackedProduct product, String pincode) {
		StockProvider provider = providersBySite.get(product.site());
		if (provider == null) {
			log.warn("No StockProvider registered for site '{}' (product '{}')", product.site(), product.name());
			return;
		}

		try {
			StockCheckResult result = provider.checkAvailability(product, pincode);
			handleResult(product, pincode, result);
		} catch (Exception e) {
			log.error("Stock check failed for '{}' at pincode {}: {}", product.name(), pincode, e.getMessage());
		}
	}

	private void handleResult(TrackedProduct product, String pincode, StockCheckResult result) {
		String key = product.site() + ":" + product.itemId() + ":" + pincode;
		Boolean wasAvailable = lastKnownAvailability.put(key, result.available());

		log.debug("Checked {} at {} -> available={} ({})", product.name(), pincode, result.available(), result.detail());

		boolean justBecameAvailable = result.available() && !Boolean.TRUE.equals(wasAvailable);
		if (justBecameAvailable) {
			notifier.notify(result);
		}
	}
}
