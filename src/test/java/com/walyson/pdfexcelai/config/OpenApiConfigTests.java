package com.walyson.pdfexcelai.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigTests {

    @Test
    void openApiConfig_canBeCreated() {
        OpenApiConfig config = new OpenApiConfig();
        assertThat(config).isNotNull();
    }

    @Test
    void openApiConfig_pdfExcelAiOpenApi_createsValidOpenAPI() {
        OpenApiConfig config = new OpenApiConfig();
        OpenAPI openAPI = config.pdfExcelAiOpenApi();

        assertThat(openAPI).isNotNull();
    }

    @Test
    void openApiConfig_pdfExcelAiOpenApi_hasCorrectInfo() {
        OpenApiConfig config = new OpenApiConfig();
        OpenAPI openAPI = config.pdfExcelAiOpenApi();
        Info info = openAPI.getInfo();

        assertThat(info).isNotNull();
        assertThat(info.getTitle()).isEqualTo("PDF Excel AI API");
        assertThat(info.getVersion()).isEqualTo("v1");
        assertThat(info.getDescription()).contains("extrair dados de PDFs/CSVs");
    }

    @Test
    void openApiConfig_pdfExcelAiOpenApi_hasServers() {
        OpenApiConfig config = new OpenApiConfig();
        OpenAPI openAPI = config.pdfExcelAiOpenApi();
        List<Server> servers = openAPI.getServers();

        assertThat(servers).isNotNull();
        assertThat(servers).isNotEmpty();
    }

    @Test
    void openApiConfig_pdfExcelAiOpenApi_serverPointsToCurrentHost() {
        OpenApiConfig config = new OpenApiConfig();
        OpenAPI openAPI = config.pdfExcelAiOpenApi();
        List<Server> servers = openAPI.getServers();

        assertThat(servers).hasSize(1);
        Server server = servers.get(0);
        assertThat(server.getUrl()).isEqualTo("/");
        assertThat(server.getDescription()).isEqualTo("Current host");
    }

    @Test
    void openApiConfig_multipleCallsCreateDifferentInstances() {
        OpenApiConfig config = new OpenApiConfig();
        OpenAPI openAPI1 = config.pdfExcelAiOpenApi();
        OpenAPI openAPI2 = config.pdfExcelAiOpenApi();

        assertThat(openAPI1).isNotSameAs(openAPI2);
    }

    @Test
    void openApiConfig_openApiContainsApiTitle() {
        OpenApiConfig config = new OpenApiConfig();
        OpenAPI openAPI = config.pdfExcelAiOpenApi();

        assertThat(openAPI.getInfo().getTitle()).contains("PDF", "Excel", "AI");
    }

    @Test
    void openApiConfig_openApiDescriptionIsInPortuguese() {
        OpenApiConfig config = new OpenApiConfig();
        OpenAPI openAPI = config.pdfExcelAiOpenApi();

        assertThat(openAPI.getInfo().getDescription()).contains("API");
        assertThat(openAPI.getInfo().getDescription()).contains("Excel");
    }
}
