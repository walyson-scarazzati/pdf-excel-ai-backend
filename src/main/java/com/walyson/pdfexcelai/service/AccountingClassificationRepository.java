package com.walyson.pdfexcelai.service;

import com.walyson.pdfexcelai.model.AccountingClassificationRule;
import java.util.List;

public interface AccountingClassificationRepository {

    List<AccountingClassificationRule> findActiveRules();

    default List<String> findKnownAccountCodes() {
        return List.of();
    }
}
