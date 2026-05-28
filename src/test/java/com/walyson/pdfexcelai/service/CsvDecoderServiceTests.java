package com.walyson.pdfexcelai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class CsvDecoderServiceTests {

    private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");

    private final CsvDecoderService service = new CsvDecoderService();

    @Test
    void decodesUtf8CsvWithAccents() {
        String csv = "DATA;DÉBITO;CRÉDITO;CÓDIGO DO HISTÓRICO\n01/10/2025;10,00;;53";

        String decoded = service.decode(csv.getBytes(StandardCharsets.UTF_8));

        assertTrue(decoded.contains("DÉBITO"));
        assertTrue(decoded.contains("CÓDIGO"));
    }

    @Test
    void decodesWindows1252CsvWithoutLosingPortugueseCharacters() {
        String csv = "DATA;DÉBITO;CRÉDITO;CÓDIGO DO HISTÓRICO\n01/10/2025;10,00;;53";

        String decoded = service.decode(csv.getBytes(WINDOWS_1252));

        assertTrue(decoded.contains("DÉBITO"));
        assertTrue(decoded.contains("CRÉDITO"));
    }

    @Test
    void returnsEmptyStringForNullInput() {
        assertEquals("", service.decode(null));
    }

    @Test
    void returnsEmptyStringForEmptyInput() {
        assertEquals("", service.decode(new byte[0]));
    }

    @Test
    void stripsUtf8BomBeforeDecoding() {
        String csv = "DATA;VALOR\n01/10/2025;100,00";
        byte[] rawUtf8 = csv.getBytes(StandardCharsets.UTF_8);
        // Prepend UTF-8 BOM: EF BB BF
        byte[] withBom = new byte[rawUtf8.length + 3];
        withBom[0] = (byte) 0xEF;
        withBom[1] = (byte) 0xBB;
        withBom[2] = (byte) 0xBF;
        System.arraycopy(rawUtf8, 0, withBom, 3, rawUtf8.length);

        String decoded = service.decode(withBom);

        assertTrue(decoded.startsWith("DATA"));
        assertTrue(decoded.contains("100,00"));
    }

    @Test
    void decodesPlainAsciiCsv() {
        String csv = "DATE;VALUE\n01/10/2025;500.00";
        String decoded = service.decode(csv.getBytes(StandardCharsets.US_ASCII));
        assertTrue(decoded.contains("500.00"));
    }
}
