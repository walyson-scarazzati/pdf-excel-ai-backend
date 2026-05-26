package com.walyson.pdfexcelai.service;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Service;

@Service
public class CsvDecoderService {

    private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");
    private static final byte UTF8_BOM_1 = (byte) 0xEF;
    private static final byte UTF8_BOM_2 = (byte) 0xBB;
    private static final byte UTF8_BOM_3 = (byte) 0xBF;

    public String decode(byte[] content) {
        if (content == null || content.length == 0) {
            return "";
        }

        int offset = hasUtf8Bom(content) ? 3 : 0;

        String utf8 = tryDecode(content, offset, StandardCharsets.UTF_8);
        if (utf8 != null) {
            return utf8;
        }

        String cp1252 = new String(content, offset, content.length - offset, WINDOWS_1252);
        String latin1 = new String(content, offset, content.length - offset, StandardCharsets.ISO_8859_1);

        return scorePortuguese(cp1252) >= scorePortuguese(latin1) ? cp1252 : latin1;
    }

    private String tryDecode(byte[] content, int offset, Charset charset) {
        CharsetDecoder decoder = charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            return decoder.decode(ByteBuffer.wrap(content, offset, content.length - offset)).toString();
        } catch (CharacterCodingException ex) {
            return null;
        }
    }

    private int scorePortuguese(String text) {
        String lower = text.toLowerCase();
        int score = 0;
        score += count(lower, "débito") * 5;
        score += count(lower, "crédito") * 5;
        score += count(lower, "código") * 4;
        score += count(lower, "histórico") * 4;
        score += count(lower, "agência") * 2;
        score += count(lower, "conta") * 2;
        score -= count(lower, "�") * 6;
        return score;
    }

    private int count(String text, String token) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(token, index)) >= 0) {
            count++;
            index += token.length();
        }
        return count;
    }

    private boolean hasUtf8Bom(byte[] content) {
        return content.length >= 3
                && content[0] == UTF8_BOM_1
                && content[1] == UTF8_BOM_2
                && content[2] == UTF8_BOM_3;
    }
}