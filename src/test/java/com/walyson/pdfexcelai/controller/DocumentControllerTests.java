package com.walyson.pdfexcelai.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.walyson.pdfexcelai.model.ExtractedRow;
import com.walyson.pdfexcelai.model.ExtractionResult;
import com.walyson.pdfexcelai.service.DocumentProcessingService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class DocumentControllerTests {

    private DocumentProcessingService documentProcessingService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        documentProcessingService = mock(DocumentProcessingService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new DocumentController(documentProcessingService)).build();
    }

    @Test
    void previewReturnExtractionResult() throws Exception {
        ExtractionResult result = new ExtractionResult(
                "extrato.pdf", "Conta 12345-6", "09/2025", 1, 2,
                List.of(
                        new ExtractedRow("01/09/2025", "R$ 100,00", "3220", "7560", "54", "Test"),
                        new ExtractedRow("02/09/2025", "R$ 200,00", "7560", "3239", "55", "Pix")
                ),
                List.of(), false, false, "heuristic", "preview text");

        MockMultipartFile file = new MockMultipartFile("file", "extrato.pdf",
                "application/pdf", "PDF content".getBytes());

        when(documentProcessingService.preview(file)).thenReturn(result);

        mockMvc.perform(multipart("/api/documents/preview").file(file))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"));
    }

    @Test
    void exportReturnsExcelFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "extrato.pdf",
                "application/pdf", "PDF content".getBytes());

        when(documentProcessingService.export(file)).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/documents/export").file(file))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"dados-extraidos.xlsx\""));
    }
}
