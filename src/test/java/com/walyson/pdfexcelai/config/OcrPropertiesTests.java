package com.walyson.pdfexcelai.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OcrPropertiesTests {

    @Test
    void ocrProperties_canBeCreated() {
        OcrProperties props = new OcrProperties(
                true, "tesseract", "AUTO", "pt_BR", 3, 30, 100,
                10000000L, true, 1.5, 127, 2, true, 30.0,
                0.1, 0.1, 0.05
        );

        assertThat(props.enabled()).isTrue();
        assertThat(props.command()).isEqualTo("tesseract");
        assertThat(props.mode()).isEqualTo("AUTO");
        assertThat(props.language()).isEqualTo("pt_BR");
        assertThat(props.pageSegmentationMode()).isEqualTo(3);
        assertThat(props.timeoutSeconds()).isEqualTo(30);
        assertThat(props.maxPages()).isEqualTo(100);
        assertThat(props.maxImagePixels()).isEqualTo(10000000L);
        assertThat(props.preprocessEnabled()).isTrue();
        assertThat(props.contrastFactor()).isEqualTo(1.5);
        assertThat(props.threshold()).isEqualTo(127);
        assertThat(props.upscaleFactor()).isEqualTo(2);
        assertThat(props.deskewEnabled()).isTrue();
        assertThat(props.maxDeskewAngle()).isEqualTo(30.0);
    }

    @Test
    void ocrProperties_recordContract() {
        OcrProperties props1 = new OcrProperties(
                true, "cmd", "mode", "lang", 3, 30, 100, 1000000L,
                true, 1.5, 127, 2, true, 30.0, 0.1, 0.1, 0.05
        );
        OcrProperties props2 = new OcrProperties(
                true, "cmd", "mode", "lang", 3, 30, 100, 1000000L,
                true, 1.5, 127, 2, true, 30.0, 0.1, 0.1, 0.05
        );

        assertThat(props1).isEqualTo(props2);
        assertThat(props1.hashCode()).isEqualTo(props2.hashCode());
    }

    @Test
    void ocrProperties_disabled() {
        OcrProperties props = new OcrProperties(
                false, "tesseract", "AUTO", "pt_BR", 3, 30, 100, 10000000L,
                true, 1.5, 127, 2, true, 30.0, 0.1, 0.1, 0.05
        );

        assertThat(props.enabled()).isFalse();
    }

    @Test
    void ocrProperties_withDifferentLanguages() {
        OcrProperties ptBr = new OcrProperties(
                true, "tesseract", "AUTO", "pt_BR", 3, 30, 100, 10000000L,
                true, 1.5, 127, 2, true, 30.0, 0.1, 0.1, 0.05
        );
        OcrProperties enUs = new OcrProperties(
                true, "tesseract", "AUTO", "eng", 3, 30, 100, 10000000L,
                true, 1.5, 127, 2, true, 30.0, 0.1, 0.1, 0.05
        );

        assertThat(ptBr.language()).isNotEqualTo(enUs.language());
    }

    @Test
    void ocrProperties_preprocessingSettings() {
        OcrProperties props = new OcrProperties(
                true, "tesseract", "AUTO", "pt_BR", 3, 30, 100, 10000000L,
                true, 1.5, 127, 2, true, 30.0, 0.1, 0.1, 0.05
        );

        assertThat(props.preprocessEnabled()).isTrue();
        assertThat(props.contrastFactor()).isEqualTo(1.5);
        assertThat(props.threshold()).isEqualTo(127);
        assertThat(props.upscaleFactor()).isEqualTo(2);
    }

    @Test
    void ocrProperties_deskewSettings() {
        OcrProperties props = new OcrProperties(
                true, "tesseract", "AUTO", "pt_BR", 3, 30, 100, 10000000L,
                true, 1.5, 127, 2, true, 30.0, 0.1, 0.1, 0.05
        );

        assertThat(props.deskewEnabled()).isTrue();
        assertThat(props.maxDeskewAngle()).isEqualTo(30.0);
    }

    @Test
    void ocrProperties_cropSettings() {
        OcrProperties props = new OcrProperties(
                true, "tesseract", "AUTO", "pt_BR", 3, 30, 100, 10000000L,
                true, 1.5, 127, 2, true, 30.0, 0.15, 0.15, 0.10
        );

        assertThat(props.cropTopRatio()).isEqualTo(0.15);
        assertThat(props.cropBottomRatio()).isEqualTo(0.15);
        assertThat(props.cropSideRatio()).isEqualTo(0.10);
    }

    @Test
    void ocrProperties_toString() {
        OcrProperties props = new OcrProperties(
                true, "tesseract", "AUTO", "pt_BR", 3, 30, 100, 10000000L,
                true, 1.5, 127, 2, true, 30.0, 0.1, 0.1, 0.05
        );

        assertThat(props.toString()).contains("tesseract", "pt_BR");
    }
}
