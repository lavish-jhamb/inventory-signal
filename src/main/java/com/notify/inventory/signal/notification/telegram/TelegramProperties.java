package com.notify.inventory.signal.notification.telegram;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Bound from application.yaml (telegram.*). */
@ConfigurationProperties(prefix = "telegram")
public record TelegramProperties(String botToken, String ownerChatId, String apiBaseUrl) {
}
