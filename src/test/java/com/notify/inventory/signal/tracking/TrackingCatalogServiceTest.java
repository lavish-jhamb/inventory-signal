package com.notify.inventory.signal.tracking;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class TrackingCatalogServiceTest {

	private static final CatalogProperties CATALOG_PROPERTIES = new CatalogProperties(false, "test", "test", "main", "products.yaml");

	@Test
	void parsesPincodesAndProductsFromYaml() {
		TrackingCatalogService service =
				new TrackingCatalogService(new TrackingProperties(List.of(), List.of()), CATALOG_PROPERTIES);

		String yaml = """
				app:
				  tracking:
				    pincodes:
				      - "400049"
				      - "110001"
				    products:
				      - site: croma
				        itemId: "317577"
				        name: "iPhone 17e"
				        url: "https://www.croma.com/apple-iphone-17e-512gb-black-/p/317577"
				""";

		TrackingProperties parsed = service.parse(yaml);

		assertThat(parsed.pincodes()).containsExactly("400049", "110001");
		assertThat(parsed.products()).hasSize(1);
		TrackedProduct product = parsed.products().get(0);
		assertThat(product.site()).isEqualTo("croma");
		assertThat(product.itemId()).isEqualTo("317577");
		assertThat(product.name()).isEqualTo("iPhone 17e");
		assertThat(product.pincodes()).isNull();
	}

	@Test
	void refreshIsNoOpWhenDisabled() {
		TrackingProperties initial = new TrackingProperties(List.of("400049"), List.of());
		TrackingCatalogService service = new TrackingCatalogService(initial, CATALOG_PROPERTIES);

		service.refresh();

		assertThat(service.current()).isSameAs(initial);
	}
}
