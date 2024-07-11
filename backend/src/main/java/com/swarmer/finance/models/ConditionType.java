package com.swarmer.finance.models;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ConditionType {
    PARTY_EQUALS(1), PARTY_CONTAINS(2), DETAILS_EQUALS(3), DETAILS_CONTAINS(4);
    private final int value;
    private ConditionType(int value) {
        this.value = value;
    }

    @JsonValue
    public int getValue() {
        return value;
    }
}
