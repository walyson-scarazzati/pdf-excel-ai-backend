package com.walyson.pdfexcelai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai")
public record AiProperties(
	String provider,
	String apiUrl,
	String apiKey,
	String model,
	String githubApiVersion
) {
}
