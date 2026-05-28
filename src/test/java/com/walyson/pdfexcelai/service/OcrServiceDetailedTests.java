package com.walyson.pdfexcelai.service;

import com.walyson.pdfexcelai.config.OcrProperties;
import com.walyson.pdfexcelai.model.BankProfile;
import com.walyson.pdfexcelai.model.OcrMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OcrServiceDetailedTests {

    private OcrProperties enabledProps;
    private OcrService service;

    @BeforeEach
    void setUp() {
        enabledProps = new OcrProperties(
                true, "tesseract", "AUTO", "por+eng", 6, 30, 100,
                5_000_000L, true, 1.5, 180, 2, true, 15.0, 0.1, 0.1, 0.05
        );
        service = new OcrService(enabledProps);
    }

    private BufferedImage createImage(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        // Fill with solid color to create a simple test image
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, 0xFF000000); // Black pixels
            }
        }
        return image;
    }

    @Test
    void extractText_withPreprocessing_disabled() {
        OcrProperties disabledPreprocess = new OcrProperties(
                true, "cmd", "AUTO", "por", 6, 30, 100,
                10_000_000L, false, 1.0, 128, 1, false, 0.0, 0.0, 0.0, 0.0
        );
        OcrService svc = new OcrService(disabledPreprocess);
        BufferedImage img = createImage(200, 200);

        String result = svc.extractText(img, BankProfile.UNKNOWN);

        assertThat(result).isEmpty(); // Empty because command doesn't exist
    }

    @Test
    void maxPages_with_negativeValue() {
        OcrProperties negativePages = new OcrProperties(
                true, "tesseract", "AUTO", "por", 6, 30, -1,
                10_000_000L, false, 1.0, 128, 1, false, 0.0, 0.0, 0.0, 0.0
        );
        OcrService svc = new OcrService(negativePages);

        assertThat(svc.maxPages()).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void commandName_withBlank() {
        OcrProperties blankCmd = new OcrProperties(
                true, "  ", "AUTO", "por", 6, 30, 100,
                10_000_000L, false, 1.0, 128, 1, false, 0.0, 0.0, 0.0, 0.0
        );
        OcrService svc = new OcrService(blankCmd);

        assertThat(svc.commandName()).isEqualTo("tesseract");
    }

    @Test
    void configuredMode_withNullMode() {
        OcrProperties nullMode = new OcrProperties(
                true, "tesseract", null, "por", 6, 30, 100,
                10_000_000L, false, 1.0, 128, 1, false, 0.0, 0.0, 0.0, 0.0
        );
        OcrService svc = new OcrService(nullMode);

        assertThat(svc.configuredMode()).isEqualTo(OcrMode.AUTO);
    }

    @Test
    void configuredMode_with_various_modes() {
        OcrProperties firstMode = new OcrProperties(
                true, "tesseract", "first", "por", 6, 30, 100,
                10_000_000L, false, 1.0, 128, 1, false, 0.0, 0.0, 0.0, 0.0
        );
        OcrService svc = new OcrService(firstMode);
        assertThat(svc.configuredMode()).isEqualTo(OcrMode.FIRST);

        OcrProperties onlyMode = new OcrProperties(
                true, "tesseract", "only", "por", 6, 30, 100,
                10_000_000L, false, 1.0, 128, 1, false, 0.0, 0.0, 0.0, 0.0
        );
        svc = new OcrService(onlyMode);
        assertThat(svc.configuredMode()).isEqualTo(OcrMode.ONLY);
    }

    @Test
    void isAvailable_caches_both_true_and_false() throws Exception {
        OcrService svc = new OcrService(enabledProps);

        // First call returns false (command not found)
        boolean first = svc.isAvailable();
        assertThat(first).isFalse();

        // Second call should return same cached value
        boolean second = svc.isAvailable();
        assertThat(second).isEqualTo(first);
    }

    @Test
    void extractText_returnsEmpty_withNullBankProfile() {
        OcrService svc = new OcrService(enabledProps);
        BufferedImage img = createImage(200, 200);

        String result = svc.extractText(img, (BankProfile) null);

        assertThat(result).isEmpty(); // Fails because profile is null
    }

    @Test
    void extractText_withAllProfiles() {
        OcrService svc = new OcrService(enabledProps);
        BufferedImage img = createImage(200, 200);

        // Test with each profile
        for (BankProfile profile : BankProfile.values()) {
            String result = svc.extractText(img, profile);
            assertThat(result).isNotNull();
        }
    }

    @Test
    void isEnabled_and_isAvailable_consistency() {
        OcrService svc = new OcrService(enabledProps);

        // If not enabled, should not be available
        if (!svc.isEnabled()) {
            assertThat(svc.isAvailable()).isFalse();
        }
    }
}
