package com.walyson.pdfexcelai.service;

import com.walyson.pdfexcelai.model.BankProfile;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class BankProfileResolver {

    public BankProfile resolve(String fileName, String rawText, List<String> pageTexts) {
        StringBuilder source = new StringBuilder();
        if (StringUtils.hasText(fileName)) {
            source.append(fileName).append('\n');
        }
        if (StringUtils.hasText(rawText)) {
            source.append(rawText).append('\n');
        }
        if (pageTexts != null && !pageTexts.isEmpty()) {
            source.append(String.join("\n", pageTexts));
        }

        String normalized = normalize(source.toString());
        if (containsAny(normalized,
                "santander",
                "contamax",
                "getnet",
                "qr checkout",
                "extratoconsolidado")) {
            return BankProfile.SANTANDER;
        }

        if (containsAny(normalized,
                "banco do brasil",
                "bb rende facil",
                "saldo em ",
                "pagamento de boleto",
                "pix - recebido",
                "pix - enviado")) {
            return BankProfile.BANCO_DO_BRASIL;
        }

        return BankProfile.UNKNOWN;
    }

    private boolean containsAny(String value, String... terms) {
        for (String term : terms) {
            if (value.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        String normalized = value == null ? "" : value.toLowerCase(Locale.ROOT);
        return Normalizer.normalize(normalized, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
    }
}