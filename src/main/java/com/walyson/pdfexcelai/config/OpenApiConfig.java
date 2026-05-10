package com.walyson.pdfexcelai.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI pdfExcelAiOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("PDF Excel AI API")
                        .version("v1")
                        .description("API para extrair dados de PDFs/CSVs bancarios e exportar para Excel."))
                .servers(List.of(new Server()
                        .url("/")
                        .description("Current host")));
    }
}
