package com.notify.inventory.signal.tracking;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Bound from products.yaml (app.tracking.*). */
@ConfigurationProperties(prefix = "app.tracking")
public record TrackingProperties(List<String> pincodes, List<TrackedProduct> products) {
}
