package com.walyson.pdfexcelai.service;

import com.walyson.pdfexcelai.model.ExtractedRow;
import com.walyson.pdfexcelai.model.ExtractionResult;
import com.walyson.pdfexcelai.model.PdfDocumentSnapshot;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentProcessingService {

    private final PdfTextExtractor pdfTextExtractor;
    private final BankStatementParserService bankStatementParserService;
    private final AiExtractionService aiExtractionService;
    private final ExcelExportService excelExportService;
    private final AccountingClassificationService accountingClassificationService;

    public DocumentProcessingService(
            PdfTextExtractor pdfTextExtractor,
            BankStatementParserService bankStatementParserService,
            AiExtractionService aiExtractionService,
            ExcelExportService excelExportService,
            AccountingClassificationService accountingClassificationService
    ) {
        this.pdfTextExtractor = pdfTextExtractor;
        this.bankStatementParserService = bankStatementParserService;
        this.aiExtractionService = aiExtractionService;
        this.excelExportService = excelExportService;
        this.accountingClassificationService = accountingClassificationService;
    }

    public ExtractionResult preview(MultipartFile file) throws IOException {
        // Verificar se é CSV
        String filename = file.getOriginalFilename();
        if (filename != null && filename.toLowerCase().endsWith(".csv")) {
            return previewFromCsv(file);
        }
        
        // Processar PDF
        System.out.println("[DEBUG] Extraindo texto do PDF: " + filename);
        PdfDocumentSnapshot snapshot = pdfTextExtractor.extract(file, false);
        System.out.println("[DEBUG] Paginas extraidas: " + snapshot.pageCount());
        System.out.println("[DEBUG] Texto disponivel: " + snapshot.textAvailable());
        System.out.println("[DEBUG] Tamanho do texto normalizado: " + (snapshot.normalizedText() != null ? snapshot.normalizedText().length() : 0));
        System.out.println("[DEBUG] Perfil bancario detectado: " + snapshot.bankProfile());
        System.out.println("[DEBUG] Modo OCR usado: " + snapshot.ocrMode());
        
        List<ExtractedRow> rows = bankStatementParserService.parse(snapshot);
        System.out.println("[DEBUG] Linhas extraidas pela heuristica: " + rows.size());
        
        // Se não encontrou dados no parser direto, usar a camada de extração estruturada.
        // Quando a IA não está configurada, esse serviço ainda aplica a heurística local.
        if (rows.isEmpty()) {
            System.out.println("[DEBUG] Tentando extrair com camada estruturada...");
            rows = aiExtractionService.extractRows(snapshot);
            System.out.println("[DEBUG] Linhas extraidas pela camada estruturada: " + rows.size());
        }
        rows = accountingClassificationService.classify(rows);
        
        String extractionMode = buildExtractionMode(snapshot);
        String metadataSource = snapshot.rawText() + "\n" + snapshot.normalizedText();
        String accountInfo = bankStatementParserService.extractAccountInfo(metadataSource);
        String period = bankStatementParserService.extractPeriod(metadataSource);

        return new ExtractionResult(
                file.getOriginalFilename(),
                accountInfo,
                period,
                snapshot.pageCount(),
                rows.size(),
                rows,
                snapshot.pageImages(),
                aiExtractionService.isConfigured(),
                snapshot.ocrUsed(),
                extractionMode,
                snapshot.previewText()
        );
    }

    private ExtractionResult previewFromCsv(MultipartFile file) throws IOException {
        String csvContent = new String(file.getBytes(), StandardCharsets.UTF_8);
        List<ExtractedRow> rows = bankStatementParserService.parseFromCsv(csvContent);
        rows = accountingClassificationService.classify(rows);
        
        return new ExtractionResult(
                file.getOriginalFilename(),
                "",
                "",
                1,
                rows.size(),
                rows,
                List.of(),
                false,
                false,
                "csv",
                csvContent.substring(0, Math.min(csvContent.length(), 500))
        );
    }

    public byte[] export(MultipartFile file) throws IOException {
        // Verificar se é CSV
        String filename = file.getOriginalFilename();
        if (filename != null && filename.toLowerCase().endsWith(".csv")) {
            String csvContent = new String(file.getBytes(), StandardCharsets.UTF_8);
            List<ExtractedRow> rows = bankStatementParserService.parseFromCsv(csvContent);
            rows = accountingClassificationService.classify(rows);
            return excelExportService.export(rows);
        }
        
        // Processar PDF
        PdfDocumentSnapshot snapshot = pdfTextExtractor.extract(file);
        List<ExtractedRow> rows = bankStatementParserService.parse(snapshot);

        // Usar a mesma camada estruturada do preview.
        // Quando a IA não está configurada, esse serviço cai na heurística local.
        if (rows.isEmpty()) {
            rows = aiExtractionService.extractRows(snapshot);
        }
        rows = accountingClassificationService.classify(rows);
        
        String metadataSource = snapshot.rawText() + "\n" + snapshot.normalizedText();
        String accountInfo = bankStatementParserService.extractAccountInfo(metadataSource);
        String period = bankStatementParserService.extractPeriod(metadataSource);
        
        return excelExportService.export(rows, accountInfo, period);
    }

    private String buildExtractionMode(PdfDocumentSnapshot snapshot) {
        StringBuilder mode = new StringBuilder(snapshot.sourceType());
        mode.append(snapshot.textAvailable() ? "+texto" : "+imagem");
        if (snapshot.ocrUsed()) {
            mode.append("+ocr");
        }
        mode.append("+").append(snapshot.bankProfile().name().toLowerCase());
        mode.append("+").append(snapshot.ocrMode().name().toLowerCase());
        return mode.toString();
    }
}
