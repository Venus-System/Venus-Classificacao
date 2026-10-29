package com.venus.classificacao.entity.converter;

import com.venus.classificacao.entity.enums.AnalysisStatus;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AnalysisStatusConverter extends LowercaseEnumConverter<AnalysisStatus> {
    public AnalysisStatusConverter() {
        super(AnalysisStatus.class);
    }
}
