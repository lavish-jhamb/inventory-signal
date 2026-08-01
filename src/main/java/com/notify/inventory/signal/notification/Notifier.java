package com.notify.inventory.signal.notification;

import com.notify.inventory.signal.provider.StockCheckResult;

/** Delivers a stock-availability notification. Implementations pick the channel (console, Telegram, ...). */
public interface Notifier {

	void notify(StockCheckResult result);
}
