package com.walyson.pdfexcelai.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.walyson.pdfexcelai.model.ExtractedRow;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExcelExportServiceTests {

    private final ExcelExportService service = new ExcelExportService();

    @Test
    void exportsEmptyRowListToValidWorkbook() throws IOException {
        byte[] result = service.export(List.of());
        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    void exportsSingleRowToWorkbook() throws IOException {
        ExtractedRow row = new ExtractedRow(
                "01/09/2025", "R$ 1.000,00", "3220", "7560", "54", "Pagamento fornecedor");

        byte[] result = service.export(List.of(row));

        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    void exportsWithAccountInfoAndPeriod() throws IOException {
        ExtractedRow row = new ExtractedRow(
                "01/09/2025", "R$ 500,00", "1880", "7560", "53", "Tarifa bancaria");

        byte[] result = service.export(List.of(row), "Conta 12345-6", "09/2025");

        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    void exportsMultipleRowsIncludingCreditAndDebit() throws IOException {
        List<ExtractedRow> rows = List.of(
                new ExtractedRow("01/09/2025", "R$ 957,00", "7560", "3239", "55", "Pix recebido"),
                new ExtractedRow("02/09/2025", "R$ 1.127,60", "3220", "7560", "54", "Pagamento boleto"),
                new ExtractedRow("03/09/2025", "R$ 10,00", "1880", "7560", "53", "Tarifa bancaria")
        );

        byte[] result = service.export(rows, "Conta 99999-1", "09/2025");

        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    void exportsWithOnlyAccountInfo() throws IOException {
        byte[] result = service.export(List.of(), "Conta 12345-6", "");
        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    void exportsWithOnlyPeriod() throws IOException {
        byte[] result = service.export(List.of(), "", "09/2025");
        assertNotNull(result);
        assertTrue(result.length > 0);
    }
}
