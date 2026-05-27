package com.walyson.pdfexcelai.service;

import com.walyson.pdfexcelai.model.ExtractedRow;
import com.walyson.pdfexcelai.model.PdfDocumentSnapshot;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.Normalizer;

@Service
public class BankStatementParserService {

        // Patterns para extrair linhas do extrato
        // Formato: DD/MM/YYYY;R$ VALUE;DEBIT;CREDIT;CODE;COMPLEMENT

    private static final Pattern DATE_AT_START_PATTERN = Pattern.compile("^(\\d{2}/\\d{2})(?:/(\\d{4}))?\\b");
    private static final String MONEY_NUMBER_REGEX = "-?\\d{1,3}(?:\\.\\d{3})*,\\d{2}-?|-?\\d+,\\d{2}-?|-?\\d{1,3}(?:,\\d{3})*\\.\\d{2}-?|-?\\d+\\.\\d{2}-?";
    private static final Pattern MONEY_PATTERN = Pattern.compile(MONEY_NUMBER_REGEX);
    private static final Pattern MONEY_WITH_DIRECTION_PATTERN = Pattern.compile("(" + MONEY_NUMBER_REGEX + ")\\s*([CcDd])\\s*[-_/]?");
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

            String[] columns = line.split(";", 6);
            if (columns.length < 2) {
                continue;
            }

            String date = sanitizeDate(column(columns, 0));
            if (!date.matches("\\d{2}/\\d{2}/\\d{4}")) {
                continue;
            }

            String value = sanitizeCurrency(column(columns, 1));
            String debit = sanitizeAccountCode(column(columns, 2));
            String credit = sanitizeAccountCode(column(columns, 3));
            String historyCode = sanitizeHistoryCode(column(columns, 4));
            String complement = clean(column(columns, 5));

            rows.add(new ExtractedRow(date, value, debit, credit, historyCode, complement));
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
        String statementMonthYear = inferStatementMonthYear(text);
        List<ExtractedRow> rows = new ArrayList<>();
        for (String line : buildBancoDoBrasilBlocks(text)) {
            if (!looksLikeBancoDoBrasilTransaction(line, statementMonthYear)) {
                continue;
            }

            String date = resolveDate(line, statementMonthYear);
            String amount = normalizeAmount(extractLastAmount(line));
            if (!StringUtils.hasText(date) || !StringUtils.hasText(amount)) {
                continue;
            }

            String historyCode = extractHistoryCode(line, date);
            String complement = cleanDescription(line, date, amount, historyCode);
            boolean credit = isCreditLine(line, amount);
            rows.add(createRow(date, amount, credit, historyCode, complement));
        }

        List<ExtractedRow> blockRows = deduplicate(rows);
        return pickMostReliableRows(columnRows, blockRows);
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
            String normalizedDirection = normalizeDirection(dc);
            if (!StringUtils.hasText(date)
                    || !StringUtils.hasText(normalizedAmount)
                    || !StringUtils.hasText(description)
                    || !StringUtils.hasText(normalizedDirection)) {
                continue;
            }

            String complement = String.join(" ", List.of(branchOrLot, description)).trim().replaceAll("\\s{2,}", " ");
            boolean credit = normalizedDirection.equals("C");
            rows.add(createRow(date, normalizedAmount, credit, normalizeHistoryCode(historyCode), complement));
        }
        return deduplicate(rows);
    }

    private String normalizeDirection(String value) {
        String normalized = clean(value).toUpperCase(Locale.ROOT).replaceAll("[^CD]", "");
        if (normalized.contains("C")) {
            return "C";
        }
        if (normalized.contains("D")) {
            return "D";
        }
        return "";
    }

    private List<ExtractedRow> pickMostReliableRows(List<ExtractedRow> columnRows, List<ExtractedRow> blockRows) {
        if (columnRows.isEmpty()) {
            return blockRows;
        }
        if (blockRows.isEmpty()) {
            return columnRows;
        }

        int columnScore = scoreRows(columnRows);
        int blockScore = scoreRows(blockRows);
        return columnScore > blockScore ? columnRows : blockRows;
    }

    private int scoreRows(List<ExtractedRow> rows) {
        int score = 0;
        for (ExtractedRow row : rows) {
            score += scoreRow(row);
        }
        return score;
    }

    private int scoreRow(ExtractedRow row) {
        int score = 0;

        if (row.date().matches("\\d{2}/\\d{2}/\\d{4}")) {
            score += 3;
        }
        if (row.value().matches("\\d{1,3}(?:\\.\\d{3})*,\\d{2}|\\d+,\\d{2}")) {
            score += 3;
        }

        boolean hasDebit = StringUtils.hasText(row.debit());
        boolean hasCredit = StringUtils.hasText(row.credit());
        if (hasDebit ^ hasCredit) {
            score += 2;
        }

        if (StringUtils.hasText(row.historyCode())) {
            score += 1;
        }

        if (StringUtils.hasText(row.complement())) {
            String normalized = normalize(row.complement());
            if (normalized.matches(".*[a-z].*")) {
                score += 2;
            }
            if (normalized.length() > 12) {
                int digits = row.complement().replaceAll("[^0-9]", "").length();
                if ((double) digits / normalized.length() > 0.45d) {
                    score -= 2;
                }
            }
            if (containsAny(normalized, "saldo anterior", "saldo final", "saldo ", "limite especial")) {
                score -= 2;
            }
        }

        return score;
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

    private String sanitizeDate(String value) {
        String trimmed = clean(value);
        if (trimmed.matches("\\d{2}/\\d{2}/\\d{2}")) {
            return trimmed.substring(0, 6) + "20" + trimmed.substring(6);
        }
        return trimmed;
    }

    private String sanitizeCurrency(String value) {
        String normalized = normalizeAmount(clean(value));
        if (!StringUtils.hasText(normalized)) {
            return "";
        }
        if (normalized.matches("\\d+,\\d{2}")) {
            return "R$ " + normalized;
        }
        return normalized;
    }

    private String sanitizeAccountCode(String value) {
        String digits = clean(value).replaceAll("[^0-9]", "");
        if (!StringUtils.hasText(digits)) {
            return "";
        }
        return digits;
    }

    private String sanitizeHistoryCode(String value) {
        String digits = clean(value).replaceAll("[^0-9]", "");
        return StringUtils.hasText(digits) ? digits : "";
    }

    private String column(String[] columns, int index) {
        if (index >= columns.length) {
            return "";
        }
        return Objects.requireNonNullElse(columns[index], "");
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean looksLikeBancoDoBrasilTransaction(String line, String statementMonthYear) {
        return StringUtils.hasText(resolveDate(line, statementMonthYear))
                && StringUtils.hasText(extractLastAmount(line))
                && !containsAny(normalize(line), "saldo anterior", "saldo ", "limite especial", "taxa limite", "custo efetivo", "valor total")
                && (StringUtils.hasText(extractHistoryCode(line, resolveDate(line, statementMonthYear)))
                || containsAny(normalize(line), "pix", "boleto", "transfer", "cobranca", "credito", "debito", "bb rende", "rende facil", "pagamento", "saque", "tarifa", "impostos"));
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
        Matcher directedMatcher = MONEY_WITH_DIRECTION_PATTERN.matcher(line);
        String directedAmount = "";
        while (directedMatcher.find()) {
            String candidate = directedMatcher.group(1);
            if (isLikelyMoney(candidate)) {
                directedAmount = candidate;
            }
        }
        if (StringUtils.hasText(directedAmount)) {
            return directedAmount;
        }

        Matcher matcher = MONEY_PATTERN.matcher(line);
        String amount = "";
        while (matcher.find()) {
            String candidate = matcher.group();
            if (isLikelyMoney(candidate)) {
                amount = candidate;
            }
        }
        return amount;
    }

    private boolean isLikelyMoney(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        String cleaned = value.replace("-", "").trim();
        if (cleaned.equals("0,00") || cleaned.equals("0.00")) {
            return false;
        }
        return cleaned.matches("\\d{1,3}(?:[.,]\\d{3})*[,.]\\d{2}|\\d+[,.]\\d{2}");
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
        String rawAmount = amount.replace(',', '.');
        if (!rawAmount.equals(amount)) {
            cleaned = cleaned.replace(rawAmount, " ");
        }
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
        String direction = extractDirectionForAmount(line, amount);
        if (direction.equalsIgnoreCase("C")) {
            return true;
        }
        if (direction.equalsIgnoreCase("D")) {
            return false;
        }

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

    private String extractDirectionForAmount(String line, String amount) {
        if (!StringUtils.hasText(amount)) {
            return "";
        }
        Matcher matcher = MONEY_WITH_DIRECTION_PATTERN.matcher(line);
        String normalizedAmount = normalizeAmount(amount);
        String direction = "";
        while (matcher.find()) {
            if (normalizeAmount(matcher.group(1)).equals(normalizedAmount)) {
                direction = matcher.group(2);
            }
        }
        return direction;
    }

    private String normalizeBankLine(String line) {
        String normalized = line == null ? "" : line.trim();
        normalized = normalizeBancoDoBrasilDateTokens(normalized);
        normalized = normalized.replaceAll("(?<!\\d)([0-3]\\d)\\s*[tTiI|lLjJ,/.-]\\s*([01]\\d)\\s*[tTiI|lLnNjJ,/.-]\\s*(20\\d{2})(?!\\d)", "$1/$2/$3");
        normalized = normalized.replaceAll("(?<!\\d)([0-3]\\d)([01]\\d)(20\\d{2})(?!\\d)", "$1/$2/$3");
        normalized = normalized.replaceAll("^[^0-9]*(\\d{2})[tTiI|lLjJ/](\\d{2})[tTiI|lLnNjJ/](\\d{4})", "$1/$2/$3");
        normalized = normalized.replaceAll("^[^0-9]*(\\d{2})[tTiI|lLjJ/](\\d{2})\\b", "$1/$2");
        normalized = normalized.replaceAll("\\s{2,}", " ");
        return normalized;
    }

    private String normalizeBancoDoBrasilDateTokens(String line) {
        Matcher matcher = Pattern.compile("\\S*(?:202\\d|20[oO0]\\d)\\S*").matcher(line);
        StringBuilder builder = new StringBuilder();
        while (matcher.find()) {
            String normalizedDate = normalizeBancoDoBrasilDateToken(matcher.group());
            matcher.appendReplacement(builder, StringUtils.hasText(normalizedDate)
                    ? Matcher.quoteReplacement(normalizedDate)
                    : Matcher.quoteReplacement(matcher.group()));
        }
        matcher.appendTail(builder);
        return builder.toString();
    }

    private String normalizeBancoDoBrasilDateToken(String token) {
        String canonical = token
                .replace('O', '0')
                .replace('o', '0')
                .replace('@', '0')
                .replace('Z', '2')
                .replace('z', '2');
        String digits = canonical.replaceAll("[^0-9]", "");
        int yearIndex = digits.indexOf("202");
        if (yearIndex < 3) {
            return "";
        }

        String year = digits.substring(yearIndex, Math.min(yearIndex + 4, digits.length()));
        if (year.length() != 4) {
            return "";
        }

        String prefix = digits.substring(0, yearIndex);
        if (prefix.length() < 3) {
            return "";
        }

        String day = prefix.substring(0, Math.min(2, prefix.length()));
        String month = inferMonthDigits(prefix);
        if (!isValidDayMonth(day, month)) {
            return "";
        }
        return day + "/" + month + "/" + year;
    }

    private String inferMonthDigits(String datePrefix) {
        List<String> candidates = new ArrayList<>();
        if (datePrefix.length() >= 4) {
            candidates.add(datePrefix.substring(datePrefix.length() - 2));
        }
        if (datePrefix.length() >= 5) {
            candidates.add(datePrefix.substring(datePrefix.length() - 3, datePrefix.length() - 1));
        }
        if (datePrefix.length() >= 4) {
            candidates.add(datePrefix.substring(2, 4));
        }
        if (datePrefix.length() == 3) {
            candidates.add("0" + datePrefix.substring(2));
        }

        for (String candidate : candidates) {
            int month = parsePositiveInt(candidate);
            if (month >= 1 && month <= 12) {
                return String.format("%02d", month);
            }
        }
        return "";
    }

    private boolean isValidDayMonth(String dayValue, String monthValue) {
        int day = parsePositiveInt(dayValue);
        int month = parsePositiveInt(monthValue);
        return day >= 1 && day <= 31 && month >= 1 && month <= 12;
    }

    private int parsePositiveInt(String value) {
        if (!StringUtils.hasText(value) || !value.matches("\\d+")) {
            return -1;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private List<String> buildBancoDoBrasilBlocks(String text) {
        List<String> blocks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean currentStartedWithDate = false;

        for (String rawLine : text.split("\\R")) {
            String line = normalizeBankLine(rawLine);
            if (!StringUtils.hasText(line)) {
                continue;
            }

            for (String segment : splitBancoDoBrasilDateSegments(line)) {
                boolean startsWithDate = DATE_AT_START_PATTERN.matcher(segment).find();
                if (startsWithDate && currentStartedWithDate && current.length() > 0) {
                    blocks.add(current.toString());
                    current.setLength(0);
                }

                if (startsWithDate || currentStartedWithDate) {
                    if (current.length() > 0) {
                        current.append(' ');
                    }
                    current.append(segment);
                    currentStartedWithDate = true;
                }
            }
        }

        if (currentStartedWithDate && current.length() > 0) {
            blocks.add(current.toString());
        }

        return blocks;
    }

    private List<String> splitBancoDoBrasilDateSegments(String line) {
        List<String> segments = new ArrayList<>();
        Matcher matcher = Pattern.compile("\\b[0-3]\\d/[01]\\d(?:/20\\d{2})?\\b").matcher(line);
        int start = -1;
        while (matcher.find()) {
            if (start >= 0 && matcher.start() > start) {
                String segment = line.substring(start, matcher.start()).trim();
                if (StringUtils.hasText(segment)) {
                    segments.add(segment);
                }
            }
            start = matcher.start();
        }

        if (start >= 0) {
            String segment = line.substring(start).trim();
            if (StringUtils.hasText(segment)) {
                segments.add(segment);
            }
        } else {
            segments.add(line);
        }
        return segments;
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
        return normalizeAmount(normalized);
    }

    private String normalizeAmount(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = value.replaceAll("[^0-9,.-]", "").trim();
        if (!normalized.contains(",") && normalized.matches("-?\\d+(?:\\.\\d{3})*\\.\\d{2}-?|-?\\d+\\.\\d{2}-?")) {
            int lastDot = normalized.lastIndexOf('.');
            normalized = normalized.substring(0, lastDot).replace(".", "") + "," + normalized.substring(lastDot + 1);
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
