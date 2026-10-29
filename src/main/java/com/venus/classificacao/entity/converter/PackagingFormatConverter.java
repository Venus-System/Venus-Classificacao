package com.venus.classificacao.entity.converter;

import com.venus.classificacao.entity.enums.PackagingFormat;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PackagingFormatConverter extends LowercaseEnumConverter<PackagingFormat> {
    public PackagingFormatConverter() {
        super(PackagingFormat.class);
    }
}
