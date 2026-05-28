package com.walyson.pdfexcelai.service;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountingPlanImportServiceTests {

    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private ResourceLoader resourceLoader;
    @Mock private Resource resource;

    private AccountingPlanImportService service;

    @BeforeEach
    void setUp() {
        service = new AccountingPlanImportService(jdbcTemplate, resourceLoader);
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "resourceLocation", "classpath:plano.xlsx");
    }

    @Test
    void importPlanIfAvailable_disabled_returnsEarly() {
        ReflectionTestUtils.setField(service, "enabled", false);

        service.importPlanIfAvailable();

        verifyNoInteractions(resourceLoader, jdbcTemplate);
    }

    @Test
    void importPlanIfAvailable_resourceMissing_returnsEarly() {
        when(resourceLoader.getResource("classpath:plano.xlsx")).thenReturn(resource);
        when(resource.exists()).thenReturn(false);

        service.importPlanIfAvailable();

        verify(resourceLoader).getResource("classpath:plano.xlsx");
        verify(resource).exists();
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void importPlanIfAvailable_headerNotFound_doesNotImport() throws Exception {
        byte[] workbook = workbookWithoutHeader();
        when(resourceLoader.getResource("classpath:plano.xlsx")).thenReturn(resource);
        when(resource.exists()).thenReturn(true);
        when(resource.getInputStream()).thenReturn(new ByteArrayInputStream(workbook));

        service.importPlanIfAvailable();

        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void importPlanIfAvailable_importsRowsAndAppliesDefaults() throws Exception {
        byte[] workbook = workbookWithValidRows();
        when(resourceLoader.getResource("classpath:plano.xlsx")).thenReturn(resource);
        when(resource.exists()).thenReturn(true);
        when(resource.getInputStream()).thenReturn(new ByteArrayInputStream(workbook));
        when(resource.getFilename()).thenReturn("plano de contas.xlsx");

        service.importPlanIfAvailable();

        verify(jdbcTemplate, times(2)).update(anyString(), any(), any(), any(), any(), any());
        verify(jdbcTemplate).update(anyString(), eq("111"), eq("111"), eq("Caixa"), eq("NAO INFORMADO"), eq("plano de contas.xlsx"));
        verify(jdbcTemplate).update(anyString(), eq("222"), eq("222333"), eq("Receitas"), eq("RECEITAS"), eq("plano de contas.xlsx"));
    }

    @Test
    void importPlanIfAvailable_fallsBackToResourceLocationWhenFilenameMissing() throws Exception {
        byte[] workbook = workbookWithSingleRow();
        ReflectionTestUtils.setField(service, "resourceLocation", "classpath:arquivo-sem-nome.xlsx");

        when(resourceLoader.getResource("classpath:arquivo-sem-nome.xlsx")).thenReturn(resource);
        when(resource.exists()).thenReturn(true);
        when(resource.getInputStream()).thenReturn(new ByteArrayInputStream(workbook));
        when(resource.getFilename()).thenReturn(null);

        service.importPlanIfAvailable();

        verify(jdbcTemplate).update(anyString(), eq("999"), eq("999"), eq("Conta teste"), eq("Conta teste"), eq("classpath:arquivo-sem-nome.xlsx"));
    }

    private byte[] workbookWithoutHeader() throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Plano");
            Row row = sheet.createRow(0);
            row.createCell(0).setCellValue("valor qualquer");
            row.createCell(1).setCellValue("sem cabecalho");
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private byte[] workbookWithValidRows() throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Plano");

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Codigo");
            header.createCell(1).setCellValue("Descricao");
            header.createCell(2).setCellValue("Codigo Completo");
            header.createCell(3).setCellValue("Grupo");

            Row validWithDefaults = sheet.createRow(1);
            validWithDefaults.createCell(0).setCellValue("111");
            validWithDefaults.createCell(1).setCellValue("Caixa");

            Row validFull = sheet.createRow(2);
            validFull.createCell(0).setCellValue("222");
            validFull.createCell(1).setCellValue("Receitas");
            validFull.createCell(2).setCellValue("2.22.333");
            validFull.createCell(3).setCellValue("RECEITAS");

            Row invalidNoCode = sheet.createRow(3);
            invalidNoCode.createCell(1).setCellValue("Sem codigo");

            Row invalidNoDescription = sheet.createRow(4);
            invalidNoDescription.createCell(0).setCellValue("333");

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private byte[] workbookWithSingleRow() throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Plano");

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Codigo");
            header.createCell(1).setCellValue("Descricao");

            Row valid = sheet.createRow(1);
            valid.createCell(0).setCellValue("999");
            valid.createCell(1).setCellValue("Conta teste");

            workbook.write(out);
            return out.toByteArray();
        }
    }
}
