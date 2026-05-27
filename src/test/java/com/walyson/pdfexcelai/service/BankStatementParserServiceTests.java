package com.walyson.pdfexcelai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.walyson.pdfexcelai.model.BankProfile;
import com.walyson.pdfexcelai.model.ExtractedRow;
import com.walyson.pdfexcelai.model.OcrMode;
import com.walyson.pdfexcelai.model.PdfDocumentSnapshot;
import java.util.List;
import org.junit.jupiter.api.Test;

class BankStatementParserServiceTests {

    private final BankStatementParserService service = new BankStatementParserService();

    @Test
    void parsesBancoDoBrasilLineUsingOcrOnlyText() {
        PdfDocumentSnapshot snapshot = new PdfDocumentSnapshot(
                "",
                "10/2025\n01/10/2025 0000 14175 976 Credito em Conta 35.632.527 970,00 C",
                "",
                List.of(),
                List.of(),
                1,
                true,
                true,
                "pdf",
                BankProfile.BANCO_DO_BRASIL,
                OcrMode.ONLY
        );

        List<ExtractedRow> rows = service.parse(snapshot);

        assertEquals(1, rows.size());
        assertEquals("01/10/2025", rows.get(0).date());
        assertEquals("970,00", rows.get(0).credit());
        assertEquals("14175", rows.get(0).historyCode());
    }

    @Test
    void parsesBancoDoBrasilColumnarOcrRows() {
        PdfDocumentSnapshot snapshot = new PdfDocumentSnapshot(
                "",
                "10/2025\nCOL|01102025|0000|14175|Credito em Conta|970,00|C\nCOL|06102025|13113|124|Debito Servico Cobranca|4,55|D",
                "",
                List.of(),
                List.of(),
                1,
                true,
                true,
                "pdf",
                BankProfile.BANCO_DO_BRASIL,
                OcrMode.ONLY
        );

        List<ExtractedRow> rows = service.parse(snapshot);

        assertEquals(2, rows.size());
        assertEquals("01/10/2025", rows.get(0).date());
        assertEquals("970,00", rows.get(0).credit());
        assertEquals("14175", rows.get(0).historyCode());
        assertEquals("Credito em Conta", rows.get(0).complement());
        assertEquals("06/10/2025", rows.get(1).date());
        assertEquals("4,55", rows.get(1).debit());
        assertEquals("Debito Servico Cobranca", rows.get(1).complement());
    }

    @Test
    void fallsBackToLineParsingWhenColumnDirectionIsInvalid() {
        String normalizedText = """
            10/2025
            COL|01102025|0000|14175|Credito em Conta|970,00|?
            COL|06102025|13113|124|Debito Servico Cobranca|4,55|?
            01/10/2025 0000 14175 976 Credito em Conta 35.632.527 970,00 C
            06/10/2025 0000 13113 124 Debito Servico Cobranca 4,55 D
            """;

        PdfDocumentSnapshot snapshot = new PdfDocumentSnapshot(
                "",
            normalizedText,
                "",
                List.of(),
                List.of(),
                1,
                true,
                true,
                "pdf",
                BankProfile.BANCO_DO_BRASIL,
                OcrMode.ONLY
        );

        List<ExtractedRow> rows = service.parse(snapshot);

        assertEquals(2, rows.size());
        assertEquals("01/10/2025", rows.get(0).date());
        assertEquals("970,00", rows.get(0).credit());
        assertEquals("06/10/2025", rows.get(1).date());
        assertEquals("4,55", rows.get(1).debit());
    }

    @Test
    void parsesSantanderCreditAndDebitLines() {
        PdfDocumentSnapshot snapshot = new PdfDocumentSnapshot(
                "",
                "01/2026\n05/01 PIX RECEBIDO 05222245802 - 1.100,00\n30/01 PREST. EMPREST/FINANC. C/ GARANTIA 040500 7.863,55-",
                "",
                List.of(),
                List.of(),
                1,
                true,
                true,
                "pdf",
                BankProfile.SANTANDER,
                OcrMode.FIRST
        );

        List<ExtractedRow> rows = service.parse(snapshot);

        assertEquals(2, rows.size());
        assertEquals("05/01/2026", rows.get(0).date());
        assertEquals("1.100,00", rows.get(0).credit());
        assertEquals("30/01/2026", rows.get(1).date());
        assertEquals("7.863,55", rows.get(1).debit());
    }

    @Test
    void stripsNumericNoiseFromBancoDoBrasilComplement() {
        PdfDocumentSnapshot snapshot = new PdfDocumentSnapshot(
                "",
                "10/2025\n01/10/2025 0000 612 Recebimento fornecedor 2.229.415/0001-10 1.200,00 C",
                "",
                List.of(),
                List.of(),
                1,
                true,
                true,
                "pdf",
                BankProfile.BANCO_DO_BRASIL,
                OcrMode.ONLY
        );

        List<ExtractedRow> rows = service.parse(snapshot);

        assertEquals(1, rows.size());
        assertEquals("Recebimento fornecedor 2.229.415/0001-10", rows.get(0).complement());
    }
}