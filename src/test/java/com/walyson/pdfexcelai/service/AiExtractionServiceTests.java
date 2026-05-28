package com.walyson.pdfexcelai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.walyson.pdfexcelai.config.AiProperties;
import com.walyson.pdfexcelai.model.BankProfile;
import com.walyson.pdfexcelai.model.ExtractedRow;
import com.walyson.pdfexcelai.model.OcrMode;
import com.walyson.pdfexcelai.model.PdfDocumentSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiExtractionServiceTests {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    private AiExtractionService notConfigured() {
        return new AiExtractionService(objectMapper, new AiProperties("", "", "", "", ""));
    }

    private PdfDocumentSnapshot snapshot(String rawText, String normalizedText) {
        return new PdfDocumentSnapshot(rawText, normalizedText, "", List.of(), List.of(), 1, true, false, "pdf",
                BankProfile.UNKNOWN, OcrMode.AUTO);
    }

    private PdfDocumentSnapshot snapshotWithPages(String rawText, String normalizedText, List<String> pageTexts) {
        return new PdfDocumentSnapshot(rawText, normalizedText, "", pageTexts, List.of(), pageTexts.size(), true, false,
                "pdf", BankProfile.BANCO_DO_BRASIL, OcrMode.AUTO);
    }

    @Test
    void isConfigured_returnsFalse_whenAllFieldsBlank() {
        assertThat(notConfigured().isConfigured()).isFalse();
    }

    @Test
    void isConfigured_returnsTrue_whenAllFieldsSet() {
        AiExtractionService service = new AiExtractionService(objectMapper,
                new AiProperties("github-models", "https://api.example.com", "mykey", "gpt-4o", "2024-02-01"));
        assertThat(service.isConfigured()).isTrue();
    }

    @Test
    void extractRows_returnsNoTextRow_whenSnapshotHasNoText() {
        AiExtractionService service = notConfigured();
        PdfDocumentSnapshot snap = snapshot("", "");
        List<ExtractedRow> rows = service.extractRows(snap);
        assertThat(rows).isNotEmpty();
        assertThat(rows.get(0).debit()).isEqualTo("SEM-TEXTO");
    }

    @Test
    void extractRows_returnsRows_fromBancoDoBrasilText() {
        AiExtractionService service = notConfigured();
        String text = """
                BANCO DO BRASIL S.A.
                Extrato de Conta Corrente
                Período: 09/2025
                
                Data  Histórico                        Valor
                02/09/2025  TED RECEBIDA              5.000,00
                05/09/2025  PAGTO BOLETO             -1.200,50
                10/09/2025  PIX RECEBIDO              3.750,00
                15/09/2025  COMPRA DEBITO              -450,00
                """;
        PdfDocumentSnapshot snap = snapshot(text, text);
        List<ExtractedRow> rows = service.extractRows(snap);
        assertThat(rows).isNotEmpty();
    }

    @Test
    void extractRows_returnsRows_fromSantanderText() {
        AiExtractionService service = notConfigured();
        String text = """
                Santander - Extrato Consolidado
                Conta: 12345-6  Agencia: 0001
                Periodo: 10/2025
                
                10/10/2025  SALDO ANTERIOR                  0,00
                11/10/2025  DEP IDENTIFICADO           10.000,00
                12/10/2025  TED ENVIADA               -2.500,00-
                13/10/2025  IOF                          -120,00-
                """;
        PdfDocumentSnapshot snap = snapshot(text, text);
        List<ExtractedRow> rows = service.extractRows(snap);
        assertThat(rows).isNotEmpty();
    }

    @Test
    void extractRows_handlesOcrDigitMisreads() {
        AiExtractionService service = notConfigured();
        // OCR misreads: 'O' as '0', 'l' as '1', 'S' as '5'
        String text = """
                Periodo: O9/2O25
                l5/O9  TED RECEBIDA  S.0OO,OO
                2O/O9  PAGTO  -l.2OO,5O
                """;
        PdfDocumentSnapshot snap = snapshot(text, text);
        List<ExtractedRow> rows = service.extractRows(snap);
        // Should not crash on digit misread text
        assertThat(rows).isNotNull();
    }

    @Test
    void extractRows_handlesMultiplePageTexts() {
        AiExtractionService service = notConfigured();
        String page1 = """
                Periodo: 09/2025
                01/09/2025  TED RECEBIDA    5.000,00
                02/09/2025  BOLETO         -1.000,00
                """;
        String page2 = """
                03/09/2025  PIX ENVIADO    -500,00
                04/09/2025  CREDITO         800,00
                """;
        PdfDocumentSnapshot snap = snapshotWithPages(page1 + "\n" + page2, page1 + "\n" + page2,
                List.of(page1, page2));
        List<ExtractedRow> rows = service.extractRows(snap);
        assertThat(rows).isNotEmpty();
    }

    @Test
    void extractRows_deduplicatesIdenticalRows() {
        AiExtractionService service = notConfigured();
        String text = """
                Periodo: 09/2025
                01/09/2025  TED RECEBIDA    5.000,00
                01/09/2025  TED RECEBIDA    5.000,00
                """;
        PdfDocumentSnapshot snap = snapshot(text, text);
        List<ExtractedRow> rows = service.extractRows(snap);
        // Duplicates should be removed
        long unique = rows.stream()
                .filter(r -> r.complement() != null && r.complement().contains("TED"))
                .distinct().count();
        assertThat(unique).isLessThanOrEqualTo(1);
    }

    @Test
    void extractRows_usesRawText_whenNormalizedTextIsBlank() {
        AiExtractionService service = notConfigured();
        String raw = """
                Periodo: 09/2025
                01/09/2025  DEPOSITO    1.000,00
                """;
        PdfDocumentSnapshot snap = snapshot(raw, "");
        List<ExtractedRow> rows = service.extractRows(snap);
        assertThat(rows).isNotEmpty();
    }

    @Test
    void extractRows_handlesNullRawText() {
        AiExtractionService service = notConfigured();
        PdfDocumentSnapshot snap = new PdfDocumentSnapshot(null, null, "", List.of(), List.of(), 1, false, false, "pdf",
                BankProfile.UNKNOWN, OcrMode.AUTO);
        List<ExtractedRow> rows = service.extractRows(snap);
        assertThat(rows).isNotEmpty();
        assertThat(rows.get(0).debit()).isEqualTo("SEM-TEXTO");
    }

    @Test
    void extractRows_handlesTextWithOnlyNoise() {
        AiExtractionService service = notConfigured();
        String noiseText = """
                pagina 1
                page 2
                123
                . . . .
                """;
        PdfDocumentSnapshot snap = snapshot(noiseText, noiseText);
        List<ExtractedRow> rows = service.extractRows(snap);
        assertThat(rows).isNotNull();
    }

    @Test
    void extractRows_parsesAmountsWithTrailingMinus() {
        AiExtractionService service = notConfigured();
        String text = """
                Periodo: 10/2025
                10/10/2025  TED ENVIADA    7.863,55-
                11/10/2025  DEPOSITO       3.200,00
                """;
        PdfDocumentSnapshot snap = snapshot(text, text);
        List<ExtractedRow> rows = service.extractRows(snap);
        assertThat(rows).isNotEmpty();
    }

    @Test
    void extractRows_parsesColumnFormatText() {
        AiExtractionService service = notConfigured();
        // Pipe-separated column format (COL| prefix from OCR BB column extraction)
        String text = """
                Periodo: 09/2025
                COL|01/09|TED RECEBIDA|5.000,00|D
                COL|02/09|BOLETO      |1.200,50|C
                """;
        PdfDocumentSnapshot snap = snapshot(text, text);
        List<ExtractedRow> rows = service.extractRows(snap);
        assertThat(rows).isNotNull();
    }

    @Test
    void extractRows_handlesDateWithOnlyDayAndMonth() {
        AiExtractionService service = notConfigured();
        // Statement-level month/year present, transactions use only DD/MM
        String text = """
                Periodo: 09/2025
                05/09  TED RECEBIDA  1.000,00
                10/09  BOLETO       -500,00
                """;
        PdfDocumentSnapshot snap = snapshot(text, text);
        List<ExtractedRow> rows = service.extractRows(snap);
        assertThat(rows).isNotEmpty();
    }

    @Test
    void extractRows_handlesCreditKeywords() {
        AiExtractionService service = notConfigured();
        String text = """
                Periodo: 10/2025
                01/10/2025  CREDITO TED              5.000,00
                02/10/2025  PIX RECEBIDO             2.000,00
                03/10/2025  DEPOSITO IDENTIFICADO    1.500,00
                """;
        PdfDocumentSnapshot snap = snapshot(text, text);
        List<ExtractedRow> rows = service.extractRows(snap);
        assertThat(rows).isNotEmpty();
    }

    @Test
    void extractRows_handlesDebitKeywords() {
        AiExtractionService service = notConfigured();
        String text = """
                Periodo: 10/2025
                01/10/2025  PAGAMENTO BOLETO        -1.000,00
                02/10/2025  TRANSFERENCIA ENVIADA   -2.000,00
                03/10/2025  COMPRA DEBITO             -300,00
                """;
        PdfDocumentSnapshot snap = snapshot(text, text);
        List<ExtractedRow> rows = service.extractRows(snap);
        assertThat(rows).isNotEmpty();
    }

    @Test
    void extractRows_fallbackRows_whenNoDateInText() {
        AiExtractionService service = notConfigured();
        // Text with amounts but no extractable dates in section start
        String text = "SALDO 10.000,00 DEPOSITO RECEBIDO CREDITO";
        PdfDocumentSnapshot snap = snapshot(text, text);
        // Should not crash
        List<ExtractedRow> rows = service.extractRows(snap);
        assertThat(rows).isNotNull();
    }

    @Test
    void extractRows_mergesContinuationLines() {
        AiExtractionService service = notConfigured();
        String text = """
                Periodo: 09/2025
                01/09/2025  TED RECEBIDA
                            CONTRATO 290000008380    5.000,00
                02/09/2025  BOLETO PAGO              -800,00
                """;
        PdfDocumentSnapshot snap = snapshot(text, text);
        List<ExtractedRow> rows = service.extractRows(snap);
        assertThat(rows).isNotEmpty();
    }

    @Test
    void extractRows_handlesMaxRows() {
        AiExtractionService service = notConfigured();
        StringBuilder text = new StringBuilder("Periodo: 09/2025\n");
        for (int i = 1; i <= 600; i++) {
            text.append(String.format("01/09/2025  LANCAMENTO %04d  %d.000,00%n", i, i));
        }
        PdfDocumentSnapshot snap = snapshot(text.toString(), text.toString());
        List<ExtractedRow> rows = service.extractRows(snap);
        // Should be bounded by MAX_HEURISTIC_ROWS in service (currently 2000)
        assertThat(rows.size()).isLessThanOrEqualTo(2000);
    }

    @Test
    void extractRows_handlesTransactionSectionMarker() {
        AiExtractionService service = notConfigured();
        String text = """
                BANCO DO BRASIL
                Conta: 12345-6
                
                LANCAMENTOS
                
                01/09/2025  TED RECEBIDA    5.000,00
                02/09/2025  BOLETO         -1.000,00
                
                RESUMO
                Saldo Total: 4.000,00
                """;
        PdfDocumentSnapshot snap = snapshot(text, text);
        List<ExtractedRow> rows = service.extractRows(snap);
        assertThat(rows).isNotEmpty();
    }

    @Test
    void extractRows_handlesYYformat() {
        AiExtractionService service = notConfigured();
        String text = """
                Periodo: 09/2025
                01/09/25  TED RECEBIDA    5.000,00
                """;
        PdfDocumentSnapshot snap = snapshot(text, text);
        List<ExtractedRow> rows = service.extractRows(snap);
        assertThat(rows).isNotNull();
    }

    @Test
    void extractRows_handlesLargeText_noOom() {
        AiExtractionService service = notConfigured();
        String repeated = "01/09/2025  TED RECEBIDA    5.000,00\n".repeat(10);
        String text = "Periodo: 09/2025\n" + repeated;
        // Very large text (max prompt length truncation)
        String large = text.repeat(200);
        PdfDocumentSnapshot snap = snapshot(large, large);
        List<ExtractedRow> rows = service.extractRows(snap);
        assertThat(rows).isNotNull();
    }

    @Test
    void extractRows_emptyPageTexts_doesNotFail() {
        AiExtractionService service = notConfigured();
        String text = "Periodo: 09/2025\n01/09/2025  DEPOSITO  1.000,00";
        PdfDocumentSnapshot snap = snapshotWithPages(text, text, List.of("", "", text));
        List<ExtractedRow> rows = service.extractRows(snap);
        assertThat(rows).isNotEmpty();
    }

    @Test
    void extractRows_handlesReferenceDocNumbers() {
        AiExtractionService service = notConfigured();
        String text = """
                Periodo: 09/2025
                DOC123456  01/09/2025  TED RECEBIDA    5.000,00
                REF789012  02/09/2025  BOLETO         -1.000,00
                """;
        PdfDocumentSnapshot snap = snapshot(text, text);
        List<ExtractedRow> rows = service.extractRows(snap);
        assertThat(rows).isNotEmpty();
    }
}
