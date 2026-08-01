package com.notify.inventory.signal.provider.croma;

import static org.assertj.core.api.Assertions.assertThat;

import com.notify.inventory.signal.provider.StockCheckResult;
import com.notify.inventory.signal.tracking.TrackedProduct;
import org.junit.jupiter.api.Test;

class CromaStockProviderTest {

	private final CromaStockProvider provider = new CromaStockProvider();
	private final TrackedProduct product = new TrackedProduct("croma", "317577", "iPhone 17e 512GB Black", null);

	@Test
	void inStockResponseIsParsedAsAvailable() {
		String responseBody = """
				{
				  "promiseLines": {
				    "promiseLine": [
				      {
				        "quantity": "1",
				        "deliveryDate": "2026-08-03T12:41:32.316+00:00",
				        "carrierServiceCode": "BlueDart - 7 Day Frieght",
				        "scac": "BLUE",
				        "zipCode": "400049",
				        "shipNode": "CROMA_WH_01"
				      }
				    ]
				  },
				  "unavailableLines": { "unavailableLine": [] }
				}
				""";

		StockCheckResult result = provider.parseAvailability(product, "400049", responseBody);

		assertThat(result.available()).isTrue();
		assertThat(result.detail()).contains("BlueDart - 7 Day Frieght");
	}

	@Test
	void outOfStockResponseIsParsedAsUnavailable() {
		String responseBody = """
				{
				  "promiseLines": { "promiseLine": [] },
				  "unavailableLines": {
				    "unavailableLine": [
				      {
				        "itemID": "317577",
				        "lineId": "1",
				        "unavailableReason": "NOT_ENOUGH_PRODUCT_CHOICES"
				      }
				    ]
				  }
				}
				""";

		StockCheckResult result = provider.parseAvailability(product, "400049", responseBody);

		assertThat(result.available()).isFalse();
		assertThat(result.detail()).isEqualTo("NOT_ENOUGH_PRODUCT_CHOICES");
	}

	@Test
	void unrecognizedResponseIsTreatedAsUnavailable() {
		StockCheckResult result = provider.parseAvailability(product, "400049", "{}");

		assertThat(result.available()).isFalse();
		assertThat(result.detail()).isEqualTo("unrecognized response shape");
	}

	@Test
	void siteNameIsCroma() {
		assertThat(provider.siteName()).isEqualTo("croma");
	}
}
