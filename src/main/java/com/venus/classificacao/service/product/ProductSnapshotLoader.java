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
import com.venus.classificacao.exception.DataAccessFailureTranslator;
import com.venus.classificacao.exception.DataIntegrityViolationTranslator;
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
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
public class ProductSnapshotLoader {

    private static final Logger log = LoggerFactory.getLogger(ProductSnapshotLoader.class);

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
        Optional<ProductVersion> productVersion = executeOrFail(
                () -> productVersionRepository.findWithBrandById(productVersionId),
                "Falha ao consultar versao de produto no banco de dados");

        return productVersion.orElseThrow(() -> new ResourceNotFoundException(ClassificationErrorCode.VERSION_NOT_FOUND,
                "Versao de produto nao encontrada com id " + productVersionId));
    }

    private void ensureNotUnderReview(ProductVersion productVersion) {
        if (productVersion.getStatus() == VersionStatus.NEEDS_REVIEW) {
            throw new UnprocessableAnalysisException(ClassificationErrorCode.VERSION_UNDER_REVIEW,
                    "A versao " + productVersion.getId() + " do produto esta em revisao e nao pode ser classificada");
        }
    }

    private List<Ingredient> getIngredientsOrThrow(Long productVersionId) {
        List<ProductIngredient> productIngredients = executeOrFail(
                () -> productIngredientRepository.findByProductVersionIdOrderByPosition(productVersionId),
                "Falha ao consultar ingredientes da versao de produto");

        List<Ingredient> ingredients = productIngredients.stream()
                .map(ProductIngredient::getIngredient)
                .toList();

        if (ingredients.isEmpty()) {
            throw new UnprocessableAnalysisException(ClassificationErrorCode.NO_INGREDIENTS,
                    "O produto nao tem nenhum ingrediente cadastrado");
        }
        return ingredients;
    }

    private Optional<ProductSnapshot.PackagingData> findPackaging(Long productVersionId) {
        Optional<Packaging> packaging = executeOrFail(() -> packagingRepository.findByProductVersionId(productVersionId),
                "Falha ao consultar embalagem no banco de dados");

        return packaging.map(this::toPackagingData);
    }

    private int countVerifiedEthicalSeals(Long productVersionId) {
        long verifiedEthicalSealCount = executeOrFail(() -> productClaimRepository
                        .countByProductVersionIdAndWasVerifiedTrueAndClaimClaimType(productVersionId, ClaimType.ETHICAL),
                "Falha ao consultar claims da versao de produto");

        return Math.toIntExact(verifiedEthicalSealCount);
    }

    private Map<Long, Integer> benefitCountByIngredientId(List<Long> ingredientIds) {
        List<IngredientEffect> benefitEffects = executeOrFail(
                () -> ingredientEffectRepository.findByIngredientIdInAndEffectCategory(ingredientIds, EffectCategory.BENEFIT),
                "Falha ao consultar efeitos de ingrediente");

        return benefitEffects.stream()
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
        List<CompatibilityRule> rulesOfBothModels = executeOrFail(
                () -> compatibilityRuleRepository.findEnabledRules(scoringModelIds, ingredientIds, activeTagIds),
                "Falha ao consultar regras de compatibilidade ativas do modelo de scoring");

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
                packaging.getRecycledContentPercentage(),
                isEvaluated(packaging));
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

    private boolean isEvaluated(Packaging packaging) {
        return packaging.getConfidenceScore() > 0;
    }

    private <T> T executeOrFail(Supplier<T> action, String errorMessage) {
        try {
            return action.get();
        } catch (DataIntegrityViolationException ex) {
            throw DataIntegrityViolationTranslator.translate(ex);
        } catch (DataAccessException ex) {
            log.error(errorMessage, ex);
            throw DataAccessFailureTranslator.translate(ex, errorMessage);
        }
    }
}
