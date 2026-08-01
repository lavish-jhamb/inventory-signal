package com.notify.inventory.signal.provider;

import com.notify.inventory.signal.tracking.TrackedProduct;

public record StockCheckResult(TrackedProduct product, String pincode, boolean available, String detail) {
}
