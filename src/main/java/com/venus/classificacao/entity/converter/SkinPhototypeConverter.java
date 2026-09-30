package com.venus.classificacao.entity.converter;

import com.venus.classificacao.entity.enums.SkinPhototype;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class SkinPhototypeConverter extends LowercaseEnumConverter<SkinPhototype> {
    public SkinPhototypeConverter() {
        super(SkinPhototype.class);
    }
}
