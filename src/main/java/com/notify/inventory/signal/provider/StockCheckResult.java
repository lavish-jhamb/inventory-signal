package com.notify.inventory.signal.provider;

import com.notify.inventory.signal.tracking.TrackedProduct;

// error=true means the check itself failed (blocked/timeout/unexpected shape), not that the product is out of stock
public record StockCheckResult(TrackedProduct product, String pincode, boolean available, String detail, boolean error) {
}
