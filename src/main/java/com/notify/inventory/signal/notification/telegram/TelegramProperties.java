package com.notify.inventory.signal.notification.telegram;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Bound from application.yaml (telegram.*). pollingTimeoutSeconds is reserved for future inbound-command support. */
@ConfigurationProperties(prefix = "telegram")
public record TelegramProperties(String botToken, String ownerChatId, int pollingTimeoutSeconds, String apiBaseUrl) {
}
