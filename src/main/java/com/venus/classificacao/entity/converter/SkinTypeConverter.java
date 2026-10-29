package com.venus.classificacao.entity.converter;

import com.venus.classificacao.entity.enums.SkinType;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class SkinTypeConverter extends LowercaseEnumConverter<SkinType> {
    public SkinTypeConverter() {
        super(SkinType.class);
    }
}
