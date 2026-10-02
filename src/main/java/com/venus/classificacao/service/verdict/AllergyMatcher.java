package com.venus.classificacao.service.verdict;

import com.venus.classificacao.service.product.ProductSnapshot;
import com.venus.classificacao.service.profile.ProfileSnapshot;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class AllergyMatcher {

    public List<AllergyMatch> findMatches(ProfileSnapshot profile, ProductSnapshot product) {
        Map<Long, String> ingredientNameById = product.ingredients().stream()
                .collect(Collectors.toMap(ProductSnapshot.IngredientData::id, ProductSnapshot.IngredientData::name));

        return profile.allergies().stream()
                .map(allergy -> toMatch(allergy, ingredientNameById))
                .filter(match -> !match.ingredientNames().isEmpty())
                .sorted(Comparator.comparing(AllergyMatch::severity).reversed())
                .toList();
    }

    private AllergyMatch toMatch(ProfileSnapshot.DeclaredAllergy allergy, Map<Long, String> ingredientNameById) {
        List<String> ingredientNamesInProduct = allergy.ingredientIds().stream()
                .filter(ingredientNameById::containsKey)
                .map(ingredientNameById::get)
                .sorted()
                .toList();

        return new AllergyMatch(allergy.name(), allergy.severity(), ingredientNamesInProduct);
    }
}
