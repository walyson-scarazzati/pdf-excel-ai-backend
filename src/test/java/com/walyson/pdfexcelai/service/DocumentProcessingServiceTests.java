package com.walyson.pdfexcelai.service;

import com.walyson.pdfexcelai.model.BankProfile;
import com.walyson.pdfexcelai.model.ExtractionResult;
import com.walyson.pdfexcelai.model.ExtractedRow;
import com.walyson.pdfexcelai.model.PdfDocumentSnapshot;
import com.walyson.pdfexcelai.model.OcrMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentProcessingServiceTests {

    @Mock private PdfTextExtractor pdfTextExtractor;
    @Mock private BankStatementParserService bankStatementParserService;
    @Mock private AiExtractionService aiExtractionService;
    @Mock private ExcelExportService excelExportService;
    @Mock private AccountingClassificationService accountingClassificationService;
    @Mock private CsvDecoderService csvDecoderService;

    private DocumentProcessingService service;

    private static final ExtractedRow ROW = new ExtractedRow("01/09/2025", "1.000,00", "1.000,00", "", "DOC001", "TED RECEBIDA");

    @BeforeEach
    void setUp() {
        service = new DocumentProcessingService(pdfTextExtractor, bankStatementParserService,
                aiExtractionService, excelExportService, accountingClassificationService, csvDecoderService);
    }

    private PdfDocumentSnapshot emptySnapshot() {
        return new PdfDocumentSnapshot("raw", "normalized", "preview", List.of(), List.of(), 1, true, false, "pdf",
                BankProfile.UNKNOWN, OcrMode.AUTO);
    }

    // ---- preview: CSV path ----

    @Test
    void preview_csv_returnsCsvResult() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "extrato.csv",
                "text/csv", "data".getBytes());
        when(csvDecoderService.decode(any())).thenReturn("decoded csv content");
        when(bankStatementParserService.parseFromCsv(any())).thenReturn(List.of(ROW));
        when(accountingClassificationService.classify(anyList())).thenReturn(List.of(ROW));

        ExtractionResult result = service.preview(file);

        assertThat(result.fileName()).isEqualTo("extrato.csv");
        assertThat(result.rows()).hasSize(1);
        assertThat(result.extractionMode()).isEqualTo("csv");
    }

    @Test
    void preview_csv_emptyRows_returnsEmptyResult() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "extrato.csv",
                "text/csv", "data".getBytes());
        when(csvDecoderService.decode(any())).thenReturn("col1;col2");
        when(bankStatementParserService.parseFromCsv(any())).thenReturn(List.of());
        when(accountingClassificationService.classify(anyList())).thenReturn(List.of());

        ExtractionResult result = service.preview(file);

        assertThat(result.rows()).isEmpty();
        assertThat(result.totalRows()).isZero();
    }

    // ---- preview: PDF path with rows from parser ----

    @Test
    void preview_pdf_returnsRows_fromParser() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "extrato.pdf",
                "application/pdf", new byte[]{1, 2, 3});
        PdfDocumentSnapshot snap = emptySnapshot();
        when(pdfTextExtractor.extract(any(), anyBoolean())).thenReturn(snap);
        when(bankStatementParserService.parse(snap)).thenReturn(List.of(ROW));
        when(accountingClassificationService.classify(anyList())).thenReturn(List.of(ROW));
        when(bankStatementParserService.extractAccountInfo(any())).thenReturn("CC 12345-6");
        when(bankStatementParserService.extractPeriod(any())).thenReturn("09/2025");
        when(aiExtractionService.isConfigured()).thenReturn(false);

        ExtractionResult result = service.preview(file);

        assertThat(result.fileName()).isEqualTo("extrato.pdf");
        assertThat(result.rows()).hasSize(1);
    }

    // ---- preview: PDF path — parser returns empty, falls back to AI service ----

    @Test
    void preview_pdf_fallsBackToAiExtraction_whenParserEmpty() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "extrato.pdf",
                "application/pdf", new byte[]{1, 2, 3});
        PdfDocumentSnapshot snap = emptySnapshot();
        when(pdfTextExtractor.extract(any(), anyBoolean())).thenReturn(snap);
        when(bankStatementParserService.parse(snap)).thenReturn(List.of());
        when(aiExtractionService.extractRows(snap)).thenReturn(List.of(ROW));
        when(accountingClassificationService.classify(anyList())).thenReturn(List.of(ROW));
        when(bankStatementParserService.extractAccountInfo(any())).thenReturn("");
        when(bankStatementParserService.extractPeriod(any())).thenReturn("");
        when(aiExtractionService.isConfigured()).thenReturn(true);

        ExtractionResult result = service.preview(file);

        assertThat(result.rows()).hasSize(1);
        assertThat(result.aiUsed()).isTrue();
    }

    // ---- preview: builds extractionMode string ----

    @Test
    void preview_pdf_buildExtractionMode() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "ext.pdf", "application/pdf", new byte[]{1});
        PdfDocumentSnapshot snap = new PdfDocumentSnapshot("r", "n", "p", List.of(), List.of(), 2, true, true, "pdf",
                BankProfile.SANTANDER, OcrMode.FIRST);
        when(pdfTextExtractor.extract(any(), anyBoolean())).thenReturn(snap);
        when(bankStatementParserService.parse(snap)).thenReturn(List.of(ROW));
        when(accountingClassificationService.classify(anyList())).thenReturn(List.of(ROW));
        when(bankStatementParserService.extractAccountInfo(any())).thenReturn("");
        when(bankStatementParserService.extractPeriod(any())).thenReturn("");
        when(aiExtractionService.isConfigured()).thenReturn(false);

        ExtractionResult result = service.preview(file);

        assertThat(result.extractionMode()).contains("ocr");
        assertThat(result.extractionMode()).contains("santander");
        assertThat(result.extractionMode()).contains("first");
        assertThat(result.ocrUsed()).isTrue();
    }

    // ---- export: CSV path ----

    @Test
    void export_csv_returnsExcelBytes() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "extrato.csv",
                "text/csv", "data".getBytes());
        when(csvDecoderService.decode(any())).thenReturn("decoded");
        when(bankStatementParserService.parseFromCsv(any())).thenReturn(List.of(ROW));
        when(accountingClassificationService.classify(anyList())).thenReturn(List.of(ROW));
        when(excelExportService.export(any())).thenReturn(new byte[]{1, 2, 3});

        byte[] result = service.export(file);

        assertThat(result).isNotEmpty();
    }

    // ---- export: PDF path ----

    @Test
    void export_pdf_returnsExcelBytes_fromParser() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "extrato.pdf",
                "application/pdf", new byte[]{1, 2, 3});
        PdfDocumentSnapshot snap = emptySnapshot();
        when(pdfTextExtractor.extract(any(MockMultipartFile.class))).thenReturn(snap);
        when(bankStatementParserService.parse(snap)).thenReturn(List.of(ROW));
        when(accountingClassificationService.classify(anyList())).thenReturn(List.of(ROW));
        when(bankStatementParserService.extractAccountInfo(any())).thenReturn("CC 12345-6");
        when(bankStatementParserService.extractPeriod(any())).thenReturn("09/2025");
        when(excelExportService.export(any(), any(), any())).thenReturn(new byte[]{4, 5, 6});

        byte[] result = service.export(file);

        assertThat(result).isNotEmpty();
    }

    @Test
    void export_pdf_fallsBackToAiExtraction_whenParserEmpty() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "extrato.pdf",
                "application/pdf", new byte[]{1, 2, 3});
        PdfDocumentSnapshot snap = emptySnapshot();
        when(pdfTextExtractor.extract(any(MockMultipartFile.class))).thenReturn(snap);
        when(bankStatementParserService.parse(snap)).thenReturn(List.of());
        when(aiExtractionService.extractRows(snap)).thenReturn(List.of(ROW));
        when(accountingClassificationService.classify(anyList())).thenReturn(List.of(ROW));
        when(bankStatementParserService.extractAccountInfo(any())).thenReturn("");
        when(bankStatementParserService.extractPeriod(any())).thenReturn("");
        when(excelExportService.export(any(), any(), any())).thenReturn(new byte[]{7, 8, 9});

        byte[] result = service.export(file);

        assertThat(result).isNotEmpty();
    }

    // ---- filename null or without extension ----

    @Test
    void preview_noFilename_treatsPdf() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", null, "application/pdf", new byte[]{1});
        PdfDocumentSnapshot snap = emptySnapshot();
        when(pdfTextExtractor.extract(any(), anyBoolean())).thenReturn(snap);
        when(bankStatementParserService.parse(snap)).thenReturn(List.of());
        when(aiExtractionService.extractRows(snap)).thenReturn(List.of());
        when(accountingClassificationService.classify(anyList())).thenReturn(List.of());
        when(bankStatementParserService.extractAccountInfo(any())).thenReturn("");
        when(bankStatementParserService.extractPeriod(any())).thenReturn("");
        when(aiExtractionService.isConfigured()).thenReturn(false);

        ExtractionResult result = service.preview(file);

        assertThat(result).isNotNull();
    }
}
