package com.venus.classificacao.entity.converter;

import com.venus.classificacao.entity.enums.SensitivityLevel;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class SensitivityLevelConverter extends LowercaseEnumConverter<SensitivityLevel> {
    public SensitivityLevelConverter() {
        super(SensitivityLevel.class);
    }
}
