package com.venus.classificacao.entity.converter;

import com.venus.classificacao.entity.enums.EffectCategory;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class EffectCategoryConverter extends LowercaseEnumConverter<EffectCategory> {
    public EffectCategoryConverter() {
        super(EffectCategory.class);
    }
}
