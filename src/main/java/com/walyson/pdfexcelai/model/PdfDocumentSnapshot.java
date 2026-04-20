package com.walyson.pdfexcelai.model;

import java.util.List;

public record PdfDocumentSnapshot(
        String rawText,
        String previewText,
        List<String> pageImages,
        int pageCount,
        boolean textAvailable
) {
}