package com.notify.inventory.signal.provider;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Bound from application.yaml (app.connector.*) - shared HTTP/anti-block tuning for all StockProviders. */
@ConfigurationProperties(prefix = "app.connector")
public record ConnectorProperties(
		int requestTimeoutSeconds, long rateLimitDelayMs, int maxConsecutiveFails, int cooldownMinutes) {

	public Duration requestTimeout() {
		return Duration.ofSeconds(requestTimeoutSeconds);
	}

	public Duration cooldown() {
		return Duration.ofMinutes(cooldownMinutes);
	}
}
