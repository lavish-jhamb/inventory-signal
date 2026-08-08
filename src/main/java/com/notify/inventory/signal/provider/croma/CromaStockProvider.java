package com.notify.inventory.signal.provider.croma;

import com.notify.inventory.signal.provider.ConnectorProperties;
import com.notify.inventory.signal.provider.StockCheckResult;
import com.notify.inventory.signal.provider.StockProvider;
import com.notify.inventory.signal.tracking.TrackedProduct;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Checks delivery availability via Croma's inventory "promise" API (details-pwa). */
@Component
public class CromaStockProvider implements StockProvider {

	private static final Logger log = LoggerFactory.getLogger(CromaStockProvider.class);

	private static final String SITE = "croma";
	private static final String ENDPOINT = "https://api.croma.com/inventory/oms/v2/tms/details-pwa/";
	private static final String SUBSCRIPTION_KEY =
			System.getenv().getOrDefault("CROMA_OMS_APIM_KEY", "1131858141634e2abe2efb2b3a2a2a5d");
	// Croma is behind Akamai bot management, which blocks/challenges known datacenter and
	// cloud-provider IP ranges (Oracle Cloud, AWS, GCP, etc.) far more aggressively than
	// residential IPs, independent of request headers. If deployed there, route this call
	// through a residential/forward proxy via these env vars to work around the IP block.
	private static final String PROXY_HOST = System.getenv("CROMA_PROXY_HOST");
	private static final String PROXY_PORT = System.getenv("CROMA_PROXY_PORT");

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

	private final ConnectorProperties connectorProperties;
	private final HttpClient httpClient;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public CromaStockProvider(ConnectorProperties connectorProperties) {
		this.connectorProperties = connectorProperties;
		this.httpClient = buildHttpClient(connectorProperties.requestTimeout());
	}

	private static HttpClient buildHttpClient(Duration timeout) {
		HttpClient.Builder builder = HttpClient.newBuilder().connectTimeout(timeout);
		if (PROXY_HOST != null && !PROXY_HOST.isBlank() && PROXY_PORT != null && !PROXY_PORT.isBlank()) {
			builder.proxy(ProxySelector.of(new InetSocketAddress(PROXY_HOST, Integer.parseInt(PROXY_PORT))));
		}
		return builder.build();
	}

	@Override
	public String siteName() {
		return SITE;
	}

	@Override
	public StockCheckResult checkAvailability(TrackedProduct product, String pincode) {
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(ENDPOINT))
				.timeout(connectorProperties.requestTimeout())
				.header("Content-Type", "application/json")
				.header("Accept", "application/json, text/plain, */*")
				.header("Accept-Language", "en-IN,en-US;q=0.9,en;q=0.8")
				.header("Origin", "https://www.croma.com")
				.header("Referer", "https://www.croma.com/")
				.header("Sec-Fetch-Site", "same-site")
				.header("Sec-Fetch-Mode", "cors")
				.header("Sec-Fetch-Dest", "empty")
				.header("User-Agent",
						"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
								+ "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
				.header("oms-apim-subscription-key", SUBSCRIPTION_KEY)
				.POST(HttpRequest.BodyPublishers.ofString(REQUEST_TEMPLATE.formatted(product.itemId(), pincode)))
				.build();

		try {
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() != 200) {
				// Likely an Akamai bot-management block on the source IP (common for cloud/
				// datacenter ASNs such as Oracle Cloud) rather than a malformed request - log
				// the body so a real auth/shape problem can be told apart from an IP block.
				log.warn("Croma details-pwa returned HTTP {} for item {} / pincode {}: {}",
						response.statusCode(), product.itemId(), pincode, truncate(response.body()));
				return new StockCheckResult(product, pincode, false, "HTTP " + response.statusCode(), true);
			}
			return parseAvailability(product, pincode, response.body());
		} catch (Exception e) {
			return new StockCheckResult(product, pincode, false, "request failed: " + e.getMessage(), true);
		}
	}

	private static String truncate(String body) {
		if (body == null) {
			return "";
		}
		return body.length() <= 500 ? body : body.substring(0, 500) + "...";
	}

	// package-private so CromaStockProviderTest can exercise it with saved sample responses
	StockCheckResult parseAvailability(TrackedProduct product, String pincode, String responseBody) {
		JsonNode root = objectMapper.readTree(responseBody);
		JsonNode suggestedOption = root.path("promise").path("suggestedOption");

		JsonNode unavailableLines = suggestedOption.path("unavailableLines").path("unavailableLine");
		if (unavailableLines.isArray() && !unavailableLines.isEmpty()) {
			String reason = unavailableLines.get(0).path("unavailableReason").asString("unknown reason");
			return new StockCheckResult(product, pincode, false, reason, false);
		}

		JsonNode promiseLines = suggestedOption.path("option").path("promiseLines").path("promiseLine");
		if (promiseLines.isArray() && !promiseLines.isEmpty()) {
			JsonNode line = promiseLines.get(0);
			String carrier = line.path("carrierServiceCode").asString("");
			JsonNode assignment = line.path("assignments").path("assignment").path(0);
			String deliveryDate = assignment.path("deliveryDate").asString("");
			return new StockCheckResult(product, pincode, true, "deliverable via %s, by %s".formatted(carrier, deliveryDate), false);
		}

		// unexpected shape (e.g. a bot-challenge page returned with HTTP 200) - flag as an error, not a real "out of stock"
		return new StockCheckResult(product, pincode, false, "unrecognized response shape", true);
	}
}
