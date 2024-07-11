package com.swarmer.finance.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.swarmer.finance.models.ConditionType;
import com.swarmer.finance.models.Rule;

public record DumpRule(Long id, ConditionType conditionType, String conditionValue,
        @JsonProperty("category_id") Long categoryId, LocalDateTime created, LocalDateTime updated) {
    public static DumpRule from(Rule rule) {
        return new DumpRule(rule.getId(), rule.getConditionType(), rule.getConditionValue(),
                rule.getCategory().getId(), rule.getCreated(), rule.getUpdated());
    }
}
