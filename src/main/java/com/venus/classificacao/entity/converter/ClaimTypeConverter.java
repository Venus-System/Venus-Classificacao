package com.venus.classificacao.entity.converter;

import com.venus.classificacao.entity.enums.ClaimType;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ClaimTypeConverter extends LowercaseEnumConverter<ClaimType> {
    public ClaimTypeConverter() {
        super(ClaimType.class);
    }
}
