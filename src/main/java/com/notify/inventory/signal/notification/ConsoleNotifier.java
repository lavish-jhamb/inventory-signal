package com.notify.inventory.signal.notification;

import com.notify.inventory.signal.provider.StockCheckResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** MVP notifier: logs to the console. Runs alongside other Notifier implementations (e.g. Telegram). */
@Component
public class ConsoleNotifier implements Notifier {

	private static final Logger log = LoggerFactory.getLogger(ConsoleNotifier.class);

	@Override
	public void notify(StockCheckResult result) {
		log.info("\n{}", StockAlertMessage.format(result));
	}
}
