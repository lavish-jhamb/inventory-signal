package com.notify.inventory.signal.tracking;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

/**
 * Keeps the tracked product/pincode list in sync with products.yaml on GitHub, so edits made
 * through the CMS take effect without a redeploy - the Render build filter skips rebuilding on
 * that file's commits, so the classpath-bundled copy alone would otherwise go stale forever.
 */
@Service
public class TrackingCatalogService {

	private static final Logger log = LoggerFactory.getLogger(TrackingCatalogService.class);

	private final CatalogProperties catalogProperties;
	private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

	// starts as the classpath-bundled copy, then kept fresh via refresh(); never left null on fetch failure
	private volatile TrackingProperties current;

	public TrackingCatalogService(TrackingProperties initial, CatalogProperties catalogProperties) {
		this.current = initial;
		this.catalogProperties = catalogProperties;
	}

	public TrackingProperties current() {
		return current;
	}

	/** Re-fetches products.yaml from GitHub; keeps the previous list if the fetch or parse fails. */
	public void refresh() {
		if (!catalogProperties.enabled()) {
			return;
		}
		try {
			HttpRequest request = HttpRequest.newBuilder(URI.create(catalogProperties.rawUrl()))
					.timeout(Duration.ofSeconds(10))
					.GET()
					.build();
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() != 200) {
				log.warn("Catalog refresh failed: GitHub returned HTTP {}", response.statusCode());
				return;
			}
			current = parse(response.body());
		} catch (Exception e) {
			log.warn("Catalog refresh failed, keeping previous product list: {}", e.getMessage());
		}
	}

	@SuppressWarnings("unchecked")
	TrackingProperties parse(String yamlText) {
		Map<String, Object> root = new Yaml().load(yamlText);
		Map<String, Object> app = (Map<String, Object>) root.get("app");
		Map<String, Object> tracking = (Map<String, Object>) app.get("tracking");

		List<String> pincodes = (List<String>) tracking.get("pincodes");
		List<Map<String, Object>> rawProducts = (List<Map<String, Object>>) tracking.get("products");
		List<TrackedProduct> products = rawProducts.stream()
				.map(m -> new TrackedProduct((String) m.get("site"), (String) m.get("itemId"), (String) m.get("name"),
						(String) m.get("url"), (List<String>) m.get("pincodes")))
				.toList();

		return new TrackingProperties(pincodes, products);
	}
}
