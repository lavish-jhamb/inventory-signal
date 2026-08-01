package com.notify.inventory.signal.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.notify.inventory.signal.notification.Notifier;
import com.notify.inventory.signal.provider.StockCheckResult;
import com.notify.inventory.signal.provider.StockProvider;
import com.notify.inventory.signal.tracking.TrackedProduct;
import com.notify.inventory.signal.tracking.TrackingProperties;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import org.junit.jupiter.api.Test;

class StockCheckServiceTest {

	private static final TrackedProduct PRODUCT = new TrackedProduct("croma", "317577", "iPhone 17e", null);
	private static final TrackingProperties PROPERTIES =
			new TrackingProperties(List.of("400049"), List.of(PRODUCT));

	@Test
	void notifiesOnlyWhenProductTransitionsToInStock() {
		Deque<Boolean> resultsInOrder = new ArrayDeque<>(List.of(true, true, false, true));
		List<StockCheckResult> notified = new ArrayList<>();

		StockProvider fakeProvider = new StockProvider() {
			@Override
			public String siteName() {
				return "croma";
			}

			@Override
			public StockCheckResult checkAvailability(TrackedProduct product, String pincode) {
				return new StockCheckResult(product, pincode, resultsInOrder.poll(), "detail");
			}
		};
		Notifier recordingNotifier = notified::add;

		StockCheckService service = new StockCheckService(PROPERTIES, List.of(fakeProvider), recordingNotifier);

		service.checkAll(); // available=true -> transition, should notify
		service.checkAll(); // available=true again -> no transition, should NOT notify
		service.checkAll(); // available=false -> should NOT notify
		service.checkAll(); // available=true -> transition again, should notify

		assertThat(notified).hasSize(2);
	}

	@Test
	void skipsProductsWithNoMatchingProvider() {
		StockCheckService service = new StockCheckService(PROPERTIES, List.of(), result -> {
			throw new AssertionError("notifier should not be called when no provider matches");
		});

		service.checkAll();
	}
}
