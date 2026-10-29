package com.venus.classificacao.entity.converter;

import com.venus.classificacao.entity.enums.RiskLevel;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class RiskLevelConverter extends LowercaseEnumConverter<RiskLevel> {
    public RiskLevelConverter() {
        super(RiskLevel.class);
    }
}
