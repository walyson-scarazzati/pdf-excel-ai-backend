package com.walyson.pdfexcelai.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiPromptConstantsTests {

    @Test
    void aiPromptConstants_bankStatementExtractionPrompt_isNotEmpty() {
        assertThat(AiPromptConstants.BANK_STATEMENT_EXTRACTION_PROMPT).isNotEmpty();
    }

    @Test
    void aiPromptConstants_bankStatementExtractionPrompt_containsKeywords() {
        String prompt = AiPromptConstants.BANK_STATEMENT_EXTRACTION_PROMPT;

        assertThat(prompt).contains("TAREFA");
        assertThat(prompt).contains("ESQUEMA");
        assertThat(prompt).contains("JSON");
        assertThat(prompt).contains("array");
    }

    @Test
    void aiPromptConstants_bankStatementExtractionPrompt_containsFieldNames() {
        String prompt = AiPromptConstants.BANK_STATEMENT_EXTRACTION_PROMPT;

        assertThat(prompt).contains("date");
        assertThat(prompt).contains("value");
        assertThat(prompt).contains("debit");
        assertThat(prompt).contains("credit");
        assertThat(prompt).contains("historyCode");
        assertThat(prompt).contains("complement");
    }

    @Test
    void aiPromptConstants_bankStatementExtractionPrompt_containsExamples() {
        String prompt = AiPromptConstants.BANK_STATEMENT_EXTRACTION_PROMPT;

        assertThat(prompt).contains("EXEMPLO");
        assertThat(prompt).contains("01/09/2025");
    }

    @Test
    void aiPromptConstants_bankStatementExtractionPrompt_containsInstructions() {
        String prompt = AiPromptConstants.BANK_STATEMENT_EXTRACTION_PROMPT;

        assertThat(prompt).contains("REGRAS");
        assertThat(prompt).contains("NORMALIZAÇÃO");
        assertThat(prompt).contains("HEURÍSTICAS");
        assertThat(prompt).contains("FORMATO");
    }

    @Test
    void aiPromptConstants_bankStatementExtractionPrompt_specifiesDateFormat() {
        String prompt = AiPromptConstants.BANK_STATEMENT_EXTRACTION_PROMPT;

        assertThat(prompt).contains("DD/MM/YYYY");
    }

    @Test
    void aiPromptConstants_bankStatementExtractionPrompt_mentionsJsonOutput() {
        String prompt = AiPromptConstants.BANK_STATEMENT_EXTRACTION_PROMPT;

        assertThat(prompt).contains("Retorne SOMENTE o JSON");
        assertThat(prompt).contains("Sem markdown");
    }

    @Test
    void aiPromptConstants_bankStatementExtractionPrompt_describesCreditAndDebit() {
        String prompt = AiPromptConstants.BANK_STATEMENT_EXTRACTION_PROMPT;

        assertThat(prompt).contains("crédito");
        assertThat(prompt).contains("débito");
        assertThat(prompt).contains("D/C");
    }

    @Test
    void aiPromptConstants_bankStatementExtractionPrompt_hasMultipleSections() {
        String prompt = AiPromptConstants.BANK_STATEMENT_EXTRACTION_PROMPT;

        // Count occurrences of section markers
        int tarefaCount = countOccurrences(prompt, "TAREFA");
        int schemCount = countOccurrences(prompt, "ESQUEMA");
        int regrasCount = countOccurrences(prompt, "REGRAS");

        assertThat(tarefaCount).isGreaterThanOrEqualTo(1);
        assertThat(schemCount).isGreaterThanOrEqualTo(1);
        assertThat(regrasCount).isGreaterThanOrEqualTo(1);
    }

    private int countOccurrences(String text, String pattern) {
        return (int) text.split(pattern, -1).length - 1;
    }
}
