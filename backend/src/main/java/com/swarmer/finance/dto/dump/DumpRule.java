package com.swarmer.finance.dto.dump;

import java.time.LocalDateTime;

import com.swarmer.finance.models.ConditionType;
import com.swarmer.finance.models.Rule;

public record DumpRule(Long id, ConditionType conditionType, String conditionValue, Long categoryId,
        LocalDateTime created, LocalDateTime updated) {
    public static DumpRule fromEntity(Rule rule) {
        return new DumpRule(rule.getId(), rule.getConditionType(), rule.getConditionValue(),
                rule.getCategory().getId(), rule.getCreated(), rule.getUpdated());
    }
}
