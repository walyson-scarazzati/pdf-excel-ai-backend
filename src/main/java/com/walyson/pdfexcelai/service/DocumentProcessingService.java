package com.walyson.pdfexcelai.service;

import com.walyson.pdfexcelai.model.ExtractedRow;
import com.walyson.pdfexcelai.model.ExtractionResult;
import com.walyson.pdfexcelai.model.PdfDocumentSnapshot;
import java.io.IOException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentProcessingService {

    private final PdfTextExtractor pdfTextExtractor;
    private final AiExtractionService aiExtractionService;
    private final ExcelExportService excelExportService;

    public DocumentProcessingService(
            PdfTextExtractor pdfTextExtractor,
            AiExtractionService aiExtractionService,
            ExcelExportService excelExportService
    ) {
        this.pdfTextExtractor = pdfTextExtractor;
        this.aiExtractionService = aiExtractionService;
        this.excelExportService = excelExportService;
    }

    public ExtractionResult preview(MultipartFile file) throws IOException {
        PdfDocumentSnapshot snapshot = pdfTextExtractor.extract(file.getInputStream());
        List<ExtractedRow> rows = aiExtractionService.extractRows(snapshot);

        return new ExtractionResult(
                file.getOriginalFilename(),
                snapshot.pageCount(),
                rows.size(),
                rows,
                aiExtractionService.isConfigured(),
                snapshot.textAvailable() ? "multimodal+texto" : "multimodal+imagem",
                snapshot.previewText()
        );
    }

    public byte[] export(MultipartFile file) throws IOException {
        PdfDocumentSnapshot snapshot = pdfTextExtractor.extract(file.getInputStream());
        List<ExtractedRow> rows = aiExtractionService.extractRows(snapshot);
        return excelExportService.export(rows);
    }
}