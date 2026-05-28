package com.walyson.pdfexcelai.model;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ModelComprehensiveTests {

    @Test
    void bankProfile_allEnumValues_haveDefaults() {
        for (BankProfile profile : BankProfile.values()) {
            assertThat(profile).isNotNull();
            OcrMode mode = profile.resolveOcrMode(null);
            assertThat(mode).isNotNull();
        }
    }

    @Test
    void extractedRow_allCombinations() {
        // Credit transaction
        ExtractedRow credit = new ExtractedRow("01/09", "R$ 100", "", "100", "001", "Cred");
        assertThat(credit.debit()).isEmpty();
        assertThat(credit.credit()).isEqualTo("100");

        // Debit transaction
        ExtractedRow debit = new ExtractedRow("01/09", "R$ 100", "100", "", "001", "Deb");
        assertThat(debit.debit()).isEqualTo("100");
        assertThat(debit.credit()).isEmpty();

        // Bidirectional
        ExtractedRow both = new ExtractedRow("01/09", "R$ 100", "50", "50", "001", "Both");
        assertThat(both.debit()).isEqualTo("50");
        assertThat(both.credit()).isEqualTo("50");

        // Neither
        ExtractedRow none = new ExtractedRow("01/09", "R$ 100", "", "", "001", "None");
        assertThat(none.debit()).isEmpty();
        assertThat(none.credit()).isEmpty();
    }

    @Test
    void pdfDocumentSnapshot_variations() {
        // With OCR
        PdfDocumentSnapshot withOcr = new PdfDocumentSnapshot(
                "raw", "norm", "prev", List.of("p1"), List.of("i1"),
                1, true, true, "pdf", BankProfile.BANCO_DO_BRASIL, OcrMode.FIRST
        );
        assertThat(withOcr.ocrUsed()).isTrue();
        assertThat(withOcr.textAvailable()).isTrue();

        // Without OCR
        PdfDocumentSnapshot noOcr = new PdfDocumentSnapshot(
                "raw", "norm", "prev", List.of("p1"), List.of(),
                1, false, false, "csv", BankProfile.UNKNOWN, OcrMode.AUTO
        );
        assertThat(noOcr.ocrUsed()).isFalse();
        assertThat(noOcr.textAvailable()).isFalse();
    }

    @Test
    void extractionResult_complexScenario() {
        List<ExtractedRow> rows = List.of(
                new ExtractedRow("01/09/2025", "R$ 100,00", "100,00", "", "001", "Row1"),
                new ExtractedRow("02/09/2025", "R$ 200,00", "", "200,00", "002", "Row2")
        );

        ExtractionResult result = new ExtractionResult(
                "statement.pdf", "001-123", "09/2025", 10, 2, rows,
                List.of("img1", "img2"), true, true, "AI", "Preview"
        );

        assertThat(result.totalRows()).isEqualTo(2);
        assertThat(result.rows()).hasSize(2);
        assertThat(result.pageCount()).isGreaterThan(result.totalRows());
        assertThat(result.aiUsed()).isTrue();
        assertThat(result.ocrUsed()).isTrue();
    }

    @Test
    void accountingClassificationRule_scenarios() {
        // High priority income
        AccountingClassificationRule income = new AccountingClassificationRule(
                "Income", "SALARY,BONUS", "C", "1100", "5100", "100", 100
        );
        assertThat(income.direction()).isEqualTo("C");
        assertThat(income.priority()).isEqualTo(100);

        // Low priority misc
        AccountingClassificationRule misc = new AccountingClassificationRule(
                "Misc", "OTHER", "D", "1999", "2999", "999", 1
        );
        assertThat(misc.direction()).isEqualTo("D");
        assertThat(misc.priority()).isEqualTo(1);

        assertThat(income.priority()).isGreaterThan(misc.priority());
    }

    @Test
    void ocrMode_allModes() {
        for (OcrMode mode : OcrMode.values()) {
            assertThat(mode).isNotNull();
            assertThat(OcrMode.from(mode.name())).isEqualTo(mode);
            assertThat(OcrMode.from(mode.name().toLowerCase())).isEqualTo(mode);
        }
    }
}
