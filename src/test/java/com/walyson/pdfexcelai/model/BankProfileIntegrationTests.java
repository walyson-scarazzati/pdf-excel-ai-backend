package com.walyson.pdfexcelai.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BankProfileIntegrationTests {

    @Test
    void bankProfile_resolveOcrMode_allBanks() {
        // Test for BANCO_DO_BRASIL
        assertThat(BankProfile.BANCO_DO_BRASIL.resolveOcrMode(null))
                .isEqualTo(OcrMode.AUTO);
        assertThat(BankProfile.BANCO_DO_BRASIL.resolveOcrMode(OcrMode.AUTO))
                .isEqualTo(OcrMode.AUTO);
        assertThat(BankProfile.BANCO_DO_BRASIL.resolveOcrMode(OcrMode.FIRST))
                .isEqualTo(OcrMode.FIRST);

        // Test for SANTANDER
        assertThat(BankProfile.SANTANDER.resolveOcrMode(null))
                .isEqualTo(OcrMode.FIRST);
        assertThat(BankProfile.SANTANDER.resolveOcrMode(OcrMode.AUTO))
                .isEqualTo(OcrMode.FIRST);
        assertThat(BankProfile.SANTANDER.resolveOcrMode(OcrMode.FIRST))
                .isEqualTo(OcrMode.FIRST);

        // Test for UNKNOWN
        assertThat(BankProfile.UNKNOWN.resolveOcrMode(null))
                .isEqualTo(OcrMode.AUTO);
        assertThat(BankProfile.UNKNOWN.resolveOcrMode(OcrMode.AUTO))
                .isEqualTo(OcrMode.AUTO);
        assertThat(BankProfile.UNKNOWN.resolveOcrMode(OcrMode.FIRST))
                .isEqualTo(OcrMode.FIRST);
    }

    @Test
    void bankProfile_defaultOcrModeVariations() {
        // BANCO_DO_BRASIL uses AUTO by default
        BankProfile bbProfile = BankProfile.BANCO_DO_BRASIL;
        assertThat(bbProfile.resolveOcrMode(OcrMode.AUTO)).isEqualTo(OcrMode.AUTO);

        // SANTANDER uses FIRST by default
        BankProfile santanderProfile = BankProfile.SANTANDER;
        assertThat(santanderProfile.resolveOcrMode(OcrMode.AUTO)).isEqualTo(OcrMode.FIRST);
    }

    @Test
    void bankProfile_configuredModeAlwaysWins() {
        // When a non-AUTO mode is configured, it should always be used
        for (BankProfile profile : BankProfile.values()) {
            OcrMode result = profile.resolveOcrMode(OcrMode.FIRST);
            assertThat(result).isEqualTo(OcrMode.FIRST);
        }
    }

    @Test
    void bankProfile_enumConsistency() {
        BankProfile[] profiles = BankProfile.values();
        assertThat(profiles).hasSize(3);

        assertThat(profiles).contains(
                BankProfile.UNKNOWN,
                BankProfile.BANCO_DO_BRASIL,
                BankProfile.SANTANDER
        );
    }
}
