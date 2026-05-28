package com.walyson.pdfexcelai.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.walyson.pdfexcelai.config.AiProperties;
import com.walyson.pdfexcelai.service.OcrService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class HealthControllerTests {

    private AiProperties aiProperties;
    private OcrService ocrService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        aiProperties = mock(AiProperties.class);
        ocrService = mock(OcrService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new HealthController(aiProperties, ocrService)).build();
    }

    @Test
    void returnsOkWhenFullyConfigured() throws Exception {
        when(aiProperties.provider()).thenReturn("openai");
        when(aiProperties.apiUrl()).thenReturn("https://api.openai.com/v1");
        when(aiProperties.model()).thenReturn("gpt-4o");
        when(aiProperties.apiKey()).thenReturn("sk-valid-key");
        when(ocrService.isEnabled()).thenReturn(false);
        when(ocrService.isAvailable()).thenReturn(false);
        when(ocrService.commandName()).thenReturn("tesseract");

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.aiConfigured").value(true));
    }

    @Test
    void returnsServiceUnavailableWhenNeitherAiNorOcrReady() throws Exception {
        when(aiProperties.provider()).thenReturn(null);
        when(aiProperties.apiUrl()).thenReturn(null);
        when(aiProperties.model()).thenReturn(null);
        when(aiProperties.apiKey()).thenReturn(null);
        when(ocrService.isEnabled()).thenReturn(false);
        when(ocrService.isAvailable()).thenReturn(false);
        when(ocrService.commandName()).thenReturn("tesseract");

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("not_ok"))
                .andExpect(jsonPath("$.aiConfigured").value(false));
    }

    @Test
    void returnsOkWhenOcrIsAvailableEvenWithoutAi() throws Exception {
        when(aiProperties.provider()).thenReturn(null);
        when(aiProperties.apiUrl()).thenReturn(null);
        when(aiProperties.model()).thenReturn(null);
        when(aiProperties.apiKey()).thenReturn(null);
        when(ocrService.isEnabled()).thenReturn(true);
        when(ocrService.isAvailable()).thenReturn(true);
        when(ocrService.commandName()).thenReturn("tesseract");

        mockMvc.perform(get("/api/health"))
                .andExpect(jsonPath("$.ocrEnabled").value(true))
                .andExpect(jsonPath("$.ocrAvailable").value(true));
    }

    @Test
    void reportsProblemWhenOcrEnabledButNotAvailable() throws Exception {
        when(aiProperties.provider()).thenReturn("openai");
        when(aiProperties.apiUrl()).thenReturn("https://api.openai.com/v1");
        when(aiProperties.model()).thenReturn("gpt-4o");
        when(aiProperties.apiKey()).thenReturn("sk-valid-key");
        when(ocrService.isEnabled()).thenReturn(true);
        when(ocrService.isAvailable()).thenReturn(false);
        when(ocrService.commandName()).thenReturn("tesseract");

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.problems").isArray());
    }

    @Test
    void detectsPlaceholderApiKey() throws Exception {
        when(aiProperties.provider()).thenReturn("github-models");
        when(aiProperties.apiUrl()).thenReturn("https://models.inference.ai.azure.com");
        when(aiProperties.model()).thenReturn("gpt-4o");
        when(aiProperties.apiKey()).thenReturn("coloca_aqui_seu_token");
        when(ocrService.isEnabled()).thenReturn(false);
        when(ocrService.isAvailable()).thenReturn(false);
        when(ocrService.commandName()).thenReturn("tesseract");

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.aiConfigured").value(false));
    }
}
