package com.walyson.pdfexcelai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.walyson.pdfexcelai.config.AiProperties;
import com.walyson.pdfexcelai.model.ExtractedRow;
import com.walyson.pdfexcelai.model.PdfDocumentSnapshot;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class AiExtractionService {

    private static final String CONTENT_FIELD = "content";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;
    private final String apiUrl;
    private final String apiKey;
    private final String model;

    public AiExtractionService(
            ObjectMapper objectMapper,
            AiProperties aiProperties
    ) {
        this.objectMapper = objectMapper;
        this.apiUrl = aiProperties.apiUrl();
        this.apiKey = aiProperties.apiKey();
        this.model = aiProperties.model();
    }

    public boolean isConfigured() {
        return StringUtils.hasText(apiUrl) && StringUtils.hasText(apiKey) && StringUtils.hasText(model);
    }

    public List<ExtractedRow> extractRows(PdfDocumentSnapshot snapshot) {
        if (!isConfigured()) {
            return heuristicRows(snapshot.rawText());
        }

        try {
            String payload = objectMapper.writeValueAsString(
                objectMapper.createObjectNode()
                    .put("model", model)
                    .set("messages", objectMapper.createArrayNode()
                        .add(objectMapper.createObjectNode()
                            .put("role", "system")
                            .put(CONTENT_FIELD, "Analisa o PDF inteiro, infere automaticamente o tipo de documento e extrai os dados de negocio relevantes. Usa o texto extraido e as imagens das paginas. Devolve apenas JSON valido no formato [{\"reference\":\"\",\"description\":\"\",\"amount\":\"\",\"date\":\"\",\"notes\":\"\"}]. Se o documento nao tiver valores monetarios, usa campos vazios em amount. Se nao houver referencia explicita, cria uma referencia curta baseada na linha. Nunca devolvas markdown nem texto fora do JSON."))
                        .add(objectMapper.createObjectNode()
                            .put("role", "user")
                            .set(CONTENT_FIELD, buildUserContent(snapshot))))
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            String response = restTemplate.postForObject(apiUrl, new HttpEntity<>(payload, headers), String.class);
            if (!StringUtils.hasText(response)) {
                return heuristicRows(snapshot.rawText());
            }

            JsonNode contentNode = objectMapper.readTree(response)
                    .path("choices")
                    .path(0)
                    .path("message")
                    .path(CONTENT_FIELD);

            String rawContent = extractAssistantContent(contentNode);

            if (!StringUtils.hasText(rawContent)) {
                return heuristicRows(snapshot.rawText());
            }

            JsonNode rowsNode = objectMapper.readTree(cleanJson(rawContent));
            List<ExtractedRow> rows = new ArrayList<>();
            for (JsonNode row : rowsNode) {
                rows.add(new ExtractedRow(
                        row.path("reference").asText(""),
                        row.path("description").asText(""),
                        row.path("amount").asText(""),
                        row.path("date").asText(""),
                        row.path("notes").asText("")
                ));
            }
            return rows.isEmpty() ? heuristicRows(snapshot.rawText()) : rows;
        } catch (IOException | RestClientException ex) {
            return heuristicRows(snapshot.rawText());
        }
    }

    private JsonNode buildUserContent(PdfDocumentSnapshot snapshot) {
        var content = objectMapper.createArrayNode();
        content.add(objectMapper.createObjectNode()
                .put("type", "text")
                .put("text", buildPromptText(snapshot)));

        for (String pageImage : snapshot.pageImages()) {
            content.add(objectMapper.createObjectNode()
                    .put("type", "image_url")
                    .set("image_url", objectMapper.createObjectNode()
                            .put("url", pageImage)
                            .put("detail", "high")));
        }

        return content;
    }

    private String buildPromptText(PdfDocumentSnapshot snapshot) {
        StringBuilder builder = new StringBuilder();
        builder.append("Nome do trabalho: extracao estruturada de PDF.\n");
        builder.append("Paginas: ").append(snapshot.pageCount()).append("\n");
        builder.append("Texto extraido disponivel: ").append(snapshot.textAvailable() ? "sim" : "nao").append("\n");
        builder.append("Se o layout for tabela, fatura, extrato, relatorio, recibo ou formulario, adapta o mapeamento automaticamente.\n");
        builder.append("Prioriza linhas uteis para Excel e ignora cabecalhos repetidos, rodapes e ruido visual.\n\n");
        builder.append("Texto extraido do PDF:\n");
        if (snapshot.textAvailable()) {
            builder.append(limit(snapshot.rawText(), 15000));
        } else {
            builder.append("Sem texto selecionavel. Usa principalmente as imagens das paginas.");
        }
        return builder.toString();
    }

    private String extractAssistantContent(JsonNode contentNode) {
        if (contentNode.isTextual()) {
            return contentNode.asText();
        }

        if (contentNode.isArray()) {
            StringBuilder builder = new StringBuilder();
            for (JsonNode part : contentNode) {
                if (part.path("type").asText().equals("text")) {
                    builder.append(part.path("text").asText(""));
                }
            }
            return builder.toString();
        }

        return "";
    }

    private String cleanJson(String rawContent) {
        String cleaned = rawContent.trim();
        if (cleaned.startsWith("```") && cleaned.endsWith("```")) {
            cleaned = cleaned.replaceFirst("^```(?:json)?", "").replaceFirst("```$", "").trim();
        }
        return cleaned;
    }

    private String limit(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private List<ExtractedRow> heuristicRows(String rawText) {
        List<ExtractedRow> rows = new ArrayList<>();
        if (!StringUtils.hasText(rawText)) {
            rows.add(new ExtractedRow(
                    "SEM-TEXTO",
                    "O PDF nao tinha texto selecionavel. Configure a IA para usar as imagens renderizadas das paginas.",
                    "",
                    "",
                    "Fallback local sem OCR"
            ));
            return rows;
        }

        String[] lines = rawText.split("\\R");

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (!line.isBlank() && line.length() >= 10) {
                rows.add(new ExtractedRow(
                        "LINHA-" + (rows.size() + 1),
                        line,
                        "",
                        "",
                        "Extraido sem IA configurada"
                ));
            }

            if (rows.size() == 20) {
                break;
            }
        }

        return rows;
    }
}