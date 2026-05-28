package com.walyson.pdfexcelai.service;

import com.walyson.pdfexcelai.model.AccountingClassificationRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JdbcAccountingClassificationRepositoryTests {

    @Mock private JdbcTemplate jdbcTemplate;

    private JdbcAccountingClassificationRepository repository;

    @BeforeEach
    void setUp() {
        repository = new JdbcAccountingClassificationRepository(jdbcTemplate);
    }

    @Test
    void findActiveRules_returnsMappedRules() {
        List<AccountingClassificationRule> rules = List.of(
                new AccountingClassificationRule("PIX", "pix|transfer", "CREDIT", "1", "2", "101", 1),
                new AccountingClassificationRule("BOLETO", "boleto", "DEBIT", "3", "4", "202", 2)
        );
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(rules);

        List<AccountingClassificationRule> result = repository.findActiveRules();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).name()).isEqualTo("PIX");
        verify(jdbcTemplate).query(anyString(), any(RowMapper.class));
    }

    @Test
    void findKnownAccountCodes_returnsCodes() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(List.of("111", "222", "333"));

        List<String> codes = repository.findKnownAccountCodes();

        assertThat(codes).containsExactly("111", "222", "333");
        verify(jdbcTemplate).query(anyString(), any(RowMapper.class));
    }
}
