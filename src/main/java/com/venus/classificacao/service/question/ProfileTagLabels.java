package com.venus.classificacao.service.question;

import static java.util.Map.entry;

import java.util.Map;

public final class ProfileTagLabels {

    private static final Map<String, String> LABEL_BY_SLUG = Map.ofEntries(
            entry(ProfileTagSlugs.REACTIVE_SKIN, "pele sensível"),
            entry(ProfileTagSlugs.OIL_CONTROL, "pele oleosa"),
            entry(ProfileTagSlugs.VERY_DRY_SKIN, "pele seca"),
            entry(ProfileTagSlugs.ACNEIC_SKIN, "tendência a acne"),
            entry(ProfileTagSlugs.ROSACEA, "rosácea"),
            entry(ProfileTagSlugs.ECZEMA, "eczema"),
            entry(ProfileTagSlugs.HYPERPIGMENTATION, "hiperpigmentação"),
            entry(ProfileTagSlugs.MELASMA, "melasma"),
            entry(ProfileTagSlugs.DANDRUFF, "couro cabeludo com caspa"),
            entry(ProfileTagSlugs.DRY_SCALP, "couro cabeludo seco"),
            entry(ProfileTagSlugs.OILY_SCALP, "couro cabeludo oleoso"),
            entry(ProfileTagSlugs.SENSITIVE_SCALP, "couro cabeludo sensível"),
            entry(ProfileTagSlugs.PREGNANCY, "gravidez"),
            entry(ProfileTagSlugs.BREASTFEEDING, "amamentação"),
            entry(ProfileTagSlugs.PARABEN_FREE, "sem parabenos"),
            entry(ProfileTagSlugs.SULFATE_FREE, "sem sulfatos"),
            entry(ProfileTagSlugs.SILICONE_FREE, "sem silicones"),
            entry(ProfileTagSlugs.FRAGRANCE_FREE, "sem fragrância"));

    private ProfileTagLabels() {
    }

    public static String labelOf(String slug, String tagName) {
        return LABEL_BY_SLUG.getOrDefault(slug, tagName);
    }
}
