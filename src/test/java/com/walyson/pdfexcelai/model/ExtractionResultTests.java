package com.walyson.pdfexcelai.model;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExtractionResultTests {

    @Test
    void extractionResult_canBeCreated() {
        List<ExtractedRow> rows = List.of(
                new ExtractedRow("01/09/2025", "R$ 1.000,00", "1.000,00", "", "821", "Pix")
        );

        ExtractionResult result = new ExtractionResult(
                "statement.pdf", "001-12345", "09/2025", 5, 1, rows,
                List.of("image1"), true, false, "AI_EXTRACTION", "preview text"
        );

        assertThat(result.fileName()).isEqualTo("statement.pdf");
        assertThat(result.accountInfo()).isEqualTo("001-12345");
        assertThat(result.extractPeriod()).isEqualTo("09/2025");
        assertThat(result.pageCount()).isEqualTo(5);
        assertThat(result.totalRows()).isEqualTo(1);
        assertThat(result.rows()).hasSize(1);
        assertThat(result.pageImages()).hasSize(1);
        assertThat(result.aiUsed()).isTrue();
        assertThat(result.ocrUsed()).isFalse();
        assertThat(result.extractionMode()).isEqualTo("AI_EXTRACTION");
        assertThat(result.previewText()).isEqualTo("preview text");
    }

    @Test
    void extractionResult_recordContract() {
        List<ExtractedRow> rows = List.of();
        ExtractionResult result1 = new ExtractionResult(
                "file.pdf", "001-123", "09/2025", 1, 0, rows,
                List.of(), false, false, "HEURISTIC", "preview"
        );
        ExtractionResult result2 = new ExtractionResult(
                "file.pdf", "001-123", "09/2025", 1, 0, rows,
                List.of(), false, false, "HEURISTIC", "preview"
        );

        assertThat(result1).isEqualTo(result2);
        assertThat(result1.hashCode()).isEqualTo(result2.hashCode());
    }

    @Test
    void extractionResult_withNullFields() {
        ExtractionResult result = new ExtractionResult(
                null, null, null, 0, 0, null, null, false, false, null, null
        );

        assertThat(result.fileName()).isNull();
        assertThat(result.accountInfo()).isNull();
        assertThat(result.rows()).isNull();
    }

    @Test
    void extractionResult_withEmptyLists() {
        ExtractionResult result = new ExtractionResult(
                "file.pdf", "account", "09/2025", 1, 0, List.of(),
                List.of(), true, true, "AI", "preview"
        );

        assertThat(result.rows()).isEmpty();
        assertThat(result.pageImages()).isEmpty();
    }

    @Test
    void extractionResult_toString() {
        ExtractionResult result = new ExtractionResult(
                "statement.pdf", "001-123", "09/2025", 5, 1, List.of(),
                List.of(), true, false, "AI", "preview"
        );

        assertThat(result.toString()).contains("statement.pdf", "001-123", "09/2025", "AI");
    }
}
