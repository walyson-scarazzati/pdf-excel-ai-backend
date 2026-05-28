package com.walyson.pdfexcelai.config;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigurationPropertiesComprehensiveTests {

    @Test
    void aiProperties_allFieldsUsed() {
        AiProperties props = new AiProperties(
                "provider", "https://api.example.com", "key123",
                "model-name", "2024-01-01"
        );

        assertThat(props)
                .satisfies(p -> {
                    assertThat(p.provider()).isEqualTo("provider");
                    assertThat(p.apiUrl()).isEqualTo("https://api.example.com");
                    assertThat(p.apiKey()).isEqualTo("key123");
                    assertThat(p.model()).isEqualTo("model-name");
                    assertThat(p.githubApiVersion()).isEqualTo("2024-01-01");
                });
    }

    @Test
    void corsProperties_allFieldsUsed() {
        List<String> origins = List.of("http://localhost");
        List<String> patterns = List.of("https://*.app");

        CorsProperties props = new CorsProperties(origins, patterns);

        assertThat(props)
                .satisfies(p -> {
                    assertThat(p.allowedOrigins()).isEqualTo(origins);
                    assertThat(p.allowedOriginPatterns()).isEqualTo(patterns);
                });
    }

    @Test
    void ocrProperties_allFieldsUsed() {
        OcrProperties props = new OcrProperties(
                true, "tesseract", "mode", "pt", 3, 30, 100,
                1000000L, true, 1.2, 128, 2, true, 30.0,
                0.1, 0.1, 0.05
        );

        assertThat(props)
                .satisfies(p -> {
                    assertThat(p.enabled()).isTrue();
                    assertThat(p.command()).isEqualTo("tesseract");
                    assertThat(p.mode()).isEqualTo("mode");
                    assertThat(p.language()).isEqualTo("pt");
                    assertThat(p.pageSegmentationMode()).isEqualTo(3);
                    assertThat(p.timeoutSeconds()).isEqualTo(30);
                    assertThat(p.maxPages()).isEqualTo(100);
                    assertThat(p.maxImagePixels()).isEqualTo(1000000L);
                    assertThat(p.preprocessEnabled()).isTrue();
                    assertThat(p.contrastFactor()).isEqualTo(1.2);
                    assertThat(p.threshold()).isEqualTo(128);
                    assertThat(p.upscaleFactor()).isEqualTo(2);
                    assertThat(p.deskewEnabled()).isTrue();
                    assertThat(p.maxDeskewAngle()).isEqualTo(30.0);
                    assertThat(p.cropTopRatio()).isEqualTo(0.1);
                    assertThat(p.cropBottomRatio()).isEqualTo(0.1);
                    assertThat(p.cropSideRatio()).isEqualTo(0.05);
                });
    }

    @Test
    void ocrProperties_disabled() {
        OcrProperties props = new OcrProperties(
                false, "cmd", "mode", "lang", 3, 30, 100, 1000000L,
                false, 1.0, 127, 1, false, 0.0, 0.0, 0.0, 0.0
        );

        assertThat(props.enabled()).isFalse();
        assertThat(props.preprocessEnabled()).isFalse();
        assertThat(props.deskewEnabled()).isFalse();
    }
}
