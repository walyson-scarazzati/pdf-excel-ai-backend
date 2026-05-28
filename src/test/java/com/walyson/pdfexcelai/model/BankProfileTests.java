package com.walyson.pdfexcelai.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BankProfileTests {

    @Test
    void resolveOcrMode_returnsDefaultMode_whenConfiguredModeIsNull() {
        assertThat(BankProfile.BANCO_DO_BRASIL.resolveOcrMode(null))
                .isEqualTo(OcrMode.AUTO);
    }

    @Test
    void resolveOcrMode_returnsDefaultMode_whenConfiguredModeIsAuto() {
        assertThat(BankProfile.BANCO_DO_BRASIL.resolveOcrMode(OcrMode.AUTO))
                .isEqualTo(OcrMode.AUTO);
    }

    @Test
    void resolveOcrMode_returnsConfiguredMode_whenNotAuto() {
        assertThat(BankProfile.SANTANDER.resolveOcrMode(OcrMode.FIRST))
                .isEqualTo(OcrMode.FIRST);
    }

    @Test
    void resolveOcrMode_returnsSantanderDefault_whenAutoMode() {
        assertThat(BankProfile.SANTANDER.resolveOcrMode(OcrMode.AUTO))
                .isEqualTo(OcrMode.FIRST);
    }

    @Test
    void resolveOcrMode_returnsUnknownDefault_whenAutoMode() {
        assertThat(BankProfile.UNKNOWN.resolveOcrMode(OcrMode.AUTO))
                .isEqualTo(OcrMode.AUTO);
    }

    @Test
    void bankProfileEnum_hasExpectedValues() {
        assertThat(BankProfile.values())
                .containsExactlyInAnyOrder(BankProfile.UNKNOWN, BankProfile.BANCO_DO_BRASIL, BankProfile.SANTANDER);
    }

    @Test
    void bankProfileEnum_valueOf_works() {
        assertThat(BankProfile.valueOf("BANCO_DO_BRASIL"))
                .isEqualTo(BankProfile.BANCO_DO_BRASIL);
    }
}
