package com.notify.inventory.signal.tracking;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class TrackingPropertiesTest {

	@Autowired
	private TrackingProperties trackingProperties;

	@Test
	void loadsPincodesAndProductsFromProductsYaml() {
		assertThat(trackingProperties.pincodes()).containsExactly("400049", "110001");
		assertThat(trackingProperties.products()).hasSize(2);

		TrackedProduct iphone17e = trackingProperties.products().get(0);
		assertThat(iphone17e.site()).isEqualTo("croma");
		assertThat(iphone17e.itemId()).isEqualTo("317577");
		assertThat(iphone17e.name()).isEqualTo("iPhone 17e 512GB Black");
	}
}
