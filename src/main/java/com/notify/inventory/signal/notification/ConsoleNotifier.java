package com.notify.inventory.signal.notification;

import com.notify.inventory.signal.provider.StockCheckResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** MVP notifier: logs to the console. Swap/add a TelegramNotifier later behind the same interface. */
@Component
public class ConsoleNotifier implements Notifier {

	private static final Logger log = LoggerFactory.getLogger(ConsoleNotifier.class);

	@Override
	public void notify(StockCheckResult result) {
		log.info("✅ {} is back in stock at pincode {} ({})",
				result.product().name(), result.pincode(), result.detail());
	}
}
