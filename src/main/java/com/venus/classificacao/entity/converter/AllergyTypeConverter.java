package com.venus.classificacao.entity.converter;

import com.venus.classificacao.entity.enums.AllergyType;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AllergyTypeConverter extends LowercaseEnumConverter<AllergyType> {
    public AllergyTypeConverter() {
        super(AllergyType.class);
    }
}
