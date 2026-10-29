package com.venus.classificacao.service.quality;

import static java.util.Map.entry;

import java.util.Map;

public final class ScoreCategoryBuckets {

    private static final Map<String, ScoreBucket> BUCKET_BY_CATEGORY = Map.ofEntries(
            entry("Segurança", ScoreBucket.HEALTH),
            entry("Risco Alérgico", ScoreBucket.HEALTH),
            entry("Potencial de Irritação", ScoreBucket.HEALTH),
            entry("Risco Comedogênico", ScoreBucket.HEALTH),
            entry("Compatibilidade com Acne", ScoreBucket.HEALTH),
            entry("Pele Sensível", ScoreBucket.HEALTH),
            entry("Segurança na Gravidez", ScoreBucket.HEALTH),
            entry("Segurança de Longo Prazo", ScoreBucket.HEALTH),
            entry("Segurança de Curto Prazo", ScoreBucket.HEALTH),
            entry("Sistema Conservante", ScoreBucket.HEALTH),
            entry("Risco de Fragrância", ScoreBucket.HEALTH),
            entry("Robustez Microbiológica", ScoreBucket.HEALTH),
            entry("Qualidade dos Ingredientes", ScoreBucket.HEALTH),
            entry("Efetividade", ScoreBucket.PERFORMANCE),
            entry("Compatibilidade", ScoreBucket.PERFORMANCE),
            entry("Evidência Científica", ScoreBucket.PERFORMANCE),
            entry("Equilíbrio da Fórmula", ScoreBucket.PERFORMANCE),
            entry("Sinergia de Ingredientes", ScoreBucket.PERFORMANCE),
            entry("Estabilidade da Fórmula", ScoreBucket.PERFORMANCE),
            entry("Experiência Sensorial", ScoreBucket.PERFORMANCE),
            entry("Compatibilidade Capilar", ScoreBucket.PERFORMANCE),
            entry("Compatibilidade do Couro Cabeludo", ScoreBucket.PERFORMANCE),
            entry("Qualidade do Pigmento", ScoreBucket.PERFORMANCE),
            entry("Estabilidade de Prateleira", ScoreBucket.PERFORMANCE),
            entry("Preço vs Performance", ScoreBucket.PERFORMANCE),
            entry("Impacto Ambiental", ScoreBucket.ENVIRONMENTAL),
            entry("Sustentabilidade", ScoreBucket.ENVIRONMENTAL),
            entry("Sustentabilidade da Embalagem", ScoreBucket.ENVIRONMENTAL),
            entry("Cruelty Free", ScoreBucket.ETHICAL),
            entry("Vegano", ScoreBucket.ETHICAL),
            entry("Transparência", ScoreBucket.TRANSPARENCY),
            entry("Integridade do Claim", ScoreBucket.TRANSPARENCY));

    private ScoreCategoryBuckets() {
    }

    public static ScoreBucket bucketOf(String categoryName) {
        ScoreBucket bucket = BUCKET_BY_CATEGORY.get(categoryName);
        if (bucket == null) {
            throw new IllegalStateException("Categoria de score sem balde no motor: " + categoryName);
        }
        return bucket;
    }
}
