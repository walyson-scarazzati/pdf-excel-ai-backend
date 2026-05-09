package com.walyson.pdfexcelai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.walyson.pdfexcelai.config.AiProperties;
import com.walyson.pdfexcelai.model.ExtractedRow;
import com.walyson.pdfexcelai.model.PdfDocumentSnapshot;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static com.walyson.pdfexcelai.service.AiPromptConstants.BANK_STATEMENT_EXTRACTION_PROMPT;

@Service
public class AiExtractionService {

    private static final String CONTENT_FIELD = "content";
    private static final String PROVIDER_GITHUB_MODELS = "github-models";
    private static final String GITHUB_ACCEPT_HEADER = "application/vnd.github+json";
    private static final String GITHUB_API_VERSION_HEADER = "X-GitHub-Api-Version";
    private static final int MAX_PROMPT_TEXT_LENGTH = 120000;
    private static final int MAX_HEURISTIC_ROWS = 2000;
    private static final String ENTRY_TYPE_CREDIT = "CREDITO";
    private static final String ENTRY_TYPE_DEBIT = "DEBITO";
    private static final String ENTRY_TYPE_CREDIT_NORMALIZED = "credito";
    private static final String ENTRY_TYPE_DEBIT_NORMALIZED = "debito";
    private static final Pattern DATE_PATTERN = Pattern.compile("\\b\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}\\b");
    private static final Pattern BANK_DATE_PREFIX = Pattern.compile("^(\\d{2}/\\d{2})\\b");
    private static final Pattern OCR_LEADING_DATE_PATTERN = Pattern.compile("^(\\d{1,2})\\D+(\\d{1,2})(?:\\D+(\\d{2,4}))?.*");
        private static final Pattern RAW_OCR_LEADING_DATE_PATTERN = Pattern.compile(
            "^[\\W_]*[0-9oil!|zsbqd]{1,2}\\D+[0-9oil!|zsbqd]{1,2}(?:\\D+[0-9oil!|zsbqd]{2,4})?",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern TRANSACTION_SECTION_START = Pattern.compile("SALDO\\s+EM\\s+\\d{2}/\\d{2}",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern TRANSACTION_SECTION_END = Pattern.compile(
            "Cr[eé]ditos?\\s+Contratados?|Investimentos|Pacote\\s+de\\s+Servi[cç]os?|"
                    + "Programa\\s+de\\s+Relacionamento|D[eé]bito\\s+Autom[aá]tico|"
                    + "Saldos?\\s+por\\s+Per[ií]odo",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern GROUPED_AMOUNT_PATTERN = Pattern
            .compile("(?<!\\d)R?\\$?\\s*[+-]?\\d{1,3}(?:[.\\s]\\d{3})+,\\d{2}");
    private static final Pattern SIMPLE_AMOUNT_PATTERN = Pattern.compile("(?<!\\d)R?\\$?\\s*[+-]?\\d+[.,]\\d{2}");
    private static final Pattern REFERENCE_PATTERN = Pattern.compile("^[A-Z0-9][A-Z0-9./_-]{2,}$");
    private static final Pattern TRAILING_MINUS_GROUPED_PATTERN = Pattern.compile("(\\d{1,3}(?:[.]\\d{3})+,\\d{2})-");
    private static final Pattern TRAILING_MINUS_SIMPLE_PATTERN = Pattern.compile("(?<![\\d])(\\d+,\\d{2})-");
    private static final String[] CREDIT_KEYWORDS = {
            "estorno", "receb", "deposito", "deposit", ENTRY_TYPE_CREDIT_NORMALIZED, "entrada", "reembolso",
            "pix recebido", "transferencia recebida", "pagamento recebido"
    };
    private static final String[] DEBIT_KEYWORDS = {
            ENTRY_TYPE_DEBIT_NORMALIZED, "compra", "pagamento", "saque", "tarifa", "pix enviado",
            "transferencia enviada", "boleto"
    };

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;
    private final String provider;
    private final String apiUrl;
    private final String apiKey;
    private final String model;
    private final String githubApiVersion;

    public AiExtractionService(ObjectMapper objectMapper, AiProperties aiProperties) {
        this.objectMapper = objectMapper;
        this.provider = aiProperties.provider();
        this.apiUrl = aiProperties.apiUrl();
        this.apiKey = aiProperties.apiKey();
        this.model = aiProperties.model();
        this.githubApiVersion = aiProperties.githubApiVersion();
    }

    public boolean isConfigured() {
        return StringUtils.hasText(apiUrl) && StringUtils.hasText(apiKey) && StringUtils.hasText(model);
    }

    public List<ExtractedRow> extractRows(PdfDocumentSnapshot snapshot) {
        if (!isConfigured()) {
            System.out.println("[DEBUG AI] IA nao configurada, usando heuristica");
            return heuristicRows(snapshot);
        }

        System.out.println("[DEBUG AI] Iniciando extracao com IA");
        System.out.println("[DEBUG AI] Provider: " + provider);
        System.out.println("[DEBUG AI] Model: " + model);
        System.out.println("[DEBUG AI] API URL: " + apiUrl);

        try {
            String payload = objectMapper.writeValueAsString(
                    objectMapper.createObjectNode()
                            .put("model", model)
                            .set("messages", objectMapper.createArrayNode()
                                    .add(objectMapper.createObjectNode()
                                            .put("role", "system")
                                            .put(CONTENT_FIELD, buildSystemPrompt()))
                                    .add(objectMapper.createObjectNode()
                                            .put("role", "user")
                                            .set(CONTENT_FIELD, buildUserContent(snapshot)))));

            System.out.println("[DEBUG AI] Tamanho do payload: " + payload.length() + " bytes");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            if (isGitHubModelsProvider()) {
                headers.set(HttpHeaders.ACCEPT, GITHUB_ACCEPT_HEADER);
                if (StringUtils.hasText(githubApiVersion)) {
                    headers.set(GITHUB_API_VERSION_HEADER, githubApiVersion);
                }
            }

            System.out.println("[DEBUG AI] Enviando requisicao para a IA...");
            String response = restTemplate.postForObject(apiUrl, new HttpEntity<>(payload, headers), String.class);
            
            if (!StringUtils.hasText(response)) {
                System.out.println("[DEBUG AI] Resposta vazia da IA, usando heuristica");
                return heuristicRows(snapshot);
            }

            System.out.println("[DEBUG AI] Resposta recebida, tamanho: " + response.length() + " bytes");

            JsonNode contentNode = objectMapper.readTree(response)
                    .path("choices")
                    .path(0)
                    .path("message")
                    .path(CONTENT_FIELD);

            String rawContent = extractAssistantContent(contentNode);
            if (!StringUtils.hasText(rawContent)) {
                System.out.println("[DEBUG AI] Conteudo extraido vazio");
                return heuristicRows(snapshot);
            }

            System.out.println("[DEBUG AI] Conteudo extraido: " + rawContent.substring(0, Math.min(500, rawContent.length())));

            JsonNode rowsNode = extractRowsNode(objectMapper.readTree(cleanJson(rawContent)));
            List<ExtractedRow> rows = new ArrayList<>();
            for (JsonNode row : rowsNode) {
                // Parse fields according to SYSTEM_PROMPT format
                String date = normalizeText(row.path("date").asText(""));
                String value = normalizeText(row.path("value").asText(""));
                String debit = normalizeText(row.path("debit").asText("")).replace("-", "").trim();
                String credit = normalizeText(row.path("credit").asText("")).replace("-", "").trim();
                String historyCode = normalizeText(row.path("historyCode").asText(""));
                String complement = normalizeText(row.path("complement").asText(""));

                ExtractedRow extractedRow = new ExtractedRow(date, value, debit, credit, historyCode, complement);

                if (hasMeaningfulContent(extractedRow)) {
                    rows.add(extractedRow);
                }
            }

            System.out.println("[DEBUG AI] Total de linhas extraidas: " + rows.size());

            return rows.isEmpty() ? heuristicRows(snapshot) : deduplicateRows(rows);
        } catch (IOException | RestClientException ex) {
            System.out.println("[ERRO AI] Erro ao processar: " + ex.getMessage());
            ex.printStackTrace();
            return heuristicRows(snapshot);
        }
    }

    private boolean isGitHubModelsProvider() {
        return PROVIDER_GITHUB_MODELS.equalsIgnoreCase(normalize(provider));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
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

    private String buildSystemPrompt() {
        return BANK_STATEMENT_EXTRACTION_PROMPT;
    }

    private String buildPromptText(PdfDocumentSnapshot snapshot) {
        List<ExtractedRow> candidateRows = heuristicRows(snapshot);
        StringBuilder builder = new StringBuilder();
        builder.append("DOCUMENTO TABULAR FINANCEIRO\n\n");
        
        builder.append("METADADOS:\n");
        builder.append("- Tipo: ").append(snapshot.sourceType()).append("\n");
        builder.append("- Paginas: ").append(snapshot.pageCount()).append("\n");
        builder.append("- Texto disponivel: ").append(snapshot.textAvailable() ? "sim" : "nao").append("\n");
        builder.append("- OCR usado: ").append(snapshot.ocrUsed() ? "sim" : "nao").append("\n\n");

        builder.append("ORIENTACAO:\n");
        builder.append("- Extraia linhas de lancamento financeiro.\n");
        builder.append("- Preserve colunas explicitas quando existirem.\n");
        builder.append("- Nao classifique contas contabeis. O sistema faz isso depois usando regras do banco de dados.\n");
        builder.append("- Se houver apenas indicador D/C, use-o apenas para distinguir debito e credito do lancamento.\n");
        builder.append("- Se um campo nao puder ser determinado com seguranca, deixe vazio.\n\n");

        builder.append("TEXTO EXTRAIDO:\n\n");

        if (StringUtils.hasText(snapshot.normalizedText())) {
            builder.append(limit(snapshot.normalizedText(), MAX_PROMPT_TEXT_LENGTH));
        } else {
            builder.append("Sem texto selecionavel. Analise as imagens das paginas fornecidas.");
        }

        if (!snapshot.pageTexts().isEmpty()) {
            builder.append("\n\nTEXTO POR PAGINA:\n\n");
            for (int pageIndex = 0; pageIndex < snapshot.pageTexts().size(); pageIndex++) {
                String pageText = snapshot.pageTexts().get(pageIndex);
                if (!StringUtils.hasText(pageText)) {
                    continue;
                }
                builder.append("\n[Pagina ").append(pageIndex + 1).append("]\n");
                builder.append(limit(pageText, 12000)).append("\n");
            }
        }

        if (!candidateRows.isEmpty()) {
            builder.append("\nPOSSIVEIS LINHAS JA DETECTADAS LOCALMENTE:\n");
            for (ExtractedRow row : candidateRows) {
                builder.append("- date=").append(row.date())
                        .append(" | value=").append(row.value())
                        .append(" | debit=").append(row.debit())
                        .append(" | credit=").append(row.credit())
                        .append(" | historyCode=").append(row.historyCode())
                        .append(" | complement=").append(row.complement())
                        .append("\n");
            }
        }

        return builder.toString();
    }

    private JsonNode extractRowsNode(JsonNode rootNode) {
        if (rootNode.isArray()) {
            return rootNode;
        }

        if (rootNode.isObject()) {
            for (String fieldName : List.of("rows", "items", "data", "result")) {
                JsonNode candidate = rootNode.path(fieldName);
                if (candidate.isArray()) {
                    return candidate;
                }
            }
        }

        return objectMapper.createArrayNode();
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

    private List<ExtractedRow> heuristicRows(PdfDocumentSnapshot snapshot) {
        String sourceText = StringUtils.hasText(snapshot.normalizedText()) ? snapshot.normalizedText()
                : snapshot.rawText();
        if (!StringUtils.hasText(sourceText)) {
            List<ExtractedRow> rows = new ArrayList<>();
            rows.add(new ExtractedRow(
                    "",
                    "O PDF nao tinha texto selecionavel. Configure a IA para usar as imagens renderizadas das paginas.",
                    "SEM-TEXTO",
                    "", "", ""));
            return rows;
        }

        String sectionText = extractTransactionSection(sourceText);
        String statementMonthYear = inferStatementMonthYear(sourceText);
        List<ExtractedRow> rows = collectHeuristicRows(sectionText, statementMonthYear);
        return rows.isEmpty() ? collectFallbackTextRows(sectionText, statementMonthYear) : deduplicateRows(rows);
    }

    private String extractTransactionSection(String sourceText) {
        Matcher startMatcher = TRANSACTION_SECTION_START.matcher(sourceText);
        if (!startMatcher.find()) {
            return sourceText;
        }
        int start = startMatcher.start();
        Matcher endMatcher = TRANSACTION_SECTION_END.matcher(sourceText);
        int end = sourceText.length();
        while (endMatcher.find()) {
            if (endMatcher.start() > start) {
                end = endMatcher.start();
                break;
            }
        }
        return sourceText.substring(start, end);
    }

    private List<ExtractedRow> collectHeuristicRows(String sourceText, String statementMonthYear) {
        List<ExtractedRow> rows = new ArrayList<>();
        List<String> lines = collectCandidateLines(sourceText, statementMonthYear);
        for (String line : lines) {
            ExtractedRow parsedRow = parseCandidateLine(line, rows.size() + 1, statementMonthYear);
            if (parsedRow != null) {
                rows.add(parsedRow);
            }
            if (rows.size() == MAX_HEURISTIC_ROWS) {
                break;
            }
        }

        return deduplicateRows(rows);
    }

    private List<ExtractedRow> collectFallbackTextRows(String sourceText, String statementMonthYear) {
        List<ExtractedRow> rows = new ArrayList<>();
        String[] rawLines = sourceText.split("\\R");
        for (String rawLine : rawLines) {
            String line = rawLine.trim();
            String normalizedLine = normalizeLeadingDate(line, statementMonthYear);
            String date = extractDate(normalizedLine, statementMonthYear);
            if (!line.isBlank() && line.length() >= 10 && StringUtils.hasText(date) && hasAmount(normalizedLine)) {
                rows.add(buildRow(
                        "",
                        "",
                        normalizedLine,
                        extractAmount(normalizedLine),
                        date,
                        ""));
            }

            if (rows.size() == MAX_HEURISTIC_ROWS) {
                break;
            }
        }
        return deduplicateRows(rows);
    }

    private List<String> collectCandidateLines(String sourceText, String statementMonthYear) {
        List<String> candidates = new ArrayList<>();
        String[] rawLines = sourceText.split("\\R");

        int index = 0;
        while (index < rawLines.length) {
            String line = rawLines[index].trim();
            if (isCandidateLine(line, statementMonthYear)) {
                LineMergeResult mergeResult = mergeCandidateLine(rawLines, index, line, statementMonthYear);
                candidates.add(mergeResult.line());
                index = mergeResult.nextIndex();
            }
            index++;
        }

        return candidates;
    }

    private boolean isCandidateLine(String line, String statementMonthYear) {
        String normalizedLine = normalizeLeadingDate(line, statementMonthYear);
        if (normalizedLine.isBlank() || normalizedLine.length() < 4 || isNoise(normalizedLine)) {
            return false;
        }

        return StringUtils.hasText(extractDate(normalizedLine, statementMonthYear)) && hasAmount(normalizedLine);
    }

    private LineMergeResult mergeCandidateLine(String[] rawLines, int index, String line, String statementMonthYear) {
        StringBuilder merged = new StringBuilder(line);
        int lastConsumed = index;

        for (int ahead = 1; ahead <= 3; ahead++) {
            int nextIdx = lastConsumed + 1;
            if (nextIdx >= rawLines.length)
                break;

            String nextLine = rawLines[nextIdx].trim();
            if (!StringUtils.hasText(nextLine) || nextLine.length() < 3 || isNoise(nextLine))
                break;
            // Stop if next line starts a new transaction.
            if (StringUtils.hasText(extractDate(nextLine, statementMonthYear)))
                break;

            boolean currentHasAmount = hasAmount(merged.toString());
            boolean nextHasAmount = hasAmount(nextLine);
            boolean nextHasDate = hasDate(nextLine);

            if (!currentHasAmount) {
                // Current line has no amount yet — keep merging to find the amount
                merged.append(" ").append(nextLine);
                lastConsumed = nextIdx;
                if (nextHasAmount)
                    break; // Amount found, stop
            } else if (!nextHasAmount && !nextHasDate && nextLine.length() <= 50) {
                // Current already has amount; next is a short continuation (e.g. "CONTRATO
                // 290000008380")
                merged.append(" ").append(nextLine);
                lastConsumed = nextIdx;
            } else {
                break;
            }
        }

        return new LineMergeResult(merged.toString(), lastConsumed);
    }

    private ExtractedRow parseCandidateLine(String line, int rowNumber, String statementMonthYear) {
        String normalizedLine = normalizeLeadingDate(line, statementMonthYear);
        String date = extractDate(normalizedLine, statementMonthYear);
        String amount = extractAmount(normalizedLine);
        String docNumber = extractReference(normalizedLine);
        String description = extractDescription(normalizedLine, docNumber, amount, date);

        // Skip pure continuation lines — no financial content
        if (!StringUtils.hasText(amount) && !StringUtils.hasText(date)) {
            return null;
        }

        if (!StringUtils.hasText(description) && !StringUtils.hasText(amount) && !StringUtils.hasText(date)) {
            return null;
        }

        return buildRow(StringUtils.hasText(docNumber) ? docNumber : "", "", description, amount, date, "");
    }

    private String extractReference(String line) {
        String[] columns = splitColumns(line);
        if (columns.length > 0 && looksLikeReference(columns[0])) {
            return columns[0];
        }

        String[] tokens = line.split("\\s+");
        for (String token : tokens) {
            if (looksLikeReference(token)) {
                return token;
            }
        }

        return "";
    }

    private String extractDescription(String line, String reference, String amount, String date) {
        String description = line;
        if (StringUtils.hasText(reference)) {
            description = description.replaceFirst("^" + Pattern.quote(reference) + "\\s*", "");
        }
        if (StringUtils.hasText(date)) {
            description = description.replace(date, " ");
        }
        if (StringUtils.hasText(amount)) {
            description = removeLastOccurrence(description, amount);
        }

        String[] columns = splitColumns(description);
        if (columns.length >= 2) {
            List<String> usefulColumns = new ArrayList<>();
            for (String column : columns) {
                String cleanedColumn = column.trim();
                if (!cleanedColumn.isBlank() && !cleanedColumn.equals(reference) && !cleanedColumn.equals(date)
                        && !cleanedColumn.equals(amount)) {
                    usefulColumns.add(cleanedColumn);
                }
            }
            if (!usefulColumns.isEmpty()) {
                return String.join(" | ", usefulColumns).trim();
            }
        }

        return description.replaceAll("\\s{2,}", " ").trim();
    }

    private String[] splitColumns(String line) {
        return line.split("\\s{2,}|\\s*;\\s*|\\s*\\|\\s*");
    }

    private String extractDate(String line, String statementMonthYear) {
        line = normalizeLeadingDate(line, statementMonthYear);
        Matcher strictStartDate = Pattern.compile("^(\\d{2}/\\d{2}/\\d{4})\\b").matcher(line);
        if (strictStartDate.find()) {
            return strictStartDate.group(1);
        }

        String leadingDay = extractLeadingDay(line);
        if (StringUtils.hasText(leadingDay) && StringUtils.hasText(statementMonthYear)) {
            return leadingDay + "/" + statementMonthYear;
        }

        Matcher bankDate = BANK_DATE_PREFIX.matcher(line);
        if (bankDate.find()) {
            return bankDate.group(1);
        }
        Matcher matcher = DATE_PATTERN.matcher(line);
        return matcher.find() ? matcher.group().trim() : "";
    }

    private String normalizeLeadingDate(String line, String statementMonthYear) {
        if (!StringUtils.hasText(line)) {
            return "";
        }

        Matcher rawMatcher = RAW_OCR_LEADING_DATE_PATTERN.matcher(line);
        if (!rawMatcher.find()) {
            return line;
        }

        String leadingDigits = extractLeadingDigits(line.substring(0, rawMatcher.end()));
        Integer day = null;
        Integer month = null;
        Integer year = null;

        if (leadingDigits.length() >= 8) {
            day = Integer.parseInt(leadingDigits.substring(0, 2));
            month = Integer.parseInt(leadingDigits.substring(2, 4));
            year = Integer.parseInt(leadingDigits.substring(leadingDigits.length() - 4));
        } else if (leadingDigits.length() >= 4) {
            day = Integer.parseInt(leadingDigits.substring(0, 2));
            month = Integer.parseInt(leadingDigits.substring(2, 4));
            year = parseOcrYear(null, statementMonthYear);
        }

        if (day == null || month == null) {
            String sanitized = sanitizeLeadingDateWindow(line);
            Matcher matcher = OCR_LEADING_DATE_PATTERN.matcher(sanitized);
            if (!matcher.matches()) {
                return line;
            }

            day = parseOcrNumber(matcher.group(1));
            month = parseOcrNumber(matcher.group(2));
            year = parseOcrYear(matcher.group(3), statementMonthYear);
        }
        if (day == null || month == null || day < 1 || day > 31 || month < 1 || month > 12) {
            return line;
        }

        String canonicalDate = year == null
                ? String.format(Locale.ROOT, "%02d/%02d", day, month)
                : String.format(Locale.ROOT, "%02d/%02d/%04d", day, month, year);

        return canonicalDate + line.substring(rawMatcher.end());
    }

    private String extractLeadingDigits(String value) {
        StringBuilder digits = new StringBuilder();
        for (char character : value.toCharArray()) {
            char mapped = mapDigitLikeChar(character);
            if (Character.isDigit(mapped)) {
                digits.append(mapped);
            }
        }
        return digits.toString();
    }

    private String sanitizeLeadingDateWindow(String line) {
        StringBuilder builder = new StringBuilder();
        int limit = Math.min(line.length(), 12);
        for (int index = 0; index < limit; index++) {
            char value = mapDigitLikeChar(line.charAt(index));
            if (Character.isDigit(value)) {
                builder.append(value);
            } else {
                builder.append('/');
            }
        }
        return builder.toString();
    }

    private Integer parseOcrNumber(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        StringBuilder digits = new StringBuilder();
        for (char character : value.toCharArray()) {
            char mapped = mapDigitLikeChar(character);
            if (Character.isDigit(mapped)) {
                digits.append(mapped);
            }
        }

        if (digits.isEmpty()) {
            return null;
        }

        return Integer.parseInt(digits.toString());
    }

    private Integer parseOcrYear(String rawYear, String statementMonthYear) {
        Integer parsedYear = parseOcrNumber(rawYear);
        if (parsedYear != null) {
            return parsedYear < 100 ? 2000 + parsedYear : parsedYear;
        }

        if (!StringUtils.hasText(statementMonthYear) || !statementMonthYear.contains("/")) {
            return null;
        }

        return Integer.parseInt(statementMonthYear.substring(statementMonthYear.indexOf('/') + 1));
    }

    private String extractLeadingDay(String line) {
        if (!StringUtils.hasText(line)) {
            return "";
        }

        StringBuilder digits = new StringBuilder();
        for (int index = 0; index < Math.min(line.length(), 6); index++) {
            char ch = line.charAt(index);
            char mapped = mapDigitLikeChar(ch);
            if (Character.isDigit(mapped)) {
                digits.append(mapped);
                if (digits.length() == 2) {
                    int day = Integer.parseInt(digits.toString());
                    if (day >= 1 && day <= 31) {
                        return String.format(Locale.ROOT, "%02d", day);
                    }
                    return "";
                }
            } else if (digits.length() > 0) {
                break;
            }
        }

        return "";
    }

    private String inferStatementMonthYear(String sourceText) {
        Matcher matcher = Pattern.compile("\\b(\\d{2})\\s*/\\s*(\\d{4})\\b").matcher(sourceText);
        if (matcher.find()) {
            return matcher.group(1) + "/" + matcher.group(2);
        }
        return "";
    }

    private char mapDigitLikeChar(char value) {
        return switch (Character.toLowerCase(value)) {
            case 'o', 'q', 'd' -> '0';
            case 'i', 'l', '|', '!' -> '1';
            case 'z' -> '2';
            case 's', '$' -> '5';
            case 'b' -> '8';
            default -> value;
        };
    }

    private String extractAmount(String line) {
        // Santander format: amounts ending with '-' are debits (e.g. "7.863,55-")
        String trailingMinus = extractLastMatchGroup(TRAILING_MINUS_GROUPED_PATTERN, line, 1);
        if (StringUtils.hasText(trailingMinus)) {
            return "-" + trailingMinus;
        }
        trailingMinus = extractLastMatchGroup(TRAILING_MINUS_SIMPLE_PATTERN, line, 1);
        if (StringUtils.hasText(trailingMinus)) {
            return "-" + trailingMinus;
        }
        String groupedAmount = extractLastMatch(GROUPED_AMOUNT_PATTERN, line);
        return StringUtils.hasText(groupedAmount) ? groupedAmount : extractLastMatch(SIMPLE_AMOUNT_PATTERN, line);
    }

    private String extractLastMatchGroup(Pattern pattern, String line, int group) {
        Matcher matcher = pattern.matcher(line);
        String match = "";
        while (matcher.find()) {
            match = matcher.group(group).trim();
        }
        return match;
    }

    private String inferEntryType(String line, String amount) {
        String normalizedLine = normalizeForComparison(line);
        String signedType = inferEntryTypeFromAmount(amount, normalizedLine);
        if (StringUtils.hasText(signedType)) {
            return signedType;
        }

        if (containsAny(normalizedLine, CREDIT_KEYWORDS)) {
            return ENTRY_TYPE_CREDIT;
        }

        if (containsAny(normalizedLine, DEBIT_KEYWORDS)) {
            return ENTRY_TYPE_DEBIT;
        }

        return ENTRY_TYPE_DEBIT;
    }

    private boolean hasDate(String line) {
        return DATE_PATTERN.matcher(line).find();
    }

    private boolean hasAmount(String line) {
        return GROUPED_AMOUNT_PATTERN.matcher(line).find() || SIMPLE_AMOUNT_PATTERN.matcher(line).find();
    }

    private boolean looksLikeReference(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }
        String cleanedToken = token.trim();
        if (cleanedToken.matches("^\\d{1,2}/\\d{1,2}(?:/\\d{2,4})?.*")) {
            return false;
        }
        return REFERENCE_PATTERN.matcher(cleanedToken).matches() && cleanedToken.matches(".*\\d.*");
    }

    private boolean isNoise(String line) {
        String normalizedLine = line.toLowerCase(Locale.ROOT);
        return normalizedLine.matches("^pagina\\s+\\d+.*")
                || normalizedLine.matches("^page\\s+\\d+.*")
                || normalizedLine.matches("^[\\d\\s./-]+$")
                || normalizedLine.length() <= 3;
    }

    private String removeLastOccurrence(String input, String token) {
        int index = input.lastIndexOf(token);
        if (index < 0) {
            return input;
        }
        return (input.substring(0, index) + " " + input.substring(index + token.length())).trim();
    }

    private String extractLastMatch(Pattern pattern, String line) {
        Matcher matcher = pattern.matcher(line);
        String match = "";
        while (matcher.find()) {
            match = matcher.group().trim();
        }
        return match;
    }

    private boolean hasMeaningfulContent(ExtractedRow row) {
        return StringUtils.hasText(row.date())
                || StringUtils.hasText(row.complement())
                || StringUtils.hasText(row.credit())
                || StringUtils.hasText(row.debit());
    }

    private List<ExtractedRow> deduplicateRows(List<ExtractedRow> rows) {
        List<ExtractedRow> deduplicatedRows = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (ExtractedRow row : rows) {
            String key = (row.date() + "|" + row.complement() + "|" + row.historyCode() + "|" + row.credit() + "|"
                    + row.debit()).toLowerCase(Locale.ROOT);
            if (seen.add(key)) {
                deduplicatedRows.add(row);
            }
        }
        return deduplicatedRows;
    }

    private ExtractedRow buildRow(String docNumber, String entryType, String description, String amount, String date,
            String balance) {
        String normalizedDesc = normalizeText(description);
        String normalizedAmount = normalizeText(amount);
        String normalizedType = normalizeEntryType(entryType, normalizedDesc, normalizedAmount);
        boolean isCredit = ENTRY_TYPE_CREDIT.equals(normalizedType);
        String cleanAmount = normalizedAmount.replace("-", "").trim();
        return new ExtractedRow(
                normalizeText(date),
                normalizedAmount,
            isCredit ? "" : cleanAmount,
            isCredit ? cleanAmount : "",
                normalizeText(docNumber),
                normalizedDesc);
    }

    private String normalizeEntryType(String entryType, String context, String amount) {
        String normalizedType = normalizeForComparison(entryType);
        if (ENTRY_TYPE_CREDIT_NORMALIZED.equals(normalizedType)) {
            return ENTRY_TYPE_CREDIT;
        }
        if (ENTRY_TYPE_DEBIT_NORMALIZED.equals(normalizedType)) {
            return ENTRY_TYPE_DEBIT;
        }
        if (normalizedType.isBlank()) {
            return inferEntryType(context, amount);
        }
        return inferEntryType(context + " " + entryType, amount);
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    private String normalizeForComparison(String value) {
        String normalized = normalizeText(value).toLowerCase(Locale.ROOT);
        return Normalizer.normalize(normalized, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
    }

    private boolean containsAny(String value, String... terms) {
        for (String term : terms) {
            if (value.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private BigDecimal parseAmount(String amount) {
        if (!StringUtils.hasText(amount)) {
            return null;
        }

        String normalizedAmount = amount.replaceAll("[^\\d,.-]", "");
        if (normalizedAmount.isBlank()) {
            return null;
        }

        int lastComma = normalizedAmount.lastIndexOf(',');
        int lastDot = normalizedAmount.lastIndexOf('.');
        if (lastComma > lastDot) {
            normalizedAmount = normalizedAmount.replace(".", "").replace(',', '.');
        } else if (lastDot > lastComma && normalizedAmount.chars().filter(ch -> ch == '.').count() > 1) {
            normalizedAmount = normalizedAmount.replace(".", "");
        } else {
            normalizedAmount = normalizedAmount.replace(",", "");
        }

        try {
            return new BigDecimal(normalizedAmount);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String inferEntryTypeFromAmount(String amount, String normalizedLine) {
        if (!StringUtils.hasText(amount)) {
            return "";
        }

        BigDecimal numericAmount = parseAmount(amount);
        if (numericAmount == null) {
            return "";
        }

        int comparison = numericAmount.signum();
        if (comparison < 0) {
            return ENTRY_TYPE_DEBIT;
        }
        if (comparison > 0 && containsAny(normalizedLine, CREDIT_KEYWORDS)) {
            return ENTRY_TYPE_CREDIT;
        }
        return "";
    }

    private record LineMergeResult(String line, int nextIndex) {
    }
}
