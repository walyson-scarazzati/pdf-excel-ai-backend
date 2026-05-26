package com.walyson.pdfexcelai.service;

import com.walyson.pdfexcelai.model.AccountingClassificationRule;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcAccountingClassificationRepository implements AccountingClassificationRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcAccountingClassificationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<AccountingClassificationRule> findActiveRules() {
        return jdbcTemplate.query("""
                SELECT name, keywords, direction, debit_account_code, credit_account_code, history_code, priority
                FROM accounting_classification_rules
                WHERE active = true
                ORDER BY priority ASC, id ASC
                """,
                (rs, rowNum) -> new AccountingClassificationRule(
                        rs.getString("name"),
                        rs.getString("keywords"),
                        rs.getString("direction"),
                        rs.getString("debit_account_code"),
                        rs.getString("credit_account_code"),
                        rs.getString("history_code"),
                        rs.getInt("priority")));
    }

    @Override
    public List<String> findKnownAccountCodes() {
        return jdbcTemplate.query("""
                SELECT code
                FROM accounting_accounts
                ORDER BY LENGTH(code) DESC, code ASC
                """, (rs, rowNum) -> rs.getString("code"));
    }
}
