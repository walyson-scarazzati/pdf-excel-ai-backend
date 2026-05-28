package com.walyson.pdfexcelai.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class OpenApiConfigIntegrationTests {

    @Autowired
    private OpenAPI openAPI;

    @Test
    void openAPI_beanIsCreated() {
        assertThat(openAPI).isNotNull();
        assertThat(openAPI.getInfo()).isNotNull();
        assertThat(openAPI.getInfo().getTitle()).isEqualTo("PDF Excel AI API");
    }

    @Test
    void openAPI_hasServerConfiguration() {
        assertThat(openAPI.getServers()).isNotEmpty();
        assertThat(openAPI.getServers().get(0).getUrl()).isEqualTo("/");
    }
}
