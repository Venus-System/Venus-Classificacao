package com.venus.classificacao.service.verdict;

import com.venus.classificacao.entity.enums.RiskLevel;
import java.util.List;

public record AllergyMatch(String allergyName, RiskLevel severity, List<String> ingredientNames) {

    public AllergyMatch {
        ingredientNames = List.copyOf(ingredientNames);
    }
}
