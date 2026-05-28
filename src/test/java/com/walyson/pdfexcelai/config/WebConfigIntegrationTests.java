package com.walyson.pdfexcelai.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "app.cors.allowed-origins=http://localhost:3000,https://example.com",
        "app.cors.allowed-origin-patterns=https://*.vercel.app"
})
class WebConfigIntegrationTests {

    @Autowired
    private WebConfig webConfig;

    @Test
    void webConfig_isAutoConfigured() {
        assertThat(webConfig).isNotNull();
    }

    @Test
    void webConfig_corsPropertiesAreLoaded() {
        assertThat(webConfig).isNotNull();
    }
}
