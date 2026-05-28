package com.walyson.pdfexcelai.controller;

import com.walyson.pdfexcelai.config.AiProperties;
import com.walyson.pdfexcelai.service.OcrService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthControllerExtendedTests {

    @Mock private AiProperties aiProperties;
    @Mock private OcrService ocrService;

    private HealthController controller;

    @BeforeEach
    void setUp() {
        controller = new HealthController(aiProperties, ocrService);
    }

    @Test
    void health_returnsOk_whenFullyConfigured() {
        when(aiProperties.provider()).thenReturn("github-models");
        when(aiProperties.apiUrl()).thenReturn("https://api.example.com");
        when(aiProperties.apiKey()).thenReturn("valid-key");
        when(aiProperties.model()).thenReturn("gpt-4");
        when(ocrService.isEnabled()).thenReturn(true);
        when(ocrService.isAvailable()).thenReturn(true);
        when(ocrService.commandName()).thenReturn("tesseract");

        ResponseEntity<Map<String, Object>> response = controller.health();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("status", "ok");
        assertThat((List<?>) response.getBody().get("problems")).isEmpty();
    }

    @Test
    void health_returnsServiceUnavailable_whenMissingConfiguration() {
        when(aiProperties.provider()).thenReturn("");
        when(aiProperties.apiUrl()).thenReturn("");
        when(aiProperties.model()).thenReturn("");
        when(aiProperties.apiKey()).thenReturn("");
        when(ocrService.isEnabled()).thenReturn(false);
        when(ocrService.isAvailable()).thenReturn(false);
        when(ocrService.commandName()).thenReturn("unknown");

        ResponseEntity<Map<String, Object>> response = controller.health();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).containsEntry("status", "not_ok");
        assertThat((List<?>) response.getBody().get("problems")).isNotEmpty();
    }

    @Test
    void health_reportsProblem_whenProviderIsInvalid() {
        when(aiProperties.provider()).thenReturn("invalid-provider");
        when(aiProperties.apiUrl()).thenReturn("https://api.example.com");
        when(aiProperties.apiKey()).thenReturn("key");
        when(aiProperties.model()).thenReturn("model");
        when(ocrService.isEnabled()).thenReturn(false);
        when(ocrService.isAvailable()).thenReturn(false);
        when(ocrService.commandName()).thenReturn("tesseract");

        ResponseEntity<Map<String, Object>> response = controller.health();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat((List<?>) response.getBody().get("problems")).isNotEmpty();
    }

    @Test
    void health_reportsProblem_whenApiKeyIsPlaceholder() {
        when(aiProperties.provider()).thenReturn("openai");
        when(aiProperties.apiUrl()).thenReturn("https://api.openai.com");
            when(aiProperties.apiKey()).thenReturn("sk-your-key");
        when(aiProperties.model()).thenReturn("gpt-4");
        when(ocrService.isEnabled()).thenReturn(false);
        when(ocrService.isAvailable()).thenReturn(false);
        when(ocrService.commandName()).thenReturn("tesseract");

        ResponseEntity<Map<String, Object>> response = controller.health();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void health_reportsProblem_whenOcrEnabledButNotAvailable() {
        when(aiProperties.provider()).thenReturn("openai");
        when(aiProperties.apiUrl()).thenReturn("https://api.openai.com");
        when(aiProperties.apiKey()).thenReturn("valid-key");
        when(aiProperties.model()).thenReturn("gpt-4");
        when(ocrService.isEnabled()).thenReturn(true);
        when(ocrService.isAvailable()).thenReturn(false);
        when(ocrService.commandName()).thenReturn("nonexistent");

        ResponseEntity<Map<String, Object>> response = controller.health();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat((List<?>) response.getBody().get("problems"))
                .isNotEmpty();
    }

    @Test
    void health_includesProviderInResponse() {
        when(aiProperties.provider()).thenReturn("github-models");
        when(aiProperties.apiUrl()).thenReturn("https://api.example.com");
        when(aiProperties.apiKey()).thenReturn("key");
        when(aiProperties.model()).thenReturn("model");
        when(ocrService.isEnabled()).thenReturn(false);
        when(ocrService.isAvailable()).thenReturn(false);
        when(ocrService.commandName()).thenReturn("cmd");

        ResponseEntity<Map<String, Object>> response = controller.health();

        assertThat(response.getBody()).containsEntry("provider", "github-models");
    }

    @Test
    void health_includesOcrStatus() {
        when(aiProperties.provider()).thenReturn("openai");
        when(aiProperties.apiUrl()).thenReturn("https://api.openai.com");
        when(aiProperties.apiKey()).thenReturn("key");
        when(aiProperties.model()).thenReturn("model");
        when(ocrService.isEnabled()).thenReturn(true);
        when(ocrService.isAvailable()).thenReturn(true);
        when(ocrService.commandName()).thenReturn("tesseract");

        ResponseEntity<Map<String, Object>> response = controller.health();

        Map<String, Object> body = response.getBody();
        assertThat(body).containsEntry("ocrEnabled", true);
        assertThat(body).containsEntry("ocrAvailable", true);
        assertThat(body).containsEntry("ocrCommand", "tesseract");
    }
}
