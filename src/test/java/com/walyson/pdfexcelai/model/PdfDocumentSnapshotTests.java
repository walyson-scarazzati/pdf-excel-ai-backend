package com.walyson.pdfexcelai.model;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PdfDocumentSnapshotTests {

    @Test
    void pdfDocumentSnapshot_canBeCreated() {
        PdfDocumentSnapshot snapshot = new PdfDocumentSnapshot(
                "raw text", "normalized text", "preview",
                List.of("page1", "page2"), List.of("image1"),
                2, true, false, "pdf", BankProfile.BANCO_DO_BRASIL, OcrMode.AUTO
        );

        assertThat(snapshot.rawText()).isEqualTo("raw text");
        assertThat(snapshot.normalizedText()).isEqualTo("normalized text");
        assertThat(snapshot.previewText()).isEqualTo("preview");
        assertThat(snapshot.pageTexts()).hasSize(2);
        assertThat(snapshot.pageImages()).hasSize(1);
        assertThat(snapshot.pageCount()).isEqualTo(2);
        assertThat(snapshot.textAvailable()).isTrue();
        assertThat(snapshot.ocrUsed()).isFalse();
        assertThat(snapshot.sourceType()).isEqualTo("pdf");
        assertThat(snapshot.bankProfile()).isEqualTo(BankProfile.BANCO_DO_BRASIL);
        assertThat(snapshot.ocrMode()).isEqualTo(OcrMode.AUTO);
    }

    @Test
    void pdfDocumentSnapshot_recordContract() {
        PdfDocumentSnapshot snap1 = new PdfDocumentSnapshot(
                "text", "normalized", "preview",
                List.of("page1"), List.of(), 1, true, false, "pdf", BankProfile.UNKNOWN, OcrMode.AUTO
        );
        PdfDocumentSnapshot snap2 = new PdfDocumentSnapshot(
                "text", "normalized", "preview",
                List.of("page1"), List.of(), 1, true, false, "pdf", BankProfile.UNKNOWN, OcrMode.AUTO
        );

        assertThat(snap1).isEqualTo(snap2);
        assertThat(snap1.hashCode()).isEqualTo(snap2.hashCode());
    }

    @Test
    void pdfDocumentSnapshot_withNullFields() {
        PdfDocumentSnapshot snapshot = new PdfDocumentSnapshot(
                null, null, null, null, null, 0, false, false, null, null, null
        );

        assertThat(snapshot.rawText()).isNull();
        assertThat(snapshot.normalizedText()).isNull();
        assertThat(snapshot.pageTexts()).isNull();
    }

    @Test
    void pdfDocumentSnapshot_withEmptyLists() {
        PdfDocumentSnapshot snapshot = new PdfDocumentSnapshot(
                "text", "text", "text",
                List.of(), List.of(), 1, true, false, "pdf", BankProfile.UNKNOWN, OcrMode.AUTO
        );

        assertThat(snapshot.pageTexts()).isEmpty();
        assertThat(snapshot.pageImages()).isEmpty();
    }

    @Test
    void pdfDocumentSnapshot_toString() {
        PdfDocumentSnapshot snapshot = new PdfDocumentSnapshot(
                "text", "normalized", "preview",
                List.of("page1"), List.of(), 1, true, false, "pdf", BankProfile.BANCO_DO_BRASIL, OcrMode.AUTO
        );

        assertThat(snapshot.toString()).contains("text", "normalized", "BANCO_DO_BRASIL");
    }
}
