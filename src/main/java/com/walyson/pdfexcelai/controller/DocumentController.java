package com.walyson.pdfexcelai.controller;

import com.walyson.pdfexcelai.model.ExtractionResult;
import com.walyson.pdfexcelai.service.DocumentProcessingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documents")
@Tag(name = "Documents", description = "Preview e exportacao de arquivos bancarios")
public class DocumentController {

    private final DocumentProcessingService documentProcessingService;

    public DocumentController(DocumentProcessingService documentProcessingService) {
        this.documentProcessingService = documentProcessingService;
    }

    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Preview extracted rows",
            description = "Recebe um PDF ou CSV e retorna os dados extraidos antes de gerar o Excel.")
    @ApiResponse(responseCode = "200", description = "Preview gerado com sucesso")
    public ExtractionResult preview(
            @Parameter(description = "Arquivo PDF ou CSV", required = true) @RequestParam("file") MultipartFile file)
            throws IOException {
        return documentProcessingService.preview(file);
    }

    @PostMapping(value = "/export", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Export extracted data to Excel",
            description = "Recebe um PDF ou CSV e retorna uma planilha XLSX com os dados extraidos.")
    @ApiResponse(
            responseCode = "200",
            description = "Planilha Excel gerada com sucesso",
            content = @Content(mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
    public ResponseEntity<byte[]> export(
            @Parameter(description = "Arquivo PDF ou CSV", required = true) @RequestParam("file") MultipartFile file)
            throws IOException {
        byte[] workbook = documentProcessingService.export(file);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(ContentDisposition.attachment().filename("dados-extraidos.xlsx").build());
        return ResponseEntity.ok().headers(headers).body(workbook);
    }
}
