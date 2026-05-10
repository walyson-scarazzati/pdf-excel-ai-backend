package com.walyson.pdfexcelai.controller;

import com.walyson.pdfexcelai.config.AiProperties;
import com.walyson.pdfexcelai.service.OcrService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
@Tag(name = "Health", description = "Status da aplicacao e configuracao de OCR/IA")
public class HealthController {

    private static final String PROVIDER_FIELD = "provider";

    private final AiProperties aiProperties;
    private final OcrService ocrService;

    public HealthController(AiProperties aiProperties, OcrService ocrService) {
        this.aiProperties = aiProperties;
        this.ocrService = ocrService;
    }

    @GetMapping
    @Operation(summary = "Check application health")
    @ApiResponse(responseCode = "200", description = "Aplicacao pronta")
    @ApiResponse(responseCode = "503", description = "Configuracao incompleta ou dependencia indisponivel")
    public ResponseEntity<Map<String, Object>> health() {
        boolean aiConfigured = isAiConfigured();
        boolean ocrEnabled = ocrService.isEnabled();
        boolean ocrAvailable = ocrService.isAvailable();
        List<String> problems = validateConfiguration(aiConfigured, ocrEnabled, ocrAvailable);
        boolean healthy = problems.isEmpty();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", healthy ? "ok" : "not_ok");
        body.put(PROVIDER_FIELD, aiProperties.provider());
        body.put("aiConfigured", aiConfigured);
        body.put("ocrEnabled", ocrEnabled);
        body.put("ocrAvailable", ocrAvailable);
        body.put("ocrCommand", ocrService.commandName());
        body.put("problems", problems);

        return ResponseEntity.status(healthy ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    private List<String> validateConfiguration(boolean aiConfigured, boolean ocrEnabled, boolean ocrAvailable) {
        LinkedHashMap<String, String> checks = new LinkedHashMap<>();

        if (!StringUtils.hasText(aiProperties.provider())) {
            checks.put(PROVIDER_FIELD, "AI provider is missing");
        } else {
            String provider = aiProperties.provider().trim().toLowerCase(Locale.ROOT);
            if (!provider.equals("openai") && !provider.equals("github-models")) {
                checks.put(PROVIDER_FIELD, "AI provider must be openai or github-models");
            }
        }

        if (!aiConfigured) {
            if (!StringUtils.hasText(aiProperties.apiUrl())) {
                checks.put("apiUrl", "AI API URL is missing");
            }

            if (!StringUtils.hasText(aiProperties.model())) {
                checks.put("model", "AI model is missing");
            }

            if (!StringUtils.hasText(aiProperties.apiKey())) {
                checks.put("apiKey", "AI API key is missing");
            } else if (looksLikePlaceholder(aiProperties.apiKey())) {
                checks.put("apiKey", "AI API key is still using a placeholder value");
            }
        }

        if (ocrEnabled && !ocrAvailable) {
            checks.put("ocr", "OCR is enabled but the local tesseract command is not available");
        }

        if (!aiConfigured && !ocrAvailable) {
            checks.put("runtime", "Neither AI nor local OCR is ready for robust extraction");
        }

        return List.copyOf(checks.values());
    }

    private boolean isAiConfigured() {
        if (!StringUtils.hasText(aiProperties.provider())) {
            return false;
        }

        String provider = aiProperties.provider().trim().toLowerCase(Locale.ROOT);
        if (!provider.equals("openai") && !provider.equals("github-models")) {
            return false;
        }

        if (!StringUtils.hasText(aiProperties.apiUrl()) || !StringUtils.hasText(aiProperties.model())) {
            return false;
        }

        return StringUtils.hasText(aiProperties.apiKey()) && !looksLikePlaceholder(aiProperties.apiKey());
    }

    private boolean looksLikePlaceholder(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.contains("coloca_aqui")
                || normalized.contains("substitui")
                || normalized.contains("your-token")
                || normalized.contains("your-key");
    }
}
