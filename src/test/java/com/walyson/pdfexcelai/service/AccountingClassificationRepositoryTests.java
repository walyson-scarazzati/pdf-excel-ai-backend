package com.walyson.pdfexcelai.service;

import com.walyson.pdfexcelai.model.AccountingClassificationRule;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AccountingClassificationRepositoryTests {

    @Test
    void accountingClassificationRepository_findKnownAccountCodes_defaultReturnsEmptyList() {
        AccountingClassificationRepository repository = createTestRepository();

        List<String> codes = repository.findKnownAccountCodes();

        assertThat(codes).isEmpty();
    }

    @Test
    void accountingClassificationRepository_findActiveRules() {
        AccountingClassificationRepository repository = new AccountingClassificationRepository() {
            @Override
            public List<AccountingClassificationRule> findActiveRules() {
                return List.of(
                        new AccountingClassificationRule("PIX", "PIX", "C", "1001", "5001", "001", 1)
                );
            }
        };

        List<AccountingClassificationRule> rules = repository.findActiveRules();

        assertThat(rules).hasSize(1);
        assertThat(rules.get(0).name()).isEqualTo("PIX");
    }

    @Test
    void accountingClassificationRepository_findKnownAccountCodes_canBeOverridden() {
        AccountingClassificationRepository repository = new AccountingClassificationRepository() {
            @Override
            public List<AccountingClassificationRule> findActiveRules() {
                return List.of();
            }

            @Override
            public List<String> findKnownAccountCodes() {
                return List.of("1001", "5001", "2050");
            }
        };

        List<String> codes = repository.findKnownAccountCodes();

        assertThat(codes).hasSize(3);
        assertThat(codes).contains("1001", "5001", "2050");
    }

    @Test
    void accountingClassificationRepository_multipleRules() {
        AccountingClassificationRepository repository = new AccountingClassificationRepository() {
            @Override
            public List<AccountingClassificationRule> findActiveRules() {
                return List.of(
                        new AccountingClassificationRule("PIX", "PIX", "C", "1001", "5001", "001", 1),
                        new AccountingClassificationRule("TED", "TED", "C", "1001", "5002", "002", 2),
                        new AccountingClassificationRule("BOLETO", "BOLETO", "D", "1050", "2050", "003", 3)
                );
            }
        };

        List<AccountingClassificationRule> rules = repository.findActiveRules();

        assertThat(rules).hasSize(3);
        assertThat(rules).extracting(AccountingClassificationRule::name)
                .contains("PIX", "TED", "BOLETO");
    }

    @Test
    void accountingClassificationRepository_emptyRulesList() {
        AccountingClassificationRepository repository = new AccountingClassificationRepository() {
            @Override
            public List<AccountingClassificationRule> findActiveRules() {
                return List.of();
            }
        };

        List<AccountingClassificationRule> rules = repository.findActiveRules();

        assertThat(rules).isEmpty();
    }

    private AccountingClassificationRepository createTestRepository() {
        return new AccountingClassificationRepository() {
            @Override
            public List<AccountingClassificationRule> findActiveRules() {
                return List.of();
            }
        };
    }
}
