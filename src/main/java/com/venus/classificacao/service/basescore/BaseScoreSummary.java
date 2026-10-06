package com.venus.classificacao.service.basescore;

import com.venus.classificacao.exception.ClassificationErrorCode;
import java.util.List;

public record BaseScoreSummary(
        Long scoringModelId,
        int totalVersions,
        int recalculatedCount,
        List<SkippedVersion> skipped,
        int processingTimeMs
) {

    public BaseScoreSummary {
        skipped = List.copyOf(skipped);
    }

    public record SkippedVersion(Long productId, Long productVersionId, ClassificationErrorCode code) {
    }
}
