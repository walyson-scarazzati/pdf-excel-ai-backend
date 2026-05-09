package com.walyson.pdfexcelai.service;

import com.walyson.pdfexcelai.model.AccountingClassificationRule;
import com.walyson.pdfexcelai.model.ExtractedRow;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AccountingClassificationService {

    private static final String DIRECTION_ANY = "ANY";
    private static final String DIRECTION_CREDIT = "CREDIT";
    private static final String DIRECTION_DEBIT = "DEBIT";

    private final AccountingClassificationRepository repository;

    public AccountingClassificationService(AccountingClassificationRepository repository) {
        this.repository = repository;
    }

    public List<ExtractedRow> classify(List<ExtractedRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }

        List<AccountingClassificationRule> rules = repository.findActiveRules();
        List<ExtractedRow> classifiedRows = new ArrayList<>(rows.size());
        for (ExtractedRow row : rows) {
            classifiedRows.add(classify(row, rules));
        }
        return classifiedRows;
    }

    public ExtractedRow classify(ExtractedRow row) {
        return classify(row, repository.findActiveRules());
    }

    private ExtractedRow classify(ExtractedRow row, List<AccountingClassificationRule> rules) {
        if (row == null) {
            return new ExtractedRow("", "", "", "", "", "");
        }
        if (alreadyHasAccountCodes(row)) {
            return normalizeValue(row);
        }

        String context = normalize(row.complement() + " " + row.historyCode());
        String direction = inferDirection(row, context);
        AccountingClassificationRule rule = findRule(rules, context, direction);

        return new ExtractedRow(
                clean(row.date()),
                normalizeCurrency(row.value()),
                rule == null ? "" : rule.debitAccountCode(),
                rule == null ? "" : rule.creditAccountCode(),
                rule == null ? clean(row.historyCode()) : rule.historyCode(),
                clean(row.complement()));
    }

    private AccountingClassificationRule findRule(
            List<AccountingClassificationRule> rules,
            String context,
            String direction
    ) {
        for (AccountingClassificationRule rule : rules) {
            if (directionMatches(rule.direction(), direction) && keywordsMatch(rule.keywords(), context)) {
                return rule;
            }
        }
        return null;
    }

    private boolean directionMatches(String ruleDirection, String direction) {
        String normalizedRuleDirection = clean(ruleDirection).toUpperCase(Locale.ROOT);
        return DIRECTION_ANY.equals(normalizedRuleDirection) || normalizedRuleDirection.equals(direction);
    }

    private boolean keywordsMatch(String keywords, String context) {
        List<String> values = splitKeywords(keywords);
        if (values.isEmpty()) {
            return true;
        }
        for (String keyword : values) {
            if (context.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private List<String> splitKeywords(String keywords) {
        if (!StringUtils.hasText(keywords)) {
            return List.of();
        }
        return Arrays.stream(keywords.split(","))
                .map(this::normalize)
                .filter(StringUtils::hasText)
                .toList();
    }

    private boolean alreadyHasAccountCodes(ExtractedRow row) {
        return looksLikeAccountCode(row.debit())
                && looksLikeAccountCode(row.credit())
                && !looksLikeMoney(row.debit())
                && !looksLikeMoney(row.credit());
    }

    private boolean looksLikeAccountCode(String value) {
        return StringUtils.hasText(value) && value.trim().matches("\\d{1,8}");
    }

    private boolean looksLikeMoney(String value) {
        return StringUtils.hasText(value) && value.matches(".*\\d+[,.]\\d{2}.*");
    }

    private ExtractedRow normalizeValue(ExtractedRow row) {
        return new ExtractedRow(
                clean(row.date()),
                normalizeCurrency(row.value()),
                clean(row.debit()),
                clean(row.credit()),
                clean(row.historyCode()),
                clean(row.complement()));
    }

    private String inferDirection(ExtractedRow row, String context) {
        if (StringUtils.hasText(row.credit()) && !StringUtils.hasText(row.debit())) {
            return DIRECTION_CREDIT;
        }
        if (StringUtils.hasText(row.debit()) && !StringUtils.hasText(row.credit())) {
            return DIRECTION_DEBIT;
        }
        if (StringUtils.hasText(row.value()) && row.value().contains("-")) {
            return DIRECTION_DEBIT;
        }
        if (containsAny(context, "recebido", "credito", "deposito", "entrada", "resgate")) {
            return DIRECTION_CREDIT;
        }
        return DIRECTION_DEBIT;
    }

    private String normalizeCurrency(String value) {
        String cleaned = clean(value);
        if (!StringUtils.hasText(cleaned)) {
            return "";
        }
        cleaned = cleaned.replace("-", "").trim();
        if (cleaned.startsWith("R$")) {
            return cleaned.replaceAll("\\s+", " ");
        }
        return "R$ " + cleaned;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
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
        String normalized = clean(value).toLowerCase(Locale.ROOT);
        return Normalizer.normalize(normalized, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
    }
}
