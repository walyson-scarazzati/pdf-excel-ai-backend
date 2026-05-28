package com.walyson.pdfexcelai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.walyson.pdfexcelai.model.BankProfile;
import java.util.List;
import org.junit.jupiter.api.Test;

class BankProfileResolverTests {

    private final BankProfileResolver resolver = new BankProfileResolver();

    @Test
    void detectsBancoDoBrasilFromRawText() {
        BankProfile profile = resolver.resolve(null, "Banco do Brasil - Extrato de Conta Corrente", List.of());
        assertEquals(BankProfile.BANCO_DO_BRASIL, profile);
    }

    @Test
    void detectsBancoDoBrasilFromPixKeywords() {
        BankProfile profile = resolver.resolve(null, "01/09/2025 PIX - Recebido FULANO 100,00 C", List.of());
        assertEquals(BankProfile.BANCO_DO_BRASIL, profile);
    }

    @Test
    void detectsBancoDoBrasilFromPageTexts() {
        BankProfile profile = resolver.resolve(null, null, List.of("BB Rende Facil - Saldo em 01/09/2025"));
        assertEquals(BankProfile.BANCO_DO_BRASIL, profile);
    }

    @Test
    void detectsSantanderFromFileName() {
        BankProfile profile = resolver.resolve("extrato_santander.pdf", "", List.of());
        assertEquals(BankProfile.SANTANDER, profile);
    }

    @Test
    void detectsSantanderFromContaMaxKeyword() {
        BankProfile profile = resolver.resolve(null, "APLICACAO CONTAMAX 500,00", List.of());
        assertEquals(BankProfile.SANTANDER, profile);
    }

    @Test
    void detectsSantanderFromExtratoConsolidado() {
        BankProfile profile = resolver.resolve(null, "ExtratoConsolidado 01/2026", List.of());
        assertEquals(BankProfile.SANTANDER, profile);
    }

    @Test
    void detectsSantanderFromGetnet() {
        BankProfile profile = resolver.resolve(null, "ANTECIPACAO GETNET 1.234,56", List.of());
        assertEquals(BankProfile.SANTANDER, profile);
    }

    @Test
    void returnsUnknownWhenNoKeywordsMatch() {
        BankProfile profile = resolver.resolve("statement.pdf", "Some random text", List.of("no bank info"));
        assertEquals(BankProfile.UNKNOWN, profile);
    }

    @Test
    void handlesNullInputsGracefully() {
        BankProfile profile = resolver.resolve(null, null, null);
        assertEquals(BankProfile.UNKNOWN, profile);
    }

    @Test
    void detectsBancoDoBrasilIgnoringAccents() {
        BankProfile profile = resolver.resolve(null, "Pagamento de Boleto EMPRESA XYZ 200,00 D", List.of());
        assertEquals(BankProfile.BANCO_DO_BRASIL, profile);
    }
}
