package com.walyson.pdfexcelai.model;

import java.util.List;

public record ExtractionResult(
        String fileName,
        int pageCount,
        int totalRows,
        List<ExtractedRow> rows,
        boolean aiUsed,
        String extractionMode,
        String previewText
) {
}