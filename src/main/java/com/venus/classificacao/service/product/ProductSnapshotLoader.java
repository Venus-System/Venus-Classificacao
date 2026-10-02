package com.venus.classificacao.service.product;

import static com.venus.classificacao.service.product.ProductSnapshot.IngredientData.MAX_NOTE;
import static com.venus.classificacao.service.product.ProductSnapshot.IngredientData.MIN_NOTE;

import com.venus.classificacao.config.ScoringProperties;
import com.venus.classificacao.entity.enums.ClaimType;
import com.venus.classificacao.entity.enums.EffectCategory;
import com.venus.classificacao.entity.enums.EffectType;
import com.venus.classificacao.entity.enums.VersionStatus;
import com.venus.classificacao.entity.ingredient.CompatibilityRule;
import com.venus.classificacao.entity.ingredient.Ingredient;
import com.venus.classificacao.entity.ingredient.IngredientEffect;
import com.venus.classificacao.entity.ingredient.ProductIngredient;
import com.venus.classificacao.entity.product.Brand;
import com.venus.classificacao.entity.product.Packaging;
import com.venus.classificacao.entity.product.ProductVersion;
import com.venus.classificacao.exception.ClassificationErrorCode;
import com.venus.classificacao.exception.ResourceNotFoundException;
import com.venus.classificacao.exception.UnprocessableAnalysisException;
import com.venus.classificacao.repository.ingredient.CompatibilityRuleRepository;
import com.venus.classificacao.repository.ingredient.IngredientEffectRepository;
import com.venus.classificacao.repository.ingredient.ProductIngredientRepository;
import com.venus.classificacao.repository.product.PackagingRepository;
import com.venus.classificacao.repository.product.ProductClaimRepository;
import com.venus.classificacao.repository.product.ProductVersionRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

@Component
public class ProductSnapshotLoader {

    private final ProductVersionRepository productVersionRepository;
    private final ProductIngredientRepository productIngredientRepository;
    private final PackagingRepository packagingRepository;
    private final ProductClaimRepository productClaimRepository;
    private final IngredientEffectRepository ingredientEffectRepository;
    private final CompatibilityRuleRepository compatibilityRuleRepository;
    private final CompatibilityRuleSelector compatibilityRuleSelector;
    private final ScoringProperties scoringProperties;

    public ProductSnapshotLoader(ProductVersionRepository productVersionRepository,
            ProductIngredientRepository productIngredientRepository,
            PackagingRepository packagingRepository,
            ProductClaimRepository productClaimRepository,
            IngredientEffectRepository ingredientEffectRepository,
            CompatibilityRuleRepository compatibilityRuleRepository,
            CompatibilityRuleSelector compatibilityRuleSelector,
            ScoringProperties scoringProperties) {
        this.productVersionRepository = productVersionRepository;
        this.productIngredientRepository = productIngredientRepository;
        this.packagingRepository = packagingRepository;
        this.productClaimRepository = productClaimRepository;
        this.ingredientEffectRepository = ingredientEffectRepository;
        this.compatibilityRuleRepository = compatibilityRuleRepository;
        this.compatibilityRuleSelector = compatibilityRuleSelector;
        this.scoringProperties = scoringProperties;
    }

    public ProductSnapshot load(Long productVersionId, Long scoringModelId, Set<Long> activeTagIds) {
        ProductVersion productVersion = getVersionOrThrow(productVersionId);
        ensureNotUnderReview(productVersion);

        List<Ingredient> ingredientEntities = getIngredientsOrThrow(productVersionId);
        List<Long> ingredientIds = ingredientEntities.stream().map(Ingredient::getId).toList();

        ProductSnapshot.BrandClaims brandClaims = toBrandClaims(productVersion.getProduct().getBrand());
        List<ProductSnapshot.IngredientData> ingredients = ingredientEntities.stream()
                .map(this::toIngredientData)
                .toList();
        Optional<ProductSnapshot.PackagingData> packaging = findPackaging(productVersionId);
        int verifiedEthicalSealCount = countVerifiedEthicalSeals(productVersionId);
        Map<Long, Integer> benefitCountByIngredientId = benefitCountByIngredientId(ingredientIds);
        List<ProductSnapshot.RuleData> rules = rulesFor(scoringModelId, ingredientIds, activeTagIds);

        return new ProductSnapshot(productVersionId, brandClaims, ingredients, packaging, verifiedEthicalSealCount,
                benefitCountByIngredientId, rules);
    }

    private ProductVersion getVersionOrThrow(Long productVersionId) {
        return productVersionRepository.findWithBrandById(productVersionId)
                .orElseThrow(() -> new ResourceNotFoundException(ClassificationErrorCode.VERSION_NOT_FOUND,
                        "Versao de produto nao encontrada com id " + productVersionId));
    }

    private void ensureNotUnderReview(ProductVersion productVersion) {
        if (productVersion.getStatus() == VersionStatus.NEEDS_REVIEW) {
            throw new UnprocessableAnalysisException(ClassificationErrorCode.VERSION_UNDER_REVIEW,
                    "A versao " + productVersion.getId() + " do produto esta em revisao e nao pode ser classificada");
        }
    }

    private List<Ingredient> getIngredientsOrThrow(Long productVersionId) {
        List<Ingredient> ingredients = productIngredientRepository.findByProductVersionIdOrderByPosition(productVersionId)
                .stream()
                .map(ProductIngredient::getIngredient)
                .toList();

        if (ingredients.isEmpty()) {
            throw new UnprocessableAnalysisException(ClassificationErrorCode.NO_INGREDIENTS,
                    "O produto nao tem nenhum ingrediente cadastrado");
        }
        return ingredients;
    }

    private Optional<ProductSnapshot.PackagingData> findPackaging(Long productVersionId) {
        return packagingRepository.findByProductVersionId(productVersionId).map(this::toPackagingData);
    }

    private int countVerifiedEthicalSeals(Long productVersionId) {
        return Math.toIntExact(productClaimRepository
                .countByProductVersionIdAndWasVerifiedTrueAndClaimClaimType(productVersionId, ClaimType.ETHICAL));
    }

    private Map<Long, Integer> benefitCountByIngredientId(List<Long> ingredientIds) {
        return ingredientEffectRepository.findByIngredientIdInAndEffectCategory(ingredientIds, EffectCategory.BENEFIT)
                .stream()
                .collect(Collectors.groupingBy(effect -> effect.getIngredient().getId(),
                        Collectors.summingInt(effect -> 1)));
    }

    private List<ProductSnapshot.RuleData> rulesFor(Long scoringModelId, List<Long> ingredientIds,
            Set<Long> activeTagIds) {
        if (activeTagIds.isEmpty()) {
            return List.of();
        }

        Long baseModelId = scoringProperties.baseModelId();
        Set<Long> scoringModelIds = Stream.of(scoringModelId, baseModelId).collect(Collectors.toSet());
        List<CompatibilityRule> rulesOfBothModels = compatibilityRuleRepository.findEnabledRules(
                scoringModelIds, ingredientIds, activeTagIds);

        List<CompatibilityRule> selectedRules = compatibilityRuleSelector.select(rulesOfBothModels, scoringModelId,
                baseModelId);
        return selectedRules.stream()
                .map(this::toRuleData)
                .toList();
    }

    private ProductSnapshot.BrandClaims toBrandClaims(Brand brand) {
        return new ProductSnapshot.BrandClaims(
                Boolean.TRUE.equals(brand.getHasVeganClaim()),
                Boolean.TRUE.equals(brand.getHasCrueltyFreeClaim()));
    }

    private ProductSnapshot.IngredientData toIngredientData(Ingredient ingredient) {
        return new ProductSnapshot.IngredientData(
                ingredient.getId(),
                ingredient.getCommonName(),
                clampNote(ingredient.getIrritationRiskLevel()),
                clampNote(ingredient.getComedogenicityScore()),
                clampNote(ingredient.getBiodegradabilityLevel()),
                clampNote(ingredient.getEnvironmentalRiskLevel()),
                isEvaluated(ingredient));
    }

    private ProductSnapshot.PackagingData toPackagingData(Packaging packaging) {
        return new ProductSnapshot.PackagingData(
                Boolean.TRUE.equals(packaging.getIsRecyclable()),
                Boolean.TRUE.equals(packaging.getIsRefillable()),
                Boolean.TRUE.equals(packaging.getIsBiodegradable()),
                packaging.getRecycledContentPercentage());
    }

    private ProductSnapshot.RuleData toRuleData(CompatibilityRule rule) {
        IngredientEffect effect = rule.getIngredientEffect();
        return new ProductSnapshot.RuleData(
                rule.getId(),
                effect.getIngredient().getId(),
                effect.getIngredient().getCommonName(),
                effect.getProfileTag().getId(),
                effect.getProfileTag().getSlug(),
                scoredEffectType(rule.getEffectType()),
                rule.getScoreDelta(),
                rule.getWeight(),
                rule.getReason());
    }

    private EffectType scoredEffectType(EffectType effectType) {
        return effectType == EffectType.ALERT ? EffectType.NEUTRAL : effectType;
    }

    private int clampNote(Short note) {
        return Math.max(MIN_NOTE, Math.min(MAX_NOTE, note));
    }

    private boolean isEvaluated(Ingredient ingredient) {
        return ingredient.getScientificConfidence() > 0;
    }
}
