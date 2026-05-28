package com.walyson.pdfexcelai.config;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CorsPropertiesTests {

    @Test
    void corsProperties_canBeCreated() {
        CorsProperties props = new CorsProperties(
                List.of("http://localhost:3000", "https://example.com"),
                List.of("https://*.vercel.app")
        );

        assertThat(props.allowedOrigins()).hasSize(2);
        assertThat(props.allowedOriginPatterns()).hasSize(1);
        assertThat(props.allowedOrigins()).contains("http://localhost:3000");
        assertThat(props.allowedOriginPatterns()).contains("https://*.vercel.app");
    }

    @Test
    void corsProperties_recordContract() {
        List<String> origins = List.of("http://localhost");
        List<String> patterns = List.of("https://*");
        CorsProperties props1 = new CorsProperties(origins, patterns);
        CorsProperties props2 = new CorsProperties(origins, patterns);

        assertThat(props1).isEqualTo(props2);
        assertThat(props1.hashCode()).isEqualTo(props2.hashCode());
    }

    @Test
    void corsProperties_withNullFields() {
        CorsProperties props = new CorsProperties(null, null);

        assertThat(props.allowedOrigins()).isNull();
        assertThat(props.allowedOriginPatterns()).isNull();
    }

    @Test
    void corsProperties_withEmptyLists() {
        CorsProperties props = new CorsProperties(List.of(), List.of());

        assertThat(props.allowedOrigins()).isEmpty();
        assertThat(props.allowedOriginPatterns()).isEmpty();
    }

    @Test
    void corsProperties_withMultipleOrigins() {
        List<String> origins = List.of(
                "http://localhost:3000",
                "http://localhost:4200",
                "https://app.example.com"
        );
        CorsProperties props = new CorsProperties(origins, List.of());

        assertThat(props.allowedOrigins()).hasSize(3);
        assertThat(props.allowedOrigins()).containsAll(origins);
    }

    @Test
    void corsProperties_withMultiplePatterns() {
        List<String> patterns = List.of(
                "https://*.vercel.app",
                "https://*.run.app",
                "https://*.herokuapp.com"
        );
        CorsProperties props = new CorsProperties(List.of(), patterns);

        assertThat(props.allowedOriginPatterns()).hasSize(3);
        assertThat(props.allowedOriginPatterns()).containsAll(patterns);
    }

    @Test
    void corsProperties_toString() {
        CorsProperties props = new CorsProperties(
                List.of("http://localhost"),
                List.of("https://*")
        );

        assertThat(props.toString()).contains("localhost", "https://");
    }
}
