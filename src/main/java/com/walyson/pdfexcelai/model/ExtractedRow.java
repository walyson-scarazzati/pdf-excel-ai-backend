package com.walyson.pdfexcelai.model;

public record ExtractedRow(
        String reference,
        String description,
        String amount,
        String date,
        String notes
) {
}