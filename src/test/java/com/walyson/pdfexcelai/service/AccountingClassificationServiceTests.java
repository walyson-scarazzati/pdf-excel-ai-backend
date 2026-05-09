package com.walyson.pdfexcelai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.walyson.pdfexcelai.model.AccountingClassificationRule;
import com.walyson.pdfexcelai.model.ExtractedRow;
import java.util.List;
import org.junit.jupiter.api.Test;

class AccountingClassificationServiceTests {

    private final AccountingClassificationService service = new AccountingClassificationService(() -> List.of(
            new AccountingClassificationRule("Tarifas bancárias", "tarifa,tar.", "DEBIT", "1880", "7560", "53", 30),
            new AccountingClassificationRule("PIX recebido", "pix recebido,pix - recebido", "CREDIT", "7560", "3239", "55", 230),
            new AccountingClassificationRule("Pagamento padrão", "", "DEBIT", "3220", "7560", "54", 1000),
            new AccountingClassificationRule("Recebimento padrão", "", "CREDIT", "7560", "3239", "41", 1010)
    ));

    @Test
    void mapsPixReceivedToBankAndCustomerAccounts() {
        ExtractedRow row = new ExtractedRow(
                "01/09/2025",
                "957,00",
                "",
                "957,00",
                "821",
                "Pix - Recebido THAIS KARINA P");

        ExtractedRow classified = service.classify(row);

        assertEquals("R$ 957,00", classified.value());
        assertEquals("7560", classified.debit());
        assertEquals("3239", classified.credit());
        assertEquals("55", classified.historyCode());
    }

    @Test
    void mapsBoletoPaymentToGeneralPaymentsAndBankAccount() {
        ExtractedRow row = new ExtractedRow(
                "01/09/2025",
                "R$ 1.127,60",
                "1.127,60",
                "",
                "109",
                "Pagamento de Boleto MASTER DIESEL BOMBAS INJETORA");

        ExtractedRow classified = service.classify(row);

        assertEquals("R$ 1.127,60", classified.value());
        assertEquals("3220", classified.debit());
        assertEquals("7560", classified.credit());
        assertEquals("54", classified.historyCode());
    }

    @Test
    void keepsRowsThatAlreadyContainAccountCodes() {
        ExtractedRow row = new ExtractedRow(
                "02/09/2025",
                "500,00",
                "1830",
                "7560",
                "54",
                "VR BENEFICIOS");

        ExtractedRow classified = service.classify(row);

        assertEquals("R$ 500,00", classified.value());
        assertEquals("1830", classified.debit());
        assertEquals("7560", classified.credit());
        assertEquals("54", classified.historyCode());
    }

    @Test
    void classifiesLists() {
        List<ExtractedRow> rows = service.classify(List.of(new ExtractedRow(
                "03/09/2025",
                "10,00",
                "10,00",
                "",
                "",
                "Tarifa bancaria")));

        assertEquals(1, rows.size());
        assertEquals("1880", rows.get(0).debit());
        assertEquals("7560", rows.get(0).credit());
        assertEquals("53", rows.get(0).historyCode());
    }
}
