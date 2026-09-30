package com.venus.classificacao.entity.converter;

import com.venus.classificacao.entity.enums.PackagingMaterial;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PackagingMaterialConverter extends LowercaseEnumConverter<PackagingMaterial> {
    public PackagingMaterialConverter() {
        super(PackagingMaterial.class);
    }
}
