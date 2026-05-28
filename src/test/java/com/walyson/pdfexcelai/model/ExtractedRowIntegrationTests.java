package com.walyson.pdfexcelai.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExtractedRowIntegrationTests {

    @Test
    void extractedRow_creditTransaction() {
        ExtractedRow row = new ExtractedRow(
                "01/09/2025",
                "R$ 1.000,00",
                "",
                "1.000,00",
                "821",
                "Pix - Recebido THAIS KARINA P"
        );

        assertThat(row.date()).isEqualTo("01/09/2025");
        assertThat(row.value()).isEqualTo("R$ 1.000,00");
        assertThat(row.debit()).isEmpty();
        assertThat(row.credit()).isEqualTo("1.000,00");
        assertThat(row.historyCode()).isEqualTo("821");
        assertThat(row.complement()).isEqualTo("Pix - Recebido THAIS KARINA P");
    }

    @Test
    void extractedRow_debitTransaction() {
        ExtractedRow row = new ExtractedRow(
                "02/09/2025",
                "R$ 500,00",
                "500,00",
                "",
                "109",
                "Pagamento de Boleto MASTER DIESEL"
        );

        assertThat(row.date()).isEqualTo("02/09/2025");
        assertThat(row.value()).isEqualTo("R$ 500,00");
        assertThat(row.debit()).isEqualTo("500,00");
        assertThat(row.credit()).isEmpty();
        assertThat(row.historyCode()).isEqualTo("109");
        assertThat(row.complement()).isEqualTo("Pagamento de Boleto MASTER DIESEL");
    }

    @Test
    void extractedRow_multipleTransactions() {
        ExtractedRow[] transactions = {
                new ExtractedRow("01/09/2025", "R$ 100,00", "100,00", "", "001", "Credit 1"),
                new ExtractedRow("02/09/2025", "R$ 200,00", "", "200,00", "002", "Debit 1"),
                new ExtractedRow("03/09/2025", "R$ 300,00", "300,00", "", "003", "Credit 2"),
                new ExtractedRow("04/09/2025", "R$ 400,00", "", "400,00", "004", "Debit 2"),
                new ExtractedRow("05/09/2025", "R$ 500,00", "500,00", "", "005", "Credit 3")
        };

        assertThat(transactions).hasSize(5);
        for (int i = 0; i < transactions.length; i++) {
            ExtractedRow row = transactions[i];
            assertThat(row.date()).isNotEmpty();
            assertThat(row.value()).isNotEmpty();
            assertThat(row.historyCode()).isNotEmpty();
        }
    }

    @Test
    void extractedRow_rowsWithDifferentAmountFormats() {
        ExtractedRow row1 = new ExtractedRow("01/09", "R$ 1.000,00", "1.000,00", "", "001", "Desc");
        ExtractedRow row2 = new ExtractedRow("02/09", "R$ 10.000,00", "10.000,00", "", "002", "Desc");
        ExtractedRow row3 = new ExtractedRow("03/09", "R$ 100.000,00", "100.000,00", "", "003", "Desc");

        assertThat(row1.value()).isEqualTo("R$ 1.000,00");
        assertThat(row2.value()).isEqualTo("R$ 10.000,00");
        assertThat(row3.value()).isEqualTo("R$ 100.000,00");
    }

    @Test
    void extractedRow_comparisons() {
        ExtractedRow row1 = new ExtractedRow("01/09/2025", "R$ 100,00", "100,00", "", "001", "Desc");
        ExtractedRow row2 = new ExtractedRow("01/09/2025", "R$ 100,00", "100,00", "", "001", "Desc");
        ExtractedRow row3 = new ExtractedRow("02/09/2025", "R$ 200,00", "200,00", "", "002", "Other");

        assertThat(row1).isEqualTo(row2);
        assertThat(row1).isNotEqualTo(row3);
        assertThat(row1.hashCode()).isEqualTo(row2.hashCode());
    }
}
