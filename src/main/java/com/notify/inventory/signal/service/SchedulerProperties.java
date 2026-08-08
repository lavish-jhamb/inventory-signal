package com.notify.inventory.signal.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Bound from application.yaml (app.scheduler.*) - polling cadence, incl. random jitter to avoid a bot-detectable fixed pattern. */
@ConfigurationProperties(prefix = "app.scheduler")
public record SchedulerProperties(long initialDelayMs, long fixedDelayMs, long jitterMaxMs) {
}
