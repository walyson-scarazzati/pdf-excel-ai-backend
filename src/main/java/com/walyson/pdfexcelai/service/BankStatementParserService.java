package com.walyson.pdfexcelai.service;

import com.walyson.pdfexcelai.model.ExtractedRow;
import com.walyson.pdfexcelai.model.BankProfile;
import com.walyson.pdfexcelai.model.PdfDocumentSnapshot;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.Normalizer;

@Service
public class BankStatementParserService {

    // Patterns para extrair linhas do extrato
    // Formato: DD/MM/YYYY;R$ VALUE;DEBIT;CREDIT;CODE;COMPLEMENT
    private static final Pattern CSV_LINE_PATTERN = Pattern.compile(
            "^(\\d{2}/\\d{2}/\\d{4})\\s*;\\s*([^;]*)\\s*;\\s*([^;]*)\\s*;\\s*([^;]*)\\s*;\\s*([^;]*)\\s*;\\s*(.*)$"
    );

    // Pattern para extrair dados de linha do PDF (formato do Banco do Brasil)
    // Exemplo: 01/09/2025    0000    14134    612 Recebimento Fornecedor    150.003    1.200,00 C
    private static final Pattern PDF_LINE_PATTERN = Pattern.compile(
            "(\\d{2}/\\d{2}/\\d{4})\\s+.*?([\\d.,]+)\\s*([CD]?)\\s*$"
    );
    private static final Pattern DATE_AT_START_PATTERN = Pattern.compile("^(\\d{2}/\\d{2})(?:/(\\d{4}))?\\b");
    private static final Pattern MONEY_PATTERN = Pattern.compile("-?\\d{1,3}(?:\\.\\d{3})*,\\d{2}-?|-?\\d+,\\d{2}-?");
    private static final Pattern COLUMN_ROW_PATTERN = Pattern.compile("^COL\\|([^|]*)\\|([^|]*)\\|([^|]*)\\|([^|]*)\\|([^|]*)\\|([^|]*)$");
    private static final Pattern HISTORY_CODE_PATTERN = Pattern.compile("\\b\\d{3,5}\\b");
    private static final Pattern LONG_REFERENCE_PATTERN = Pattern.compile("\\b\\d{6,}\\b");
    private static final String[] CREDIT_KEYWORDS = {
            "credito", "credito em conta", "pix recebido", "recebido", "antecipacao getnet", "transferencia recebida"
    };
    private static final String[] DEBIT_KEYWORDS = {
            "debito", "pagamento", "boleto", "tarifa", "pix enviado", "saque", "aplicacao contamax", "prest. emprest"
    };

    public List<ExtractedRow> parse(PdfDocumentSnapshot snapshot) {
        String text = snapshot.normalizedText();
        if (!StringUtils.hasText(text)) {
            return List.of();
        }

        return switch (snapshot.bankProfile()) {
            case BANCO_DO_BRASIL -> parseBancoDoBrasil(text);
            case SANTANDER -> parseSantander(text);
            case UNKNOWN -> parseFromPdfText(text);
        };
    }

    public List<ExtractedRow> parseFromCsv(String csvContent) {
        List<ExtractedRow> rows = new ArrayList<>();
        if (!StringUtils.hasText(csvContent)) {
            return rows;
        }

        String[] lines = csvContent.split("\\r?\\n");
        boolean isFirstLine = true;

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }

            // Pular cabeçalho (primeira linha)
            if (isFirstLine) {
                isFirstLine = false;
                if (line.toUpperCase().contains("DATA") && line.toUpperCase().contains("VALOR")) {
                    continue;
                }
            }

            Matcher matcher = CSV_LINE_PATTERN.matcher(line);
            if (matcher.matches()) {
                String date = matcher.group(1).trim();
                String value = matcher.group(2).trim();
                String debit = matcher.group(3).trim();
                String credit = matcher.group(4).trim();
                String historyCode = matcher.group(5).trim();
                String complement = matcher.group(6).trim();

                rows.add(new ExtractedRow(date, value, debit, credit, historyCode, complement));
            }
        }

        return rows;
    }

    public List<ExtractedRow> parseFromPdfText(String pdfText) {
        List<ExtractedRow> rows = new ArrayList<>();
        if (!StringUtils.hasText(pdfText)) {
            return rows;
        }

        // Estratégia: procurar padrões de data seguidos de valores
        String[] lines = pdfText.split("\\r?\\n");
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                continue;
            }

            // Tentar extrair data no início da linha
            Pattern datePattern = Pattern.compile("^(\\d{2}/\\d{2}/\\d{4})");
            Matcher dateMatcher = datePattern.matcher(line);
            
            if (dateMatcher.find()) {
                String date = dateMatcher.group(1);
                
                // Extrair informações da linha
                String[] parts = line.split("\\s+");
                String value = "";
                String debit = "";
                String credit = "";
                String historyCode = "";
                StringBuilder complement = new StringBuilder();
                
                // Última parte geralmente contém valor e direção (C/D)
                for (int j = parts.length - 1; j >= 0; j--) {
                    String part = parts[j];
                    
                    // Verificar se é valor monetário
                    if (part.matches("[\\d.,]+")) {
                        value = part;
                        
                        // Verificar próxima parte para C ou D
                        if (j + 1 < parts.length) {
                            String direction = parts[j + 1];
                            if (direction.equals("C")) {
                                credit = value;
                            } else if (direction.equals("D")) {
                                debit = value;
                            }
                        }
                        break;
                    }
                }
                
                // Extrair códigos numéricos (lote, documento, histórico)
                List<String> codes = new ArrayList<>();
                for (String part : parts) {
                    if (part.matches("\\d{4,}")) {
                        codes.add(part);
                    } else if (!part.equals(date) && !part.matches("[\\d.,]+[CD]?")) {
                        if (!complement.isEmpty()) {
                            complement.append(" ");
                        }
                        complement.append(part);
                    }
                }
                
                if (codes.size() >= 2) {
                    historyCode = codes.get(1); // Normalmente o código do histórico
                }
                
                // Se encontramos pelo menos data e valor
                if (!value.isEmpty()) {
                    rows.add(new ExtractedRow(
                            date,
                            formatCurrency(value),
                            debit.isEmpty() ? "" : formatCurrency(debit),
                            credit.isEmpty() ? "" : formatCurrency(credit),
                            historyCode,
                            complement.toString().trim()
                    ));
                }
            }
        }

        return rows;
    }

    private List<ExtractedRow> parseBancoDoBrasil(String text) {
        List<ExtractedRow> columnRows = parseBancoDoBrasilColumns(text);
        if (!columnRows.isEmpty()) {
            return columnRows;
        }

        List<ExtractedRow> rows = new ArrayList<>();
        String statementMonthYear = inferStatementMonthYear(text);
        for (String rawLine : text.split("\\R")) {
            String line = normalizeBankLine(rawLine);
            if (!looksLikeBancoDoBrasilTransaction(line, statementMonthYear)) {
                continue;
            }

            String date = resolveDate(line, statementMonthYear);
            String amount = extractLastAmount(line);
            if (!StringUtils.hasText(date) || !StringUtils.hasText(amount)) {
                continue;
            }

            String historyCode = extractHistoryCode(line, date);
            String complement = cleanDescription(line, date, amount, historyCode);
            boolean credit = isCreditLine(line, amount);
            rows.add(createRow(date, amount, credit, historyCode, complement));
        }
        return deduplicate(rows);
    }

    private List<ExtractedRow> parseBancoDoBrasilColumns(String text) {
        List<ExtractedRow> rows = new ArrayList<>();
        String statementMonthYear = inferStatementMonthYear(text);
        for (String rawLine : text.split("\\R")) {
            Matcher matcher = COLUMN_ROW_PATTERN.matcher(rawLine.trim());
            if (!matcher.matches()) {
                continue;
            }

            String dateToken = matcher.group(1).trim();
            String branchOrLot = matcher.group(2).trim();
            String historyCode = matcher.group(3).trim();
            String description = matcher.group(4).trim();
            String amount = matcher.group(5).trim();
            String dc = matcher.group(6).trim();

            String date = resolveDate(normalizeBankColumnDate(dateToken), statementMonthYear);
            String normalizedAmount = normalizeColumnAmount(amount);
            if (!StringUtils.hasText(date) || !StringUtils.hasText(normalizedAmount) || !StringUtils.hasText(description)) {
                continue;
            }

            String complement = String.join(" ", List.of(branchOrLot, description)).trim().replaceAll("\\s{2,}", " ");
            boolean credit = dc.equalsIgnoreCase("C") || isCreditLine(description + " " + dc, normalizedAmount);
            rows.add(createRow(date, normalizedAmount, credit, normalizeHistoryCode(historyCode), complement));
        }
        return deduplicate(rows);
    }

    private List<ExtractedRow> parseSantander(String text) {
        List<ExtractedRow> rows = new ArrayList<>();
        String statementMonthYear = inferStatementMonthYear(text);
        for (String rawLine : text.split("\\R")) {
            String line = normalizeSantanderLine(rawLine);
            if (!looksLikeSantanderTransaction(line, statementMonthYear)) {
                continue;
            }

            String date = resolveDate(line, statementMonthYear);
            String amount = extractLastAmount(line);
            if (!StringUtils.hasText(date) || !StringUtils.hasText(amount)) {
                continue;
            }

            String historyCode = extractSantanderHistoryCode(line, date, amount);
            String complement = cleanDescription(line, date, amount, historyCode);
            boolean credit = isCreditLine(line, amount);
            rows.add(createRow(date, amount, credit, historyCode, complement));
        }
        return deduplicate(rows);
    }

    public String extractAccountInfo(String text) {
        // Extrair informações da conta
        Pattern accountPattern = Pattern.compile("Conta corrente\\s+(\\d+[-]?\\d*)\\s+([^\\n]+)");
        Matcher matcher = accountPattern.matcher(text);
        if (matcher.find()) {
            return "Conta: " + matcher.group(1) + " - " + matcher.group(2).trim();
        }
        
        // Padrão alternativo
        Pattern agencyPattern = Pattern.compile("Agência\\s+(\\d+[-]?[\\w]*)");
        Matcher agencyMatcher = agencyPattern.matcher(text);
        if (agencyMatcher.find()) {
            return "Agência: " + agencyMatcher.group(1);
        }
        
        return "";
    }

    public String extractPeriod(String text) {
        // Extrair período do extrato
        Pattern periodPattern = Pattern.compile("(?:Período do extrato|extrato)\\s+(\\d{2}/\\d{2}/\\d{4}).*?(\\d{2}/\\d{2}/\\d{4})");
        Matcher matcher = periodPattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1) + " a " + matcher.group(2);
        }
        
        // Tentar extrair apenas do cabeçalho
        Pattern headerPeriodPattern = Pattern.compile("(\\d{2})\\s*/\\s*(\\d{4})");
        Matcher headerMatcher = headerPeriodPattern.matcher(text);
        if (headerMatcher.find()) {
            return headerMatcher.group(1) + "/" + headerMatcher.group(2);
        }
        
        return "";
    }

    private String formatCurrency(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        // Garantir que o formato está correto (R$ X.XXX,XX)
        String cleaned = value.replaceAll("[^\\d,.]", "");
        if (cleaned.isEmpty()) {
            return "";
        }
        return "R$ " + cleaned;
    }

    private boolean looksLikeBancoDoBrasilTransaction(String line, String statementMonthYear) {
        return StringUtils.hasText(resolveDate(line, statementMonthYear))
                && StringUtils.hasText(extractLastAmount(line))
                && containsAny(normalize(line), "pix", "boleto", "transfer", "cobranca", "credito", "debito", "bb rende", "pagamento", "saque");
    }

    private boolean looksLikeSantanderTransaction(String line, String statementMonthYear) {
        return StringUtils.hasText(resolveDate(line, statementMonthYear))
                && StringUtils.hasText(extractLastAmount(line))
                && containsAny(normalize(line), "pix", "getnet", "contamax", "tarifa", "pagamento", "qr checkout", "emprest", "titularidade");
    }

    private String resolveDate(String line, String statementMonthYear) {
        Matcher matcher = DATE_AT_START_PATTERN.matcher(line);
        if (!matcher.find()) {
            return "";
        }

        String dayMonth = matcher.group(1);
        String year = matcher.group(2);
        if (StringUtils.hasText(year)) {
            return dayMonth + "/" + year;
        }
        if (!StringUtils.hasText(statementMonthYear)) {
            return dayMonth;
        }

        String inferredYear = statementMonthYear.contains("/")
                ? statementMonthYear.substring(statementMonthYear.indexOf('/') + 1)
                : statementMonthYear;
        return dayMonth + "/" + inferredYear;
    }

    private String inferStatementMonthYear(String text) {
        Matcher matcher = Pattern.compile("\\b(\\d{2})\\s*/\\s*(\\d{4})\\b").matcher(text);
        return matcher.find() ? matcher.group(1) + "/" + matcher.group(2) : "";
    }

    private String extractLastAmount(String line) {
        Matcher matcher = MONEY_PATTERN.matcher(line);
        String amount = "";
        while (matcher.find()) {
            amount = matcher.group();
        }
        return amount;
    }

    private String extractHistoryCode(String line, String date) {
        String withoutDate = line.replaceFirst("^" + Pattern.quote(date) + "\\s*", "");
        Matcher matcher = HISTORY_CODE_PATTERN.matcher(withoutDate);
        while (matcher.find()) {
            String candidate = matcher.group();
            if (!candidate.chars().allMatch(ch -> ch == '0')) {
                return candidate;
            }
        }
        return "";
    }

    private String extractSantanderHistoryCode(String line, String date, String amount) {
        String withoutDate = line.replaceFirst("^" + Pattern.quote(date) + "\\s*", "");
        String withoutAmount = withoutDate.replace(amount, " ");
        Matcher longRef = LONG_REFERENCE_PATTERN.matcher(withoutAmount);
        if (longRef.find()) {
            return longRef.group();
        }
        return "";
    }

    private String cleanDescription(String line, String date, String amount, String historyCode) {
        String cleaned = line.replaceFirst("^" + Pattern.quote(date) + "\\s*", "")
                .replace(amount, " ");
        if (StringUtils.hasText(historyCode)) {
            cleaned = cleaned.replaceFirst("\\b" + Pattern.quote(historyCode) + "\\b", " ");
        }
        cleaned = cleaned.replaceAll("\\s+[CD]$", " ")
                .replaceAll("\\s{2,}", " ")
                .replace('|', ' ')
                .trim();
        return cleaned;
    }

    private ExtractedRow createRow(String date, String amount, boolean credit, String historyCode, String complement) {
        String value = amount.replace("-", "").trim();
        return new ExtractedRow(
                date,
                value,
                credit ? "" : value,
                credit ? value : "",
                historyCode,
                complement
        );
    }

    private boolean isCreditLine(String line, String amount) {
        String normalized = normalize(line);
        if (amount.endsWith("-") || amount.startsWith("-")) {
            return false;
        }
        if (normalized.matches(".*\\b[c]\\s*$")) {
            return true;
        }
        if (containsAny(normalized, CREDIT_KEYWORDS)) {
            return true;
        }
        if (containsAny(normalized, DEBIT_KEYWORDS)) {
            return false;
        }
        return false;
    }

    private String normalizeBankLine(String line) {
        String normalized = line == null ? "" : line.trim();
        normalized = normalized.replaceAll("^[^0-9]*(\\d{2})[tiI|l](\\d{2})[tiI|l](\\d{4})", "$1/$2/$3");
        normalized = normalized.replaceAll("\\s{2,}", " ");
        return normalized;
    }

    private String normalizeBankColumnDate(String value) {
        String digits = value == null ? "" : value.replaceAll("[^0-9]", "");
        if (digits.length() >= 8) {
            return digits.substring(0, 2) + "/" + digits.substring(2, 4) + "/" + digits.substring(digits.length() - 4);
        }
        if (digits.length() >= 4) {
            return digits.substring(0, 2) + "/" + digits.substring(2, 4);
        }
        return value == null ? "" : value.trim();
    }

    private String normalizeColumnAmount(String value) {
        String normalized = value == null ? "" : value.replaceAll("[^0-9,.-]", "").trim();
        if (!normalized.matches(".*\\d.*")) {
            return "";
        }
        if (normalized.matches("\\d+,\\d{2}[CD]")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String normalizeHistoryCode(String value) {
        String normalized = value == null ? "" : value.replaceAll("[^0-9]", "");
        return normalized.length() >= 3 ? normalized : "";
    }

    private String normalizeSantanderLine(String line) {
        String normalized = line == null ? "" : line.trim();
        normalized = normalized.replaceAll("^[^0-9]*(\\d{2})/(\\d{2})\\s+", "$1/$2 ");
        normalized = normalized.replaceAll("\\s{2,}", " ");
        return normalized;
    }

    private List<ExtractedRow> deduplicate(List<ExtractedRow> rows) {
        List<ExtractedRow> unique = new ArrayList<>();
        List<String> seen = new ArrayList<>();
        for (ExtractedRow row : rows) {
            String key = row.date() + "|" + row.value() + "|" + row.historyCode() + "|" + row.complement();
            if (!seen.contains(key)) {
                seen.add(key);
                unique.add(row);
            }
        }
        return unique;
    }

    private boolean containsAny(String value, String... terms) {
        for (String term : terms) {
            if (value.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        String normalized = value == null ? "" : value.toLowerCase(Locale.ROOT);
        return Normalizer.normalize(normalized, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
    }
}
