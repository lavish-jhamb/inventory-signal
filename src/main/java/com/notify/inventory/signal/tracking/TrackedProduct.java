package com.notify.inventory.signal.tracking;

import java.util.List;

/** A product to monitor. Site must match a {@code StockProvider}'s siteName(). */
public record TrackedProduct(String site, String itemId, String name, List<String> pincodes) {
}
