package com.walyson.pdfexcelai.model;

import java.util.Locale;

public enum OcrMode {
    AUTO,
    FIRST,
    ONLY;

    public static OcrMode from(String value) {
        if (value == null || value.isBlank()) {
            return AUTO;
        }

        try {
            return OcrMode.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return AUTO;
        }
    }
}