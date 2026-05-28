package com.walyson.pdfexcelai.service;

import com.walyson.pdfexcelai.config.OcrProperties;
import com.walyson.pdfexcelai.model.BankProfile;
import com.walyson.pdfexcelai.model.OcrMode;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.assertj.core.api.Assertions.assertThat;

class OcrServiceTests {

    private OcrProperties disabled() {
        return new OcrProperties(false, "tesseract", "auto", "por", 6, 30, 0, 10_000_000L,
                false, 1.5, 180, 2, false, 5.0, 0.0, 0.0, 0.0);
    }

    private OcrProperties enabled(String command) {
        return new OcrProperties(true, command, "auto", "por", 6, 30, 3, 10_000_000L,
                false, 1.5, 180, 2, false, 5.0, 0.0, 0.0, 0.0);
    }

    private OcrProperties enabledWithPreprocess(String command) {
        return new OcrProperties(true, command, "auto", "por", 6, 30, 3, 10_000_000L,
                true, 1.5, 180, 2, true, 5.0, 0.1, 0.1, 0.05);
    }

    private BufferedImage createTestImage(int w, int h) {
        return new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
    }

    @Test
    void isEnabled_returnsFalse_whenDisabled() {
        OcrService service = new OcrService(disabled());
        assertThat(service.isEnabled()).isFalse();
    }

    @Test
    void isEnabled_returnsFalse_whenCommandBlank() {
        OcrProperties props = new OcrProperties(true, "", "auto", "por", 6, 30, 3, 10_000_000L,
                false, 1.5, 180, 2, false, 5.0, 0.0, 0.0, 0.0);
        OcrService service = new OcrService(props);
        assertThat(service.isEnabled()).isFalse();
    }

    @Test
    void isEnabled_returnsTrue_whenEnabledWithCommand() {
        OcrService service = new OcrService(enabled("tesseract"));
        assertThat(service.isEnabled()).isTrue();
    }

    @Test
    void isAvailable_returnsFalse_whenDisabled() {
        OcrService service = new OcrService(disabled());
        assertThat(service.isAvailable()).isFalse();
    }

    @Test
    void isAvailable_returnsFalse_whenCommandNotFound() {
        OcrService service = new OcrService(enabled("/nonexistent-command-xyz-abc"));
        assertThat(service.isAvailable()).isFalse();
    }

    @Test
    void isAvailable_caches_result() {
        OcrService service = new OcrService(enabled("/nonexistent-command-xyz-abc"));
        boolean first = service.isAvailable();
        boolean second = service.isAvailable();
        assertThat(first).isEqualTo(second);
    }

    @Test
    void commandName_returnsConfiguredCommand() {
        OcrService service = new OcrService(enabled("tesseract"));
        assertThat(service.commandName()).isEqualTo("tesseract");
    }

    @Test
    void commandName_returnsFallback_whenBlank() {
        OcrProperties props = new OcrProperties(false, "", "auto", "por", 6, 30, 3, 10_000_000L,
                false, 1.5, 180, 2, false, 5.0, 0.0, 0.0, 0.0);
        OcrService service = new OcrService(props);
        assertThat(service.commandName()).isEqualTo("tesseract");
    }

    @Test
    void maxPages_returnsMaxInt_whenZeroOrNegative() {
        OcrProperties props = new OcrProperties(true, "tesseract", "auto", "por", 6, 30, 0, 10_000_000L,
                false, 1.5, 180, 2, false, 5.0, 0.0, 0.0, 0.0);
        OcrService service = new OcrService(props);
        assertThat(service.maxPages()).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void maxPages_returnsConfigured_whenPositive() {
        OcrService service = new OcrService(enabled("tesseract"));
        assertThat(service.maxPages()).isEqualTo(3);
    }

    @Test
    void configuredMode_returnsAutoForAutoString() {
        OcrService service = new OcrService(enabled("tesseract"));
        assertThat(service.configuredMode()).isEqualTo(OcrMode.AUTO);
    }

    @Test
    void configuredMode_returnsFirstForFirstString() {
        OcrProperties props = new OcrProperties(true, "tesseract", "FIRST", "por", 6, 30, 3, 10_000_000L,
                false, 1.5, 180, 2, false, 5.0, 0.0, 0.0, 0.0);
        OcrService service = new OcrService(props);
        assertThat(service.configuredMode()).isEqualTo(OcrMode.FIRST);
    }

    @Test
    void configuredMode_returnsOnlyForOnlyString() {
        OcrProperties props = new OcrProperties(true, "tesseract", "ONLY", "por", 6, 30, 3, 10_000_000L,
                false, 1.5, 180, 2, false, 5.0, 0.0, 0.0, 0.0);
        OcrService service = new OcrService(props);
        assertThat(service.configuredMode()).isEqualTo(OcrMode.ONLY);
    }

    @Test
    void extractText_returnsEmpty_whenDisabled() {
        OcrService service = new OcrService(disabled());
        String result = service.extractText(createTestImage(100, 100));
        assertThat(result).isEmpty();
    }

    @Test
    void extractText_returnsEmpty_whenImageIsNull() {
        OcrService service = new OcrService(enabled("tesseract"));
        String result = service.extractText(null);
        assertThat(result).isEmpty();
    }

    @Test
    void extractText_withBankProfile_returnsEmpty_whenDisabled() {
        OcrService service = new OcrService(disabled());
        String result = service.extractText(createTestImage(100, 100), BankProfile.BANCO_DO_BRASIL);
        assertThat(result).isEmpty();
    }

    @Test
    void extractText_withBankProfile_returnsEmpty_whenImageIsNull() {
        OcrService service = new OcrService(enabled("tesseract"));
        String result = service.extractText(null, BankProfile.SANTANDER);
        assertThat(result).isEmpty();
    }

    @Test
    void extractText_commandNotFound_returnsEmpty() {
        OcrService service = new OcrService(enabled("/nonexistent-xyz"));
        // isAvailable() returns false since command not found, so extractText returns ""
        String result = service.extractText(createTestImage(100, 100));
        assertThat(result).isEmpty();
    }

    @Test
    void extractText_unknownProfile_commandNotFound_returnsEmpty() {
        OcrService service = new OcrService(enabled("/nonexistent-xyz"));
        String result = service.extractText(createTestImage(100, 100), BankProfile.UNKNOWN);
        assertThat(result).isEmpty();
    }

    @Test
    void extractText_santanderProfile_commandNotFound_returnsEmpty() {
        OcrService service = new OcrService(enabled("/nonexistent-xyz"));
        String result = service.extractText(createTestImage(200, 400), BankProfile.SANTANDER);
        assertThat(result).isEmpty();
    }

    @Test
    void extractText_bbProfile_commandNotFound_returnsEmpty() {
        OcrService service = new OcrService(enabled("/nonexistent-xyz"));
        String result = service.extractText(createTestImage(200, 400), BankProfile.BANCO_DO_BRASIL);
        assertThat(result).isEmpty();
    }

    @Test
    void configuredMode_returnsAuto_forUnknownString() {
        OcrProperties props = new OcrProperties(true, "tesseract", "UNKNOWN_MODE", "por", 6, 30, 3, 10_000_000L,
                false, 1.5, 180, 2, false, 5.0, 0.0, 0.0, 0.0);
        OcrService service = new OcrService(props);
        assertThat(service.configuredMode()).isEqualTo(OcrMode.AUTO);
    }

    @Test
    void configuredMode_returnsAuto_forBlankString() {
        OcrProperties props = new OcrProperties(true, "tesseract", "", "por", 6, 30, 3, 10_000_000L,
                false, 1.5, 180, 2, false, 5.0, 0.0, 0.0, 0.0);
        OcrService service = new OcrService(props);
        assertThat(service.configuredMode()).isEqualTo(OcrMode.AUTO);
    }

    @Test
    void configuredMode_returnsAuto_forNullMode() {
        OcrProperties props = new OcrProperties(true, "tesseract", null, "por", 6, 30, 3, 10_000_000L,
                false, 1.5, 180, 2, false, 5.0, 0.0, 0.0, 0.0);
        OcrService service = new OcrService(props);
        assertThat(service.configuredMode()).isEqualTo(OcrMode.AUTO);
    }

    @Test
    void maxPages_returnsNegative_treatedAsUnlimited() {
        OcrProperties props = new OcrProperties(true, "tesseract", "auto", "por", 6, 30, -5, 10_000_000L,
                false, 1.5, 180, 2, false, 5.0, 0.0, 0.0, 0.0);
        OcrService service = new OcrService(props);
        assertThat(service.maxPages()).isEqualTo(Integer.MAX_VALUE);
    }
}
