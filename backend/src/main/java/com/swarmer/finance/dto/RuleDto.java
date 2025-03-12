package com.swarmer.finance.dto;

import com.swarmer.finance.models.ConditionType;
import com.swarmer.finance.models.Rule;

public record RuleDto(
        Long id,
        ConditionType conditionType,
        String conditionValue,
        CategoryDto category) {
    public static RuleDto fromEntity(Rule rule) {
        return new RuleDto(
                rule.getId(),
                rule.getConditionType(),
                rule.getConditionValue(),
                CategoryDto.fromEntity(rule.getCategory()));
    }
}