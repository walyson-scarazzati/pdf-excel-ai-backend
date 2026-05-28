package com.walyson.pdfexcelai.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OcrModeTests {

    @Test
    void from_null_returnsAuto() {
        assertThat(OcrMode.from(null)).isEqualTo(OcrMode.AUTO);
    }

    @Test
    void from_blank_returnsAuto() {
        assertThat(OcrMode.from("   ")).isEqualTo(OcrMode.AUTO);
    }

    @Test
    void from_emptyString_returnsAuto() {
        assertThat(OcrMode.from("")).isEqualTo(OcrMode.AUTO);
    }

    @Test
    void from_auto_returnsAuto() {
        assertThat(OcrMode.from("AUTO")).isEqualTo(OcrMode.AUTO);
        assertThat(OcrMode.from("auto")).isEqualTo(OcrMode.AUTO);
    }

    @Test
    void from_first_returnsFirst() {
        assertThat(OcrMode.from("FIRST")).isEqualTo(OcrMode.FIRST);
        assertThat(OcrMode.from("first")).isEqualTo(OcrMode.FIRST);
    }

    @Test
    void from_only_returnsOnly() {
        assertThat(OcrMode.from("ONLY")).isEqualTo(OcrMode.ONLY);
        assertThat(OcrMode.from("only")).isEqualTo(OcrMode.ONLY);
    }

    @Test
    void from_unknown_returnsAuto() {
        assertThat(OcrMode.from("INVALID")).isEqualTo(OcrMode.AUTO);
        assertThat(OcrMode.from("something")).isEqualTo(OcrMode.AUTO);
    }
}
