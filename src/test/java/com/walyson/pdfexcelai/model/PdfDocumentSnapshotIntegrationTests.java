package com.walyson.pdfexcelai.model;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PdfDocumentSnapshotIntegrationTests {

    @Test
    void pdfDocumentSnapshot_allFieldsAccessible() {
        List<String> pages = List.of("page1", "page2", "page3");
        List<String> images = List.of("img1", "img2");
        
        PdfDocumentSnapshot snapshot = new PdfDocumentSnapshot(
                "raw", "normalized", "preview", pages, images,
                3, true, true, "pdf", BankProfile.SANTANDER, OcrMode.FIRST
        );

        assertThat(snapshot.rawText()).isEqualTo("raw");
        assertThat(snapshot.normalizedText()).isEqualTo("normalized");
        assertThat(snapshot.previewText()).isEqualTo("preview");
        assertThat(snapshot.pageTexts()).isSameAs(pages);
        assertThat(snapshot.pageImages()).isSameAs(images);
        assertThat(snapshot.pageCount()).isEqualTo(3);
        assertThat(snapshot.textAvailable()).isTrue();
        assertThat(snapshot.ocrUsed()).isTrue();
        assertThat(snapshot.sourceType()).isEqualTo("pdf");
        assertThat(snapshot.bankProfile()).isEqualTo(BankProfile.SANTANDER);
        assertThat(snapshot.ocrMode()).isEqualTo(OcrMode.FIRST);
    }

    @Test
    void pdfDocumentSnapshot_largePageList() {
        List<String> pages = List.of(
                "page1", "page2", "page3", "page4", "page5", 
                "page6", "page7", "page8", "page9", "page10"
        );
        
        PdfDocumentSnapshot snapshot = new PdfDocumentSnapshot(
                "raw", "normalized", "preview", pages, List.of(),
                10, true, false, "pdf", BankProfile.BANCO_DO_BRASIL, OcrMode.AUTO
        );

        assertThat(snapshot.pageTexts()).hasSize(10);
        assertThat(snapshot.pageCount()).isEqualTo(10);
    }

    @Test
    void pdfDocumentSnapshot_multipleDocumentTypes() {
        PdfDocumentSnapshot pdfSnapshot = new PdfDocumentSnapshot(
                "pdf content", "normalized", "preview", List.of(), List.of(),
                1, true, false, "pdf", BankProfile.UNKNOWN, OcrMode.AUTO
        );
        PdfDocumentSnapshot csvSnapshot = new PdfDocumentSnapshot(
                "csv content", "normalized", "preview", List.of(), List.of(),
                1, true, false, "csv", BankProfile.UNKNOWN, OcrMode.AUTO
        );

        assertThat(pdfSnapshot.sourceType()).isEqualTo("pdf");
        assertThat(csvSnapshot.sourceType()).isEqualTo("csv");
        assertThat(pdfSnapshot.sourceType()).isNotEqualTo(csvSnapshot.sourceType());
    }

    @Test
    void pdfDocumentSnapshot_ocrConfiguration() {
        PdfDocumentSnapshot ocrEnabled = new PdfDocumentSnapshot(
                "text", "text", "preview", List.of(), List.of(),
                1, true, true, "pdf", BankProfile.UNKNOWN, OcrMode.AUTO
        );
        PdfDocumentSnapshot ocrDisabled = new PdfDocumentSnapshot(
                "text", "text", "preview", List.of(), List.of(),
                1, true, false, "pdf", BankProfile.UNKNOWN, OcrMode.AUTO
        );

        assertThat(ocrEnabled.ocrUsed()).isTrue();
        assertThat(ocrDisabled.ocrUsed()).isFalse();
    }
}
