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
		String body = notifier.formBody("123456789", "Croma Stock ALERT");

		assertThat(body).contains("chat_id=123456789");
		assertThat(body).contains("text=Croma+Stock+ALERT");
		assertThat(body).contains("disable_web_page_preview=true");
	}
}
