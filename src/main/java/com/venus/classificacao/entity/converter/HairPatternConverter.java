package com.venus.classificacao.entity.converter;

import com.venus.classificacao.entity.enums.HairPattern;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class HairPatternConverter implements AttributeConverter<HairPattern, String> {

    private static final String PREFIX = "TYPE_";

    @Override
    public String convertToDatabaseColumn(HairPattern attribute) {
        return attribute == null ? null : attribute.name().substring(PREFIX.length());
    }

    @Override
    public HairPattern convertToEntityAttribute(String dbData) {
        return dbData == null ? null : HairPattern.valueOf(PREFIX + dbData);
    }
}
