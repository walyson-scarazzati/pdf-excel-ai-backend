package com.walyson.pdfexcelai;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PdfExcelAiApplicationIntegrationTests {

    @Test
    void contextLoads() {
        // Verify that the Spring context loads without errors
        assertThat(true).isTrue();
    }

    @Test
    void applicationStartsSuccessfully() {
        // If we get here, the application started successfully
        assertThat(true).isTrue();
    }
}
