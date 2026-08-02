package com.notify.inventory.signal.notification;

import com.notify.inventory.signal.provider.StockCheckResult;
import com.notify.inventory.signal.tracking.TrackedProduct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** MVP notifier: logs to the console. Swap/add a TelegramNotifier later behind the same interface. */
@Component
public class ConsoleNotifier implements Notifier {

	private static final Logger log = LoggerFactory.getLogger(ConsoleNotifier.class);

	@Override
	public void notify(StockCheckResult result) {
		TrackedProduct product = result.product();
		String siteLabel = product.site().substring(0, 1).toUpperCase() + product.site().substring(1);

		log.info("""

				{} Stock ALERT
				Product : {}
				Pincode : {}
				Buy now : {}""",
				siteLabel, product.name(), result.pincode(), product.url());
	}
}
