package com.swarmer.finance.models;

import java.util.stream.Stream;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ConditionTypeConverter implements AttributeConverter<ConditionType, Integer> {

    @Override
    public Integer convertToDatabaseColumn(ConditionType attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public ConditionType convertToEntityAttribute(Integer dbData) {
        if (dbData == null) {
            return null;
        }
        return Stream.of(ConditionType.values()).filter(c -> dbData == c.getValue()).findFirst().orElseThrow(IllegalArgumentException::new);
    }
}
