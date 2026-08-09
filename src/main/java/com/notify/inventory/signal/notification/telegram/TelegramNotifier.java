package com.notify.inventory.signal.notification.telegram;

import com.notify.inventory.signal.notification.Notifier;
import com.notify.inventory.signal.notification.StockAlertMessage;
import com.notify.inventory.signal.provider.ConnectorProperties;
import com.notify.inventory.signal.provider.StockCheckResult;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Sends stock alerts to a Telegram chat via the Bot API's sendMessage endpoint. */
@Component
public class TelegramNotifier implements Notifier {

	private static final Logger log = LoggerFactory.getLogger(TelegramNotifier.class);

	private final TelegramProperties properties;
	private final ConnectorProperties connectorProperties;
	private final HttpClient httpClient;

	public TelegramNotifier(TelegramProperties properties, ConnectorProperties connectorProperties) {
		this.properties = properties;
		this.connectorProperties = connectorProperties;
		this.httpClient = HttpClient.newBuilder().connectTimeout(connectorProperties.requestTimeout()).build();
	}

	@Override
	public void notify(StockCheckResult result) {
		String url = properties.apiBaseUrl() + "/bot" + properties.botToken() + "/sendMessage";
		String body = formBody(properties.ownerChatId(), StockAlertMessage.format(result));

		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.timeout(connectorProperties.requestTimeout())
				.header("Content-Type", "application/x-www-form-urlencoded")
				.POST(HttpRequest.BodyPublishers.ofString(body))
				.build();

		try {
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() != 200) {
				log.error("Telegram sendMessage failed: HTTP {} - {}", response.statusCode(), response.body());
			}
		} catch (Exception e) {
			log.error("Telegram sendMessage failed: {}", e.getMessage());
		}
	}

	// package-private so TelegramNotifierTest can assert the request shape without a real HTTP call
	String formBody(String chatId, String message) {
		return "chat_id=" + encode(chatId)
				+ "&text=" + encode(message)
				+ "&disable_web_page_preview=true";
	}

	private String encode(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}
}
