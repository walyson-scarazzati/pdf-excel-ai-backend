package com.walyson.pdfexcelai.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExtractedRowTests {

    @Test
    void extractedRow_canBeCreated() {
        ExtractedRow row = new ExtractedRow("01/09/2025", "R$ 1.000,00", "1.000,00", "", "821", "Pix - Recebido");
        
        assertThat(row.date()).isEqualTo("01/09/2025");
        assertThat(row.value()).isEqualTo("R$ 1.000,00");
        assertThat(row.debit()).isEqualTo("1.000,00");
        assertThat(row.credit()).isEmpty();
        assertThat(row.historyCode()).isEqualTo("821");
        assertThat(row.complement()).isEqualTo("Pix - Recebido");
    }

    @Test
    void extractedRow_recordContract() {
        ExtractedRow row1 = new ExtractedRow("01/09/2025", "R$ 1.000,00", "1.000,00", "", "821", "Pix");
        ExtractedRow row2 = new ExtractedRow("01/09/2025", "R$ 1.000,00", "1.000,00", "", "821", "Pix");
        
        assertThat(row1).isEqualTo(row2);
        assertThat(row1.hashCode()).isEqualTo(row2.hashCode());
    }

    @Test
    void extractedRow_toString() {
        ExtractedRow row = new ExtractedRow("01/09/2025", "R$ 1.000,00", "1.000,00", "", "821", "Pix");
        
        assertThat(row.toString()).contains("01/09/2025", "1.000,00", "821", "Pix");
    }

    @Test
    void extractedRow_withNullFields() {
        ExtractedRow row = new ExtractedRow(null, null, null, null, null, null);
        
        assertThat(row.date()).isNull();
        assertThat(row.value()).isNull();
        assertThat(row.debit()).isNull();
    }

    @Test
    void extractedRow_withEmptyStrings() {
        ExtractedRow row = new ExtractedRow("", "", "", "", "", "");
        
        assertThat(row.date()).isEmpty();
        assertThat(row.value()).isEmpty();
        assertThat(row.debit()).isEmpty();
        assertThat(row.credit()).isEmpty();
    }
}
