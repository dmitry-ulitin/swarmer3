package com.swarmer.finance.models;

import com.fasterxml.jackson.annotation.JsonValue;

public enum TransactionType {
    TRANSFER(0),
    EXPENSE(1),
    INCOME(2),
    CORRECTION(3);

    private final int value;

    TransactionType(int value) {
        this.value = value;
    }

    @JsonValue
    public int getValue() {
        return value;
    }

    public static TransactionType fromValue(int value) {
        for (TransactionType type : TransactionType.values()) {
            if (type.value == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown TransactionType value: " + value);
    }
} 