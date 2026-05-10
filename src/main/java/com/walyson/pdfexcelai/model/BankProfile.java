package com.walyson.pdfexcelai.model;

public enum BankProfile {
    UNKNOWN(OcrMode.AUTO),
    BANCO_DO_BRASIL(OcrMode.AUTO),
    SANTANDER(OcrMode.FIRST);

    private final OcrMode defaultOcrMode;

    BankProfile(OcrMode defaultOcrMode) {
        this.defaultOcrMode = defaultOcrMode;
    }

    public OcrMode resolveOcrMode(OcrMode configuredMode) {
        return configuredMode == null || configuredMode == OcrMode.AUTO ? defaultOcrMode : configuredMode;
    }
}
