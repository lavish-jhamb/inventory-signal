package com.notify.inventory.signal.notification;

import com.notify.inventory.signal.provider.StockCheckResult;
import com.notify.inventory.signal.tracking.TrackedProduct;

/** Shared message text for a stock alert, used by every {@link Notifier} implementation. */
public final class StockAlertMessage {

	private StockAlertMessage() {
	}

	public static String format(StockCheckResult result) {
		TrackedProduct product = result.product();
		String siteLabel = product.site().toUpperCase();

		return """
				🚨 %s STOCK ALERT 🚀

				📦 %s
				✅ Status  : IN STOCK
				📍 Pincode : %s
				🛒 Buy now : %s""".formatted(siteLabel, product.name(), result.pincode(), product.url());
	}
}
