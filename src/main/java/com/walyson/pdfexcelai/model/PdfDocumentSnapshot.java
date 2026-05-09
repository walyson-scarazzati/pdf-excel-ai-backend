package com.walyson.pdfexcelai.model;

import java.util.List;

public record PdfDocumentSnapshot(
        String rawText,
        String normalizedText,
        String previewText,
        List<String> pageTexts,
        List<String> pageImages,
        int pageCount,
        boolean textAvailable,
        boolean ocrUsed,
        String sourceType,
        BankProfile bankProfile,
        OcrMode ocrMode
) {
}