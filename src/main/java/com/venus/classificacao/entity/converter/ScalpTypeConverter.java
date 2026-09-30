package com.venus.classificacao.entity.converter;

import com.venus.classificacao.entity.enums.ScalpType;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ScalpTypeConverter extends LowercaseEnumConverter<ScalpType> {
    public ScalpTypeConverter() {
        super(ScalpType.class);
    }
}
