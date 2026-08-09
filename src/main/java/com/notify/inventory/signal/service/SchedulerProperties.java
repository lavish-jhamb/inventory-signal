package com.notify.inventory.signal.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Bound from application.yaml (app.scheduler.*) - random jitter added on top of @Scheduled's fixed interval to avoid a bot-detectable fixed pattern. */
@ConfigurationProperties(prefix = "app.scheduler")
public record SchedulerProperties(long jitterMaxMs) {
}
