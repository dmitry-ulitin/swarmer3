package com.swarmer.finance.dto;

import com.swarmer.finance.models.Category;
import com.swarmer.finance.models.ConditionType;

public record RuleDto(Long id, ConditionType conditionType,
        String conditionValue, Category category) {
    public static RuleDto from(com.swarmer.finance.models.Rule rule) {
        return new RuleDto(rule.getId(), rule.getConditionType(),
                rule.getConditionValue(), rule.getCategory());
    }
}
