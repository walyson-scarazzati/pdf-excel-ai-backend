package com.walyson.pdfexcelai;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "spring.flyway.enabled=false",
    "app.accounting.plan-import.enabled=false"
})
class PdfExcelAiApplicationTests {

    @Test
    void contextLoads() {
    }
}