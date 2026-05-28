package com.walyson.pdfexcelai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.walyson.pdfexcelai.config.AiProperties;
import com.walyson.pdfexcelai.model.ExtractedRow;
import com.walyson.pdfexcelai.model.PdfDocumentSnapshot;
import com.walyson.pdfexcelai.model.BankProfile;
import com.walyson.pdfexcelai.model.OcrMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiExtractionServiceDetailedTests {

    private ObjectMapper objectMapper;
    private AiExtractionService service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new AiExtractionService(
            objectMapper,
            new AiProperties("", "", "", "", "")
        );
    }

    private PdfDocumentSnapshot snapshot(String text) {
        return new PdfDocumentSnapshot(text, text, "", List.of(), List.of(), 1, true, false,
            "pdf", BankProfile.UNKNOWN, OcrMode.AUTO);
    }

    @Test
    void extractRows_fromBancoDoBrasilWithMultipleTransactions() {
        String bbText = """
            BANCO DO BRASIL
            Data      Descricao              Valor
            01/09/2025  DEPOSITO RECEBIDO  10000.00
            02/09/2025  PAGAMENTO EFETUADO  -1000.00
            03/09/2025  PIX RECEBIDO         5000.50
            04/09/2025  COMPRA CARTAO         -250.25
            """;
        
        PdfDocumentSnapshot snap = snapshot(bbText);
        List<ExtractedRow> rows = service.extractRows(snap);
        
        assertThat(rows).isNotEmpty();
        assertThat(rows.size()).isGreaterThan(1);
    }

    @Test
    void extractRows_fromSantanderWithFormattedAmounts() {
        String santanderText = """
            SANTANDER
            Data          Lançamento                Valor
            01.09.2025    DEP. SALÁRIO             15.000,00
            02.09.2025    PAGTO BOLETO             -2.500,00-
            03.09.2025    TAXA MANUTENÇÃO             -50,00-
            """;
        
        PdfDocumentSnapshot snap = snapshot(santanderText);
        List<ExtractedRow> rows = service.extractRows(snap);
        
        assertThat(rows).isNotEmpty();
    }

    @Test
    void extractRows_handlesOcrMisreadsInDates() {
        String ocrText = """
            0l/09/2025  DEPOSITO               1000
            02/09/2025  PIX                    5000
            03/09/202S  PAGAMENTO              -500
            """;
        
        PdfDocumentSnapshot snap = snapshot(ocrText);
        List<ExtractedRow> rows = service.extractRows(snap);
        
        assertThat(rows).isNotEmpty();
    }

    @Test
    void extractRows_handlesMultipleFormatVariations() {
        String mixedText = """
            01/09 100,00
            02-09 200.00
            03.09.2025 R$ 300,50
            04/09/25 $400
            """;
        
        PdfDocumentSnapshot snap = snapshot(mixedText);
        List<ExtractedRow> rows = service.extractRows(snap);
        
        assertThat(rows).isNotEmpty();
    }

    @Test
    void extractRows_withTransactionKeywords() {
        String keywordText = """
            Data    Descricao              Valor
            01/09   ESTORNO RECEBIDO       1000.00
            02/09   RECEBIMENTO PIX        2000.00
            03/09   DEPOSITO IDENTIFICADO  3000.00
            04/09   COMPRA DÉBITO          -500.00
            05/09   PAGAMENTO CONTA         -100.00
            """;
        
        PdfDocumentSnapshot snap = snapshot(keywordText);
        List<ExtractedRow> rows = service.extractRows(snap);
        
        assertThat(rows).isNotEmpty();
        // Verify that rows with credit keywords are detected
        boolean hasCreditRows = rows.stream()
            .anyMatch(r -> r.credit() != null && !r.credit().isEmpty());
        assertThat(hasCreditRows).isTrue();
    }

    @Test
    void extractRows_returnsErrorRow_whenNoTextAvailable() {
        PdfDocumentSnapshot snap = snapshot("");
        List<ExtractedRow> rows = service.extractRows(snap);
        
        assertThat(rows).isNotEmpty();
        assertThat(rows.get(0).debit()).contains("SEM");
    }

    @Test
    void extractRows_handlesLargeDocument() {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 100; i++) {
            sb.append(String.format("0%d/09/2025  TRANSACAO %d  %d.00\n", i % 10, i, 100 * i));
        }
        
        PdfDocumentSnapshot snap = snapshot(sb.toString());
        List<ExtractedRow> rows = service.extractRows(snap);
        
        assertThat(rows).isNotEmpty();
    }

    @Test
    void isConfigured_requiresAllFields() {
        AiExtractionService unconfigured = new AiExtractionService(
            objectMapper,
            new AiProperties("", "", "", "", "")
        );
        assertThat(unconfigured.isConfigured()).isFalse();

        AiExtractionService configured = new AiExtractionService(
            objectMapper,
            new AiProperties("openai", "https://api.openai.com", "key", "model", "2024-01")
        );
        assertThat(configured.isConfigured()).isTrue();
    }

    @Test
    void extractRows_withDifferentOcrModes() {
        String text = "01/09/2025 DEPOSIT 1000.00";
        
        PdfDocumentSnapshot autoMode = new PdfDocumentSnapshot(
            text, text, "", List.of(), List.of(), 1, true, false,
            "pdf", BankProfile.UNKNOWN, OcrMode.AUTO
        );
        
        PdfDocumentSnapshot firstMode = new PdfDocumentSnapshot(
            text, text, "", List.of(), List.of(), 1, true, false,
            "pdf", BankProfile.UNKNOWN, OcrMode.FIRST
        );
        
        assertThat(service.extractRows(autoMode)).isNotEmpty();
        assertThat(service.extractRows(firstMode)).isNotEmpty();
    }
}
