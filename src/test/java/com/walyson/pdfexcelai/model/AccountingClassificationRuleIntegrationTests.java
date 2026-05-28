package com.walyson.pdfexcelai.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AccountingClassificationRuleIntegrationTests {

    @Test
    void accountingClassificationRule_pixRule() {
        AccountingClassificationRule rule = new AccountingClassificationRule(
                "PIX",
                "PIX,Pix,pix",
                "C",
                "1001",
                "5001",
                "001",
                10
        );

        assertThat(rule.name()).isEqualTo("PIX");
        assertThat(rule.keywords()).contains("PIX", "Pix", "pix");
        assertThat(rule.direction()).isEqualTo("C");
        assertThat(rule.debitAccountCode()).isEqualTo("1001");
        assertThat(rule.creditAccountCode()).isEqualTo("5001");
        assertThat(rule.priority()).isEqualTo(10);
    }

    @Test
    void accountingClassificationRule_multipleRules() {
        AccountingClassificationRule[] rules = {
                new AccountingClassificationRule("PIX", "PIX", "C", "1001", "5001", "001", 10),
                new AccountingClassificationRule("TED", "TED", "C", "1001", "5002", "002", 20),
                new AccountingClassificationRule("BOLETO", "BOLETO", "D", "1050", "2050", "003", 15),
                new AccountingClassificationRule("DOC", "DOC", "C", "1001", "5003", "004", 5)
        };

        assertThat(rules).hasSize(4);
        assertThat(rules[0].priority()).isEqualTo(10);
        assertThat(rules[1].priority()).isEqualTo(20);
        assertThat(rules[2].priority()).isEqualTo(15);
        assertThat(rules[3].priority()).isEqualTo(5);
    }

    @Test
    void accountingClassificationRule_creditAndDebitRules() {
        AccountingClassificationRule creditRule = new AccountingClassificationRule(
                "Income", "SALARY,BONUS", "C", "1100", "5100", "100", 50
        );
        AccountingClassificationRule debitRule = new AccountingClassificationRule(
                "Expense", "EXPENSE,COST", "D", "1200", "2200", "200", 40
        );

        assertThat(creditRule.direction()).isEqualTo("C");
        assertThat(debitRule.direction()).isEqualTo("D");
        assertThat(creditRule.direction()).isNotEqualTo(debitRule.direction());
    }

    @Test
    void accountingClassificationRule_priority_comparison() {
        AccountingClassificationRule highPriority = new AccountingClassificationRule(
                "High", "keywords", "C", "1001", "5001", "001", 100
        );
        AccountingClassificationRule mediumPriority = new AccountingClassificationRule(
                "Medium", "keywords", "C", "1001", "5001", "002", 50
        );
        AccountingClassificationRule lowPriority = new AccountingClassificationRule(
                "Low", "keywords", "C", "1001", "5001", "003", 10
        );

        assertThat(highPriority.priority()).isGreaterThan(mediumPriority.priority());
        assertThat(mediumPriority.priority()).isGreaterThan(lowPriority.priority());
    }

    @Test
    void accountingClassificationRule_accountCodeVariations() {
        AccountingClassificationRule rule1 = new AccountingClassificationRule(
                "Rule1", "keywords", "C", "1001", "5001", "001", 1
        );
        AccountingClassificationRule rule2 = new AccountingClassificationRule(
                "Rule2", "keywords", "C", "1100", "5100", "002", 1
        );

        assertThat(rule1.debitAccountCode()).isNotEqualTo(rule2.debitAccountCode());
        assertThat(rule1.creditAccountCode()).isNotEqualTo(rule2.creditAccountCode());
    }

    @Test
    void accountingClassificationRule_equality() {
        AccountingClassificationRule rule1 = new AccountingClassificationRule(
                "Test", "key1,key2", "C", "1001", "5001", "001", 1
        );
        AccountingClassificationRule rule2 = new AccountingClassificationRule(
                "Test", "key1,key2", "C", "1001", "5001", "001", 1
        );
        AccountingClassificationRule rule3 = new AccountingClassificationRule(
                "Test", "key1,key2", "D", "1001", "5001", "001", 1
        );

        assertThat(rule1).isEqualTo(rule2);
        assertThat(rule1).isNotEqualTo(rule3);
    }
}
