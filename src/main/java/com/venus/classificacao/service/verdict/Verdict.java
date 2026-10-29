package com.venus.classificacao.service.verdict;

import com.venus.classificacao.entity.enums.RecommendationLevel;
import com.venus.classificacao.entity.enums.RiskLevel;

public record Verdict(int finalScore, RecommendationLevel recommendationLevel, RiskLevel riskLevel) {
}
