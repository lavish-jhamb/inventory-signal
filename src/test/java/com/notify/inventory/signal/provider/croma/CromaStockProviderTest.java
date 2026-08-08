package com.notify.inventory.signal.provider.croma;

import static org.assertj.core.api.Assertions.assertThat;

import com.notify.inventory.signal.provider.ConnectorProperties;
import com.notify.inventory.signal.provider.StockCheckResult;
import com.notify.inventory.signal.tracking.TrackedProduct;
import org.junit.jupiter.api.Test;

class CromaStockProviderTest {

	private final CromaStockProvider provider = new CromaStockProvider(new ConnectorProperties(10, 0, 10, 30));
	private final TrackedProduct product = new TrackedProduct("croma", "317577", "iPhone 17e 512GB Black",
			"https://www.croma.com/apple-iphone-17e-512gb-black-/p/317577", null);

	@Test
	void inStockResponseIsParsedAsAvailable() {
		// Real shape captured from the live API: nested under promise.suggestedOption.*
		String responseBody = """
				{
				  "promise": {
				    "suggestedOption": {
				      "option": {
				        "promiseLines": {
				          "promiseLine": [
				            {
				              "itemID": "317577",
				              "carrierServiceCode": "BlueDart - 7 Day Frieght",
				              "assignments": {
				                "assignment": [
				                  {
				                    "zipCode": "400049",
				                    "deliveryDate": "2026-08-03T14:26:57.338+00:00"
				                  }
				                ]
				              }
				            }
				          ]
				        }
				      },
				      "unavailableLines": { "unavailableLine": [] }
				    }
				  }
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
				  "promise": {
				    "suggestedOption": {
				      "option": { "promiseLines": { "promiseLine": [] } },
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
