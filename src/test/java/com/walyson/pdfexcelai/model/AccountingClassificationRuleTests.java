package com.walyson.pdfexcelai.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AccountingClassificationRuleTests {

    @Test
    void accountingClassificationRule_canBeCreated() {
        AccountingClassificationRule rule = new AccountingClassificationRule(
                "PIX Rule", "PIX,Pix", "C", "1001", "5001", "001", 1
        );

        assertThat(rule.name()).isEqualTo("PIX Rule");
        assertThat(rule.keywords()).isEqualTo("PIX,Pix");
        assertThat(rule.direction()).isEqualTo("C");
        assertThat(rule.debitAccountCode()).isEqualTo("1001");
        assertThat(rule.creditAccountCode()).isEqualTo("5001");
        assertThat(rule.historyCode()).isEqualTo("001");
        assertThat(rule.priority()).isEqualTo(1);
    }

    @Test
    void accountingClassificationRule_recordContract() {
        AccountingClassificationRule rule1 = new AccountingClassificationRule(
                "TED", "TED,ted", "C", "1001", "5001", "002", 2
        );
        AccountingClassificationRule rule2 = new AccountingClassificationRule(
                "TED", "TED,ted", "C", "1001", "5001", "002", 2
        );

        assertThat(rule1).isEqualTo(rule2);
        assertThat(rule1.hashCode()).isEqualTo(rule2.hashCode());
    }

    @Test
    void accountingClassificationRule_withNullFields() {
        AccountingClassificationRule rule = new AccountingClassificationRule(
                null, null, null, null, null, null, 0
        );

        assertThat(rule.name()).isNull();
        assertThat(rule.keywords()).isNull();
        assertThat(rule.priority()).isEqualTo(0);
    }

    @Test
    void accountingClassificationRule_toString() {
        AccountingClassificationRule rule = new AccountingClassificationRule(
                "Boleto", "BOLETO,boleto", "D", "1001", "5001", "003", 5
        );

        assertThat(rule.toString()).contains("Boleto", "BOLETO", "1001", "5001");
    }

    @Test
    void accountingClassificationRule_differentPriorities() {
        AccountingClassificationRule highPriority = new AccountingClassificationRule(
                "High", "keywords", "C", "1001", "5001", "001", 100
        );
        AccountingClassificationRule lowPriority = new AccountingClassificationRule(
                "Low", "keywords", "C", "1001", "5001", "002", 1
        );

        assertThat(highPriority.priority()).isGreaterThan(lowPriority.priority());
    }

    @Test
    void accountingClassificationRule_debitAndCreditAccounts() {
        AccountingClassificationRule rule = new AccountingClassificationRule(
                "Transfer", "TRANSFER", "D", "1050", "2050", "050", 50
        );

        assertThat(rule.debitAccountCode()).isEqualTo("1050");
        assertThat(rule.creditAccountCode()).isEqualTo("2050");
        assertThat(rule.direction()).isEqualTo("D");
    }
}
