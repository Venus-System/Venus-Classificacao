package com.venus.classificacao.service.quality;

public enum ScoreBucket {
    HEALTH(35, true),
    PERFORMANCE(20, true),
    ENVIRONMENTAL(15, true),
    ETHICAL(15, true),
    TRANSPARENCY(0, false);

    private final double baseWeight;
    private final boolean scored;

    ScoreBucket(double baseWeight, boolean scored) {
        this.baseWeight = baseWeight;
        this.scored = scored;
    }

    public double baseWeight() {
        return baseWeight;
    }

    public boolean isScored() {
        return scored;
    }
}
