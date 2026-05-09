package com.walyson.pdfexcelai.model;

import java.util.List;

public record ExtractionResult(
        String fileName,
        String accountInfo,
        String extractPeriod,
        int pageCount,
        int totalRows,
        List<ExtractedRow> rows,
        List<String> pageImages,
        boolean aiUsed,
        boolean ocrUsed,
        String extractionMode,
        String previewText
) {
}