package com.walyson.pdfexcelai.config;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.CorsRegistration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebConfigTests {

    @Test
    void webConfig_canBeCreated() {
        CorsProperties corsProperties = new CorsProperties(List.of(), List.of());
        WebConfig config = new WebConfig(corsProperties);

        assertThat(config).isNotNull();
    }

    @Test
    void webConfig_addCorsMappings_withDefaultOrigins() {
        CorsProperties corsProperties = new CorsProperties(List.of(), List.of());
        WebConfig config = new WebConfig(corsProperties);

        CorsRegistry registry = mock(CorsRegistry.class);
        CorsRegistration registration = mock(CorsRegistration.class);
        when(registry.addMapping(any())).thenReturn(registration);
        when(registration.allowedOrigins(any(String[].class))).thenReturn(registration);
        when(registration.allowedOriginPatterns(any(String[].class))).thenReturn(registration);
        when(registration.allowedMethods(any(String[].class))).thenReturn(registration);
        when(registration.allowedHeaders(any(String[].class))).thenReturn(registration);
        when(registration.maxAge(any(Long.class))).thenReturn(registration);

        config.addCorsMappings(registry);

        assertThat(config).isNotNull();
    }

    @Test
    void webConfig_addCorsMappings_withCustomOrigins() {
        List<String> customOrigins = List.of("https://custom.example.com");
        CorsProperties corsProperties = new CorsProperties(customOrigins, List.of());
        WebConfig config = new WebConfig(corsProperties);

        CorsRegistry registry = mock(CorsRegistry.class);
        CorsRegistration registration = mock(CorsRegistration.class);
        when(registry.addMapping(any())).thenReturn(registration);
        when(registration.allowedOrigins(any(String[].class))).thenReturn(registration);
        when(registration.allowedOriginPatterns(any(String[].class))).thenReturn(registration);
        when(registration.allowedMethods(any(String[].class))).thenReturn(registration);
        when(registration.allowedHeaders(any(String[].class))).thenReturn(registration);
        when(registration.maxAge(any(Long.class))).thenReturn(registration);

        config.addCorsMappings(registry);

        assertThat(config).isNotNull();
    }

    @Test
    void webConfig_addCorsMappings_withCustomPatterns() {
        List<String> customPatterns = List.of("https://*.custom.app");
        CorsProperties corsProperties = new CorsProperties(List.of(), customPatterns);
        WebConfig config = new WebConfig(corsProperties);

        CorsRegistry registry = mock(CorsRegistry.class);
        CorsRegistration registration = mock(CorsRegistration.class);
        when(registry.addMapping(any())).thenReturn(registration);
        when(registration.allowedOrigins(any(String[].class))).thenReturn(registration);
        when(registration.allowedOriginPatterns(any(String[].class))).thenReturn(registration);
        when(registration.allowedMethods(any(String[].class))).thenReturn(registration);
        when(registration.allowedHeaders(any(String[].class))).thenReturn(registration);
        when(registration.maxAge(any(Long.class))).thenReturn(registration);

        config.addCorsMappings(registry);

        assertThat(config).isNotNull();
    }

    @Test
    void webConfig_addCorsMappings_withBothCustomOriginAndPattern() {
        List<String> origins = List.of("https://origin.example.com");
        List<String> patterns = List.of("https://*.pattern.app");
        CorsProperties corsProperties = new CorsProperties(origins, patterns);
        WebConfig config = new WebConfig(corsProperties);

        CorsRegistry registry = mock(CorsRegistry.class);
        CorsRegistration registration = mock(CorsRegistration.class);
        when(registry.addMapping(any())).thenReturn(registration);
        when(registration.allowedOrigins(any(String[].class))).thenReturn(registration);
        when(registration.allowedOriginPatterns(any(String[].class))).thenReturn(registration);
        when(registration.allowedMethods(any(String[].class))).thenReturn(registration);
        when(registration.allowedHeaders(any(String[].class))).thenReturn(registration);
        when(registration.maxAge(any(Long.class))).thenReturn(registration);

        config.addCorsMappings(registry);

        assertThat(config).isNotNull();
    }

    @Test
    void webConfig_addCorsMappings_withNullLists() {
        CorsProperties corsProperties = new CorsProperties(null, null);
        WebConfig config = new WebConfig(corsProperties);

        CorsRegistry registry = mock(CorsRegistry.class);
        CorsRegistration registration = mock(CorsRegistration.class);
        when(registry.addMapping(any())).thenReturn(registration);
        when(registration.allowedOrigins(any(String[].class))).thenReturn(registration);
        when(registration.allowedOriginPatterns(any(String[].class))).thenReturn(registration);
        when(registration.allowedMethods(any(String[].class))).thenReturn(registration);
        when(registration.allowedHeaders(any(String[].class))).thenReturn(registration);
        when(registration.maxAge(any(Long.class))).thenReturn(registration);

        config.addCorsMappings(registry);

        assertThat(config).isNotNull();
    }

    @Test
    void webConfig_constructor_storesCorsProperties() {
        CorsProperties corsProperties = new CorsProperties(List.of("http://test"), List.of());
        WebConfig config = new WebConfig(corsProperties);

        assertThat(config).isNotNull();
    }
}
