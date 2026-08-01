package com.notify.inventory.signal.provider;

import com.notify.inventory.signal.tracking.TrackedProduct;

/** A per-site stock/availability checker. Implementations are looked up by siteName(). */
public interface StockProvider {

	String siteName();

	StockCheckResult checkAvailability(TrackedProduct product, String pincode);
}
