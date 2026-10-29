package com.venus.classificacao.entity.converter;

import com.venus.classificacao.entity.enums.AgeRange;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AgeRangeConverter extends LowercaseEnumConverter<AgeRange> {
    public AgeRangeConverter() {
        super(AgeRange.class);
    }
}
