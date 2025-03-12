package com.swarmer.finance.models;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ConditionType {
    PARTY_EQUALS(1),
    PARTY_CONTAINS(2),
    DETAILS_EQUALS(3),
    DETAILS_CONTAINS(4),
    CATNAME_EQUALS(5),
    CATNAME_CONTAINS(6);

    private final int value;

    ConditionType(int value) {
        this.value = value;
    }

    @JsonValue
    public int getValue() {
        return value;
    }

    public static ConditionType fromValue(int value) {
        for (ConditionType type : ConditionType.values()) {
            if (type.value == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown condition type value: " + value);
    }
} 