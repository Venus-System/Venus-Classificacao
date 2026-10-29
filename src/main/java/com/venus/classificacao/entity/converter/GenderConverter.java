package com.venus.classificacao.entity.converter;

import com.venus.classificacao.entity.enums.Gender;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class GenderConverter extends LowercaseEnumConverter<Gender> {
    public GenderConverter() {
        super(Gender.class);
    }
}
