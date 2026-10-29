package com.venus.classificacao.entity.converter;

import com.venus.classificacao.entity.enums.RecommendationLevel;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class RecommendationLevelConverter extends LowercaseEnumConverter<RecommendationLevel> {
    public RecommendationLevelConverter() {
        super(RecommendationLevel.class);
    }
}
