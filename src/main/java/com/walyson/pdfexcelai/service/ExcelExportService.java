package com.walyson.pdfexcelai.service;

import com.walyson.pdfexcelai.model.ExtractedRow;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

@Service
public class ExcelExportService {

    private static final String[] HEADERS = {
            "DATA", "VALOR", "DÉBITO", "CRÉDITO", "CÓDIGO DO HISTÓRICO", "COMPLEMENTO"
    };
    private static final int[] COLUMN_WIDTHS = {4500, 5000, 8000, 8000, 7000, 15000};

    public byte[] export(List<ExtractedRow> rows) throws IOException {
        return export(rows, "", "");
    }

    public byte[] export(List<ExtractedRow> rows, String accountInfo, String period) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle bodyStyle = createBodyStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);
            CellStyle centeredStyle = createCenteredStyle(workbook);
            CellStyle totalLabelStyle = createTotalLabelStyle(workbook);
            CellStyle totalCurrencyStyle = createTotalCurrencyStyle(workbook);

            createExtractSheet(workbook, rows, accountInfo, period, headerStyle, bodyStyle, 
                    currencyStyle, dateStyle, centeredStyle, totalLabelStyle, totalCurrencyStyle);

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private void createExtractSheet(XSSFWorkbook workbook, List<ExtractedRow> rows,
            String accountInfo, String period, CellStyle headerStyle, CellStyle bodyStyle, 
            CellStyle currencyStyle, CellStyle dateStyle, CellStyle centeredStyle,
            CellStyle totalLabelStyle, CellStyle totalCurrencyStyle) {
        Sheet sheet = workbook.createSheet("Extrato Bancário");

        int currentRow = 0;

        // Adicionar informações da conta e período se disponíveis
        if (accountInfo != null && !accountInfo.isEmpty()) {
            Row infoRow = sheet.createRow(currentRow++);
            Cell infoCell = infoRow.createCell(0);
            infoCell.setCellValue(accountInfo);
            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            CellStyle infoStyle = workbook.createCellStyle();
            infoStyle.setFont(boldFont);
            infoCell.setCellStyle(infoStyle);
        }

        if (period != null && !period.isEmpty()) {
            Row periodRow = sheet.createRow(currentRow++);
            Cell periodCell = periodRow.createCell(0);
            periodCell.setCellValue("Período: " + period);
            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            CellStyle periodStyle = workbook.createCellStyle();
            periodStyle.setFont(boldFont);
            periodCell.setCellStyle(periodStyle);
        }

        if (currentRow > 0) {
            currentRow++; // Linha em branco
        }

        // Cabeçalho
        Row header = sheet.createRow(currentRow++);
        for (int column = 0; column < HEADERS.length; column++) {
            Cell cell = header.createCell(column);
            cell.setCellValue(HEADERS[column]);
            cell.setCellStyle(headerStyle);
        }

        // Dados
        for (int index = 0; index < rows.size(); index++) {
            ExtractedRow currentRow_ = rows.get(index);
            Row row = sheet.createRow(currentRow + index);
            
            // Data
            Cell dateCell = row.createCell(0);
            dateCell.setCellValue(currentRow_.date());
            dateCell.setCellStyle(dateStyle);
            
            // Valor
            Cell valueCell = row.createCell(1);
            valueCell.setCellValue(currentRow_.value());
            valueCell.setCellStyle(bodyStyle);
            
            // Débito
            Cell debitCell = row.createCell(2);
            debitCell.setCellValue(currentRow_.debit());
            debitCell.setCellStyle(bodyStyle);
            
            // Crédito
            Cell creditCell = row.createCell(3);
            creditCell.setCellValue(currentRow_.credit());
            creditCell.setCellStyle(bodyStyle);
            
            // Código do Histórico
            Cell historyCodeCell = row.createCell(4);
            historyCodeCell.setCellValue(currentRow_.historyCode());
            historyCodeCell.setCellStyle(centeredStyle);
            
            // Complemento
            Cell complementCell = row.createCell(5);
            complementCell.setCellValue(currentRow_.complement());
            complementCell.setCellStyle(bodyStyle);
            
            row.setHeightInPoints(22);
        }

        // Adicionar linha de totais se houver dados
        if (!rows.isEmpty()) {
            int totalRowIndex = currentRow + rows.size();
            Row totalRow = sheet.createRow(totalRowIndex);
            totalRow.setHeightInPoints(26);
            
            Cell labelCell = totalRow.createCell(0);
            labelCell.setCellValue("TOTAIS");
            labelCell.setCellStyle(totalLabelStyle);
            
            // Somar a coluna VALOR. Débito e crédito são contas contábeis.
            BigDecimal totalValue = BigDecimal.ZERO;
            
            for (ExtractedRow row : rows) {
                BigDecimal value = parseAmount(row.value());
                if (value != null) {
                    totalValue = totalValue.add(value);
                }
            }
            
            Cell totalValueCell = totalRow.createCell(1);
            totalValueCell.setCellValue(formatCurrency(totalValue));
            totalValueCell.setCellStyle(totalLabelStyle);
        }

        // Congelar cabeçalho
        sheet.createFreezePane(0, currentRow);
        
        // Auto-filtro
        sheet.setAutoFilter(new CellRangeAddress(currentRow - 1, 
                Math.max(currentRow + rows.size() - 1, currentRow), 0, HEADERS.length - 1));
        
        // Definir larguras das colunas
        for (int column = 0; column < COLUMN_WIDTHS.length; column++) {
            sheet.setColumnWidth(column, COLUMN_WIDTHS[column]);
        }
    }

    private CellStyle createHeaderStyle(XSSFWorkbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        font.setFontHeightInPoints((short) 11);
        
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createBodyStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setWrapText(false);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setBorderLeft(BorderStyle.THIN);
        style.setLeftBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setBorderRight(BorderStyle.THIN);
        style.setRightBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        return style;
    }

    private CellStyle createDateStyle(XSSFWorkbook workbook) {
        CellStyle style = createBodyStyle(workbook);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createCenteredStyle(XSSFWorkbook workbook) {
        CellStyle style = createBodyStyle(workbook);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createCurrencyStyle(XSSFWorkbook workbook) {
        CellStyle style = createBodyStyle(workbook);
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("R$ #,##0.00"));
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }

    private CellStyle createTotalLabelStyle(XSSFWorkbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderTop(BorderStyle.MEDIUM);
        style.setBorderBottom(BorderStyle.MEDIUM);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createTotalCurrencyStyle(XSSFWorkbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("R$ #,##0.00"));
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderTop(BorderStyle.MEDIUM);
        return style;
    }

    private BigDecimal parseAmount(String amount) {
        if (amount == null || amount.isBlank()) {
            return null;
        }
        
        // Remover "R$" e outros caracteres não numéricos, exceto vírgula e ponto
        String normalized = amount.replaceAll("[^\\d,.-]", "");
        if (normalized.isBlank()) {
            return null;
        }
        
        // Detectar formato brasileiro (vírgula como decimal)
        int lastComma = normalized.lastIndexOf(',');
        int lastDot = normalized.lastIndexOf('.');
        
        if (lastComma > lastDot) {
            // Formato brasileiro: 1.234,56
            normalized = normalized.replace(".", "").replace(',', '.');
        } else if (lastDot > lastComma && normalized.chars().filter(ch -> ch == '.').count() > 1) {
            // Múltiplos pontos = separador de milhares
            normalized = normalized.replace(".", "");
        } else {
            // Remover vírgulas (separador de milhares no formato US)
            normalized = normalized.replace(",", "");
        }
        
        try {
            return new BigDecimal(normalized).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "";
        }
        return String.format("R$ %,.2f", amount).replace(',', '_').replace('.', ',').replace('_', '.');
    }
}
