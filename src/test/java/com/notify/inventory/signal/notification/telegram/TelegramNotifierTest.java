package com.notify.inventory.signal.notification.telegram;

import static org.assertj.core.api.Assertions.assertThat;

import com.notify.inventory.signal.provider.ConnectorProperties;
import org.junit.jupiter.api.Test;

class TelegramNotifierTest {

	private final TelegramNotifier notifier = new TelegramNotifier(
			new TelegramProperties("test-token", "123456789", "https://api.telegram.org"),
			new ConnectorProperties(10, 0, 10, 30));

	@Test
	void buildsSendMessageRequestBodyWithChatIdAndText() {
		String body = notifier.formBody("123456789", "Croma Stock ALERT", "https://www.croma.com/p/317577");

		assertThat(body).contains("chat_id=123456789");
		assertThat(body).contains("text=Croma+Stock+ALERT");
		assertThat(body).contains("reply_markup=");
		assertThat(body).doesNotContain("disable_web_page_preview");
	}

	@Test
	void buyNowButtonLinksToProductUrl() {
		String body = notifier.formBody("123456789", "Croma Stock ALERT", "https://www.croma.com/p/317577");
		String decodedMarkup = java.net.URLDecoder.decode(body.split("reply_markup=")[1], java.nio.charset.StandardCharsets.UTF_8);

		assertThat(decodedMarkup).contains("\"url\":\"https://www.croma.com/p/317577\"");
		assertThat(decodedMarkup).contains("BUY NOW");
	}
}
