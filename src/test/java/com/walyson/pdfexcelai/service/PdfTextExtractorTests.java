package com.walyson.pdfexcelai.service;

import com.walyson.pdfexcelai.config.OcrProperties;
import com.walyson.pdfexcelai.model.BankProfile;
import com.walyson.pdfexcelai.model.OcrMode;
import com.walyson.pdfexcelai.model.PdfDocumentSnapshot;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PdfTextExtractorTests {

    private OcrService disabledOcrService;
    private BankProfileResolver bankProfileResolver;
    private PdfTextExtractor extractor;

    @BeforeEach
    void setUp() {
        OcrProperties disabledProps = new OcrProperties(false, "tesseract", "auto", "por", 6, 30, 0,
                10_000_000L, false, 1.5, 180, 2, false, 5.0, 0.0, 0.0, 0.0);
        disabledOcrService = new OcrService(disabledProps);
        bankProfileResolver = new BankProfileResolver();
        extractor = new PdfTextExtractor(disabledOcrService, bankProfileResolver, 1, 72f);
    }

    private byte[] buildSimplePdf(String text) throws IOException {
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.newLineAtOffset(50, 700);
                cs.showText(text);
                cs.endText();
            }
            doc.save(out);
            return out.toByteArray();
        }
    }

    private byte[] buildMultiPagePdf(String... texts) throws IOException {
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (String text : texts) {
                PDPage page = new PDPage();
                doc.addPage(page);
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    cs.beginText();
                    cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                    cs.newLineAtOffset(50, 700);
                    cs.showText(text);
                    cs.endText();
                }
            }
            doc.save(out);
            return out.toByteArray();
        }
    }

    // ---- extract(MultipartFile) delegates to extract(file, true) ----

    @Test
    void extract_simplePdf_returnsSnapshot() throws IOException {
        byte[] pdfBytes = buildSimplePdf("01/09/2025 TED RECEBIDA 5.000,00");
        MockMultipartFile file = new MockMultipartFile("file", "extrato.pdf", "application/pdf", pdfBytes);

        PdfDocumentSnapshot snap = extractor.extract(file);

        assertThat(snap).isNotNull();
        assertThat(snap.pageCount()).isEqualTo(1);
        assertThat(snap.sourceType()).isEqualTo("pdf");
    }

    @Test
    void extract_simplePdf_withIncludePreviewImages_false() throws IOException {
        byte[] pdfBytes = buildSimplePdf("01/09/2025 TED RECEBIDA 5.000,00");
        MockMultipartFile file = new MockMultipartFile("file", "extrato.pdf", "application/pdf", pdfBytes);

        PdfDocumentSnapshot snap = extractor.extract(file, false);

        assertThat(snap.pageImages()).isEmpty();
        assertThat(snap.pageCount()).isEqualTo(1);
    }

    @Test
    void extract_simplePdf_textIsAvailable() throws IOException {
        byte[] pdfBytes = buildSimplePdf("Extrato Bancario 01/09/2025 saldo pix");
        MockMultipartFile file = new MockMultipartFile("file", "extrato.pdf", "application/pdf", pdfBytes);

        PdfDocumentSnapshot snap = extractor.extract(file, false);

        assertThat(snap.textAvailable()).isTrue();
        assertThat(snap.rawText()).contains("01/09/2025");
    }

    @Test
    void extract_emptyPdf_textIsNotAvailable() throws IOException {
        byte[] pdfBytes = buildSimplePdf("");
        MockMultipartFile file = new MockMultipartFile("file", "empty.pdf", "application/pdf", pdfBytes);

        PdfDocumentSnapshot snap = extractor.extract(file, false);

        assertThat(snap.pageCount()).isEqualTo(1);
        assertThat(snap.ocrUsed()).isFalse();
    }

    @Test
    void extract_multiPagePdf_pageCountCorrect() throws IOException {
        byte[] pdfBytes = buildMultiPagePdf("Page 1 content 01/01/2025 saldo 100,00",
                "Page 2 content 02/01/2025 pix 200,00",
                "Page 3 content 03/01/2025 boleto 300,00");
        MockMultipartFile file = new MockMultipartFile("file", "multi.pdf", "application/pdf", pdfBytes);

        PdfDocumentSnapshot snap = extractor.extract(file, false);

        assertThat(snap.pageCount()).isEqualTo(3);
        assertThat(snap.pageTexts()).hasSize(3);
    }

    @Test
    void extract_santanderPdf_detectsBankProfile() throws IOException {
        byte[] pdfBytes = buildSimplePdf("Santander ContaMax Extrato Consolidado Getnet");
        MockMultipartFile file = new MockMultipartFile("file", "extrato_santander.pdf", "application/pdf", pdfBytes);

        PdfDocumentSnapshot snap = extractor.extract(file, false);

        assertThat(snap.bankProfile()).isEqualTo(BankProfile.SANTANDER);
    }

    @Test
    void extract_bancoDoBrasilPdf_detectsBankProfile() throws IOException {
        byte[] pdfBytes = buildSimplePdf("BANCO DO BRASIL S.A. Extrato de Conta Corrente");
        MockMultipartFile file = new MockMultipartFile("file", "extrato_bb.pdf", "application/pdf", pdfBytes);

        PdfDocumentSnapshot snap = extractor.extract(file, false);

        assertThat(snap.bankProfile()).isEqualTo(BankProfile.BANCO_DO_BRASIL);
    }

    @Test
    void extract_unknownBankPdf_detectsUnknown() throws IOException {
        byte[] pdfBytes = buildSimplePdf("Extrato generico sem indicador de banco");
        MockMultipartFile file = new MockMultipartFile("file", "extrato.pdf", "application/pdf", pdfBytes);

        PdfDocumentSnapshot snap = extractor.extract(file, false);

        assertThat(snap.bankProfile()).isEqualTo(BankProfile.UNKNOWN);
    }

    @Test
    void extract_ocrNotUsed_whenOcrDisabled() throws IOException {
        byte[] pdfBytes = buildSimplePdf("01/09/2025 TED 1.000,00");
        MockMultipartFile file = new MockMultipartFile("file", "extrato.pdf", "application/pdf", pdfBytes);

        PdfDocumentSnapshot snap = extractor.extract(file, false);

        assertThat(snap.ocrUsed()).isFalse();
    }

    @Test
    void extract_previewText_isTruncated() throws IOException {
        String longText = "01/09/2025 ".repeat(300);
        byte[] pdfBytes = buildSimplePdf(longText);
        MockMultipartFile file = new MockMultipartFile("file", "extrato.pdf", "application/pdf", pdfBytes);

        PdfDocumentSnapshot snap = extractor.extract(file, false);

        assertThat(snap.previewText()).hasSizeLessThanOrEqualTo(2000);
    }

    @Test
    void extract_image_jpg_usesImagePath() throws IOException {
        // A 1x1 white JPEG
        byte[] jpegBytes = createTinyJpeg();
        MockMultipartFile file = new MockMultipartFile("file", "extrato.jpg", "image/jpeg", jpegBytes);

        PdfDocumentSnapshot snap = extractor.extract(file, false);

        assertThat(snap.sourceType()).isEqualTo("image");
        assertThat(snap.pageCount()).isEqualTo(1);
    }

    @Test
    void extract_image_png_usesImagePath() throws IOException {
        byte[] pngBytes = createTinyPng();
        MockMultipartFile file = new MockMultipartFile("file", "scan.png", "image/png", pngBytes);

        PdfDocumentSnapshot snap = extractor.extract(file, false);

        assertThat(snap.sourceType()).isEqualTo("image");
    }

    @Test
    void extract_image_detectedByContentType() throws IOException {
        byte[] pngBytes = createTinyPng();
        MockMultipartFile file = new MockMultipartFile("file", "scan.data", "image/png", pngBytes);

        PdfDocumentSnapshot snap = extractor.extract(file, false);

        assertThat(snap.sourceType()).isEqualTo("image");
    }

    @Test
    void extract_invalidImageBytes_throwsIOException() {
        MockMultipartFile file = new MockMultipartFile("file", "scan.jpg", "image/jpeg", new byte[]{0, 1, 2, 3});
        assertThatThrownBy(() -> extractor.extract(file, false))
                .isInstanceOf(IOException.class);
    }

    @Test
    void extract_withPreviewImages_includesDataUrl() throws IOException {
        byte[] pdfBytes = buildSimplePdf("01/09/2025 TED 1.000,00");
        MockMultipartFile file = new MockMultipartFile("file", "extrato.pdf", "application/pdf", pdfBytes);

        PdfDocumentSnapshot snap = extractor.extract(file, true);

        assertThat(snap.pageImages()).hasSize(1);
        assertThat(snap.pageImages().get(0)).startsWith("data:image/png;base64,");
    }

    @Test
    void extract_ocrMode_resolvedFromBankProfile() throws IOException {
        byte[] pdfBytes = buildSimplePdf("Santander ContaMax extrato");
        MockMultipartFile file = new MockMultipartFile("file", "extrato.pdf", "application/pdf", pdfBytes);

        PdfDocumentSnapshot snap = extractor.extract(file, false);

        // Santander resolves to FIRST when configured mode is AUTO
        assertThat(snap.ocrMode()).isEqualTo(OcrMode.FIRST);
    }

    private byte[] createTinyPng() throws IOException {
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(1, 1,
                java.awt.image.BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(img, "png", baos);
        return baos.toByteArray();
    }

    private byte[] createTinyJpeg() throws IOException {
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(1, 1,
                java.awt.image.BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(img, "jpg", baos);
        return baos.toByteArray();
    }
}
