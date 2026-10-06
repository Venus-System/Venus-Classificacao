package com.venus.classificacao.mapper;

import com.venus.classificacao.config.VenusMapperConfig;
import com.venus.classificacao.dto.response.BaseScoreResponse;
import com.venus.classificacao.dto.response.BaseScoreSummaryResponse;
import com.venus.classificacao.dto.response.SkippedVersionResponse;
import com.venus.classificacao.service.basescore.BaseScoreResult;
import com.venus.classificacao.service.basescore.BaseScoreSummary;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = VenusMapperConfig.class)
public interface BaseScoreMapper {

    @Mapping(target = "qualityScore", source = "scores.qualityScore")
    @Mapping(target = "healthScore", source = "scores.healthScore")
    @Mapping(target = "environmentalScore", source = "scores.environmentalScore")
    @Mapping(target = "ethicalScore", source = "scores.ethicalScore")
    @Mapping(target = "performanceScore", source = "scores.performanceScore")
    BaseScoreResponse toResponse(BaseScoreResult result);

    BaseScoreSummaryResponse toSummaryResponse(BaseScoreSummary summary);

    SkippedVersionResponse toSkippedVersionResponse(BaseScoreSummary.SkippedVersion skippedVersion);
}
