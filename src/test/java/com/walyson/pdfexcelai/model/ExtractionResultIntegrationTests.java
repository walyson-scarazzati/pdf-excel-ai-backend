package com.walyson.pdfexcelai.model;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExtractionResultIntegrationTests {

    @Test
    void extractionResult_fullScenario() {
        List<ExtractedRow> rows = List.of(
                new ExtractedRow("01/09/2025", "R$ 100,00", "100,00", "", "001", "Credit"),
                new ExtractedRow("02/09/2025", "R$ 200,00", "", "200,00", "002", "Debit"),
                new ExtractedRow("03/09/2025", "R$ 300,00", "300,00", "", "003", "Transfer")
        );

        ExtractionResult result = new ExtractionResult(
                "statement_sep_2025.pdf",
                "001-12345-6",
                "09/2025",
                5,
                3,
                rows,
                List.of("img1.png", "img2.png"),
                true,
                true,
                "AI_EXTRACTION",
                "Preview of extracted data..."
        );

        assertThat(result.fileName()).isEqualTo("statement_sep_2025.pdf");
        assertThat(result.accountInfo()).isEqualTo("001-12345-6");
        assertThat(result.extractPeriod()).isEqualTo("09/2025");
        assertThat(result.pageCount()).isEqualTo(5);
        assertThat(result.totalRows()).isEqualTo(3);
        assertThat(result.rows()).hasSize(3);
        assertThat(result.pageImages()).hasSize(2);
        assertThat(result.aiUsed()).isTrue();
        assertThat(result.ocrUsed()).isTrue();
        assertThat(result.extractionMode()).isEqualTo("AI_EXTRACTION");
        assertThat(result.previewText()).isEqualTo("Preview of extracted data...");
    }

    @Test
    void extractionResult_multipleExtractionModes() {
        ExtractionResult aiExtracted = new ExtractionResult(
                "file.pdf", "account", "09/2025", 1, 0, List.of(),
                List.of(), true, false, "AI_EXTRACTION", "preview"
        );
        ExtractionResult heuristicExtracted = new ExtractionResult(
                "file.pdf", "account", "09/2025", 1, 0, List.of(),
                List.of(), false, false, "HEURISTIC", "preview"
        );

        assertThat(aiExtracted.extractionMode()).isEqualTo("AI_EXTRACTION");
        assertThat(heuristicExtracted.extractionMode()).isEqualTo("HEURISTIC");
    }

    @Test
    void extractionResult_manyRows() {
        List<ExtractedRow> manyRows = List.of(
                new ExtractedRow("01/09/2025", "R$ 100,00", "100,00", "", "001", "Row1"),
                new ExtractedRow("02/09/2025", "R$ 200,00", "", "200,00", "002", "Row2"),
                new ExtractedRow("03/09/2025", "R$ 300,00", "300,00", "", "003", "Row3"),
                new ExtractedRow("04/09/2025", "R$ 400,00", "", "400,00", "004", "Row4"),
                new ExtractedRow("05/09/2025", "R$ 500,00", "500,00", "", "005", "Row5")
        );

        ExtractionResult result = new ExtractionResult(
                "file.pdf", "account", "09/2025", 10, 5, manyRows,
                List.of(), false, false, "HEURISTIC", "preview"
        );

        assertThat(result.totalRows()).isEqualTo(5);
        assertThat(result.rows()).hasSize(5);
    }
}
