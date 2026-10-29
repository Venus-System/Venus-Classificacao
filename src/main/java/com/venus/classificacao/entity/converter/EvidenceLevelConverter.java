package com.venus.classificacao.entity.converter;

import com.venus.classificacao.entity.enums.EvidenceLevel;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class EvidenceLevelConverter extends LowercaseEnumConverter<EvidenceLevel> {
    public EvidenceLevelConverter() {
        super(EvidenceLevel.class);
    }
}
