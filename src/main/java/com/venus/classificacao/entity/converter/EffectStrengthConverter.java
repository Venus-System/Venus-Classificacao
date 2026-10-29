package com.venus.classificacao.entity.converter;

import com.venus.classificacao.entity.enums.EffectStrength;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class EffectStrengthConverter extends LowercaseEnumConverter<EffectStrength> {
    public EffectStrengthConverter() {
        super(EffectStrength.class);
    }
}
