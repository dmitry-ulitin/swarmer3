package com.swarmer.finance.models;

import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonValue;

public enum BankType {
    LHV(1L), TINKOFF(2L), SBER(3L), ALFABANK(4L);
    private final Long value;
    private BankType(Long value) {
        this.value = value;
    }

    @JsonValue
    public Long getValue() {
        return value;
    }
    
    public static BankType fromValue(Long value) {
        if (value == null) {
            return null;
        }
        return Stream.of(BankType.values()).filter(c -> value == c.getValue()).findFirst().orElseThrow(IllegalArgumentException::new);
    }
}
