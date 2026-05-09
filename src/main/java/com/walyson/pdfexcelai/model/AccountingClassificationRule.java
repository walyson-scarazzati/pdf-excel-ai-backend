package com.walyson.pdfexcelai.model;

public record AccountingClassificationRule(
        String name,
        String keywords,
        String direction,
        String debitAccountCode,
        String creditAccountCode,
        String historyCode,
        int priority
) {
}
