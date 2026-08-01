package com.notify.inventory.signal.provider.croma;

import com.notify.inventory.signal.provider.StockCheckResult;
import com.notify.inventory.signal.provider.StockProvider;
import com.notify.inventory.signal.tracking.TrackedProduct;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Checks delivery availability via Croma's inventory "promise" API (details-pwa). */
@Component
public class CromaStockProvider implements StockProvider {

	private static final String SITE = "croma";
	private static final String ENDPOINT = "https://api.croma.com/inventory/oms/v2/tms/details-pwa/";
	private static final String SUBSCRIPTION_KEY =
			System.getenv().getOrDefault("CROMA_OMS_APIM_KEY", "1131858141634e2abe2efb2b3a2a2a5d");

	// Only itemID and zipCode vary per check; the rest of this shape is fixed by Croma's API.
	private static final String REQUEST_TEMPLATE = """
			{
			  "promise": {
			    "allocationRuleID": "SYSTEM",
			    "checkInventory": "Y",
			    "organizationCode": "CROMA",
			    "sourcingClassification": "EC",
			    "promiseLines": {
			      "promiseLine": [
			        {
			          "fulfillmentType": "HDEL",
			          "mch": "",
			          "itemID": "%s",
			          "lineId": "1",
			          "categoryType": "mobile",
			          "reqEndDate": "2500-01-01",
			          "reqStartDate": "",
			          "requiredQty": "1",
			          "shipToAddress": {
			            "company": "",
			            "country": "",
			            "city": "",
			            "mobilePhone": "",
			            "state": "",
			            "zipCode": "%s",
			            "extn": { "irlAddressLine1": "", "irlAddressLine2": "" }
			          },
			          "extn": { "widerStoreFlag": "N" }
			        }
			      ]
			    }
			  }
			}
			""";

	private final HttpClient httpClient = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(10))
			.build();
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public String siteName() {
		return SITE;
	}

	@Override
	public StockCheckResult checkAvailability(TrackedProduct product, String pincode) {
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(ENDPOINT))
				.timeout(Duration.ofSeconds(10))
				.header("Content-Type", "application/json")
				.header("Accept", "application/json, text/plain, */*")
				.header("Referer", "https://www.croma.com/")
				.header("User-Agent", "Mozilla/5.0")
				.header("oms-apim-subscription-key", SUBSCRIPTION_KEY)
				.POST(HttpRequest.BodyPublishers.ofString(REQUEST_TEMPLATE.formatted(product.itemId(), pincode)))
				.build();

		try {
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() != 200) {
				return new StockCheckResult(product, pincode, false, "HTTP " + response.statusCode());
			}
			return parseAvailability(product, pincode, response.body());
		} catch (Exception e) {
			return new StockCheckResult(product, pincode, false, "request failed: " + e.getMessage());
		}
	}

	// package-private so CromaStockProviderTest can exercise it with saved sample responses
	StockCheckResult parseAvailability(TrackedProduct product, String pincode, String responseBody) {
		JsonNode root = objectMapper.readTree(responseBody);
		JsonNode suggestedOption = root.path("promise").path("suggestedOption");

		JsonNode unavailableLines = suggestedOption.path("unavailableLines").path("unavailableLine");
		if (unavailableLines.isArray() && !unavailableLines.isEmpty()) {
			String reason = unavailableLines.get(0).path("unavailableReason").asString("unknown reason");
			return new StockCheckResult(product, pincode, false, reason);
		}

		JsonNode promiseLines = suggestedOption.path("option").path("promiseLines").path("promiseLine");
		if (promiseLines.isArray() && !promiseLines.isEmpty()) {
			JsonNode line = promiseLines.get(0);
			String carrier = line.path("carrierServiceCode").asString("");
			JsonNode assignment = line.path("assignments").path("assignment").path(0);
			String deliveryDate = assignment.path("deliveryDate").asString("");
			return new StockCheckResult(product, pincode, true, "deliverable via %s, by %s".formatted(carrier, deliveryDate));
		}

		return new StockCheckResult(product, pincode, false, "unrecognized response shape");
	}
}
