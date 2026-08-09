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
		assertThat(trackingProperties.pincodes()).containsExactly("125050");
		assertThat(trackingProperties.products()).hasSize(2);

		TrackedProduct iphone15 = trackingProperties.products().get(0);
		assertThat(iphone15.site()).isEqualTo("croma");
		assertThat(iphone15.itemId()).isEqualTo("300652");
		assertThat(iphone15.name()).isEqualTo("Apple iPhone 15 (128GB, Black)");
		assertThat(iphone15.url()).isEqualTo("https://www.croma.com/apple-iphone-15-128gb-black-/p/300652");
	}
}
