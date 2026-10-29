package com.venus.classificacao.entity.converter;

import com.venus.classificacao.entity.enums.VersionStatus;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class VersionStatusConverter extends LowercaseEnumConverter<VersionStatus> {
    public VersionStatusConverter() {
        super(VersionStatus.class);
    }
}
