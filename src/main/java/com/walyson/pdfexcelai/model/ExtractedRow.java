package com.walyson.pdfexcelai.model;

public record ExtractedRow(
        String date,
        String value,
        String debit,
        String credit,
        String historyCode,
        String complement
) {
}