package com.walyson.pdfexcelai.service;

import com.walyson.pdfexcelai.model.AccountingClassificationRule;
import com.walyson.pdfexcelai.model.ExtractedRow;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
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
        Set<String> knownCodes = repository.findKnownAccountCodes().stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toSet());
        List<ExtractedRow> classifiedRows = new ArrayList<>(rows.size());
        for (ExtractedRow row : rows) {
            classifiedRows.add(classify(row, rules, knownCodes));
        }
        return classifiedRows;
    }

    public ExtractedRow classify(ExtractedRow row) {
        Set<String> knownCodes = repository.findKnownAccountCodes().stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toSet());
        return classify(row, repository.findActiveRules(), knownCodes);
    }

    private ExtractedRow classify(ExtractedRow row, List<AccountingClassificationRule> rules, Set<String> knownCodes) {
        if (row == null) {
            return new ExtractedRow("", "", "", "", "", "");
        }
        if (alreadyHasAccountCodes(row)) {
            return normalizeValue(row, knownCodes);
        }

        String context = normalize(row.complement() + " " + row.historyCode());
        String direction = inferDirection(row, context);
        AccountingClassificationRule rule = findRule(rules, context, direction);
        String normalizedDebit = rule == null ? "" : normalizeAccountCode(rule.debitAccountCode(), knownCodes);
        String normalizedCredit = rule == null ? "" : normalizeAccountCode(rule.creditAccountCode(), knownCodes);

        return new ExtractedRow(
                clean(row.date()),
                normalizeCurrency(row.value()),
            normalizedDebit,
            normalizedCredit,
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

    private ExtractedRow normalizeValue(ExtractedRow row, Set<String> knownCodes) {
        return new ExtractedRow(
                clean(row.date()),
                normalizeCurrency(row.value()),
                normalizeAccountCode(row.debit(), knownCodes),
                normalizeAccountCode(row.credit(), knownCodes),
                clean(row.historyCode()),
                clean(row.complement()));
    }

    private String normalizeAccountCode(String code, Set<String> knownCodes) {
        String cleaned = clean(code).replaceAll("\\D", "");
        if (!StringUtils.hasText(cleaned)) {
            return "";
        }
        if (knownCodes.isEmpty()) {
            return cleaned;
        }
        if (knownCodes.contains(cleaned)) {
            return cleaned;
        }

        List<String> prefixedCandidates = knownCodes.stream()
                .filter(existing -> existing.startsWith(cleaned))
                .sorted((left, right) -> {
                    int byLength = Integer.compare(left.length(), right.length());
                    return byLength != 0 ? byLength : left.compareTo(right);
                })
                .toList();

        if (prefixedCandidates.size() == 1) {
            return prefixedCandidates.get(0);
        }

        List<String> suffixCandidates = knownCodes.stream()
            .filter(cleaned::startsWith)
                .sorted((left, right) -> Integer.compare(right.length(), left.length()))
                .toList();
        if (!suffixCandidates.isEmpty()) {
            return suffixCandidates.get(0);
        }

        return cleaned;
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
