package com.notify.inventory.signal.tracking;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Where to fetch the live products.yaml from GitHub, so catalog edits don't need a redeploy. */
@ConfigurationProperties(prefix = "app.catalog")
public record CatalogProperties(boolean enabled, String repoOwner, String repoName, String branch, String path) {

	public String rawUrl() {
		return "https://raw.githubusercontent.com/%s/%s/%s/%s".formatted(repoOwner, repoName, branch, path);
	}
}
