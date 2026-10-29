package com.venus.classificacao.entity.converter;

import com.venus.classificacao.entity.enums.SourceType;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class SourceTypeConverter extends LowercaseEnumConverter<SourceType> {
    public SourceTypeConverter() {
        super(SourceType.class);
    }
}
