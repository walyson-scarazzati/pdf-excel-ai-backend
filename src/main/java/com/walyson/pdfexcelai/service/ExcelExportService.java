package com.walyson.pdfexcelai.service;

import com.walyson.pdfexcelai.model.ExtractedRow;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

@Service
public class ExcelExportService {

    public byte[] export(List<ExtractedRow> rows) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Dados Extraidos");

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Referencia");
            header.createCell(1).setCellValue("Descricao");
            header.createCell(2).setCellValue("Valor");
            header.createCell(3).setCellValue("Data");
            header.createCell(4).setCellValue("Notas");

            for (int index = 0; index < rows.size(); index++) {
                ExtractedRow currentRow = rows.get(index);
                Row row = sheet.createRow(index + 1);
                row.createCell(0).setCellValue(currentRow.reference());
                row.createCell(1).setCellValue(currentRow.description());
                row.createCell(2).setCellValue(currentRow.amount());
                row.createCell(3).setCellValue(currentRow.date());
                row.createCell(4).setCellValue(currentRow.notes());
            }

            for (int column = 0; column < 5; column++) {
                sheet.autoSizeColumn(column);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }
}