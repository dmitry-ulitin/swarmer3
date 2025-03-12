package com.swarmer.finance.models;

import com.fasterxml.jackson.annotation.JsonValue;

public enum BankType {
    LHV(1), TINKOFF(2), SBER(3), ALFABANK(4), UNICREDIT(5), CAIXA(6);

    private final int value;

    BankType(int value) {
        this.value = value;
    }

    @JsonValue
    public int getValue() {
        return value;
    }

    public static BankType fromValue(int value) {
        for (BankType type : BankType.values()) {
            if (type.value == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown BankType value: " + value);
    }
}
