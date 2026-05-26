package com.walyson.pdfexcelai.service;

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
}