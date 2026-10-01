package com.venus.classificacao.entity.converter;

import com.venus.classificacao.entity.enums.EffectType;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class EffectTypeConverter extends LowercaseEnumConverter<EffectType> {
    public EffectTypeConverter() {
        super(EffectType.class);
    }
}
