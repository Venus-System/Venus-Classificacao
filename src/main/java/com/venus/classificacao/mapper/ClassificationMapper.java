package com.venus.classificacao.mapper;

import com.venus.classificacao.config.VenusMapperConfig;
import com.venus.classificacao.dto.response.ClassificationBreakdownResponse;
import com.venus.classificacao.dto.response.ClassificationReasonResponse;
import com.venus.classificacao.dto.response.ClassificationResponse;
import com.venus.classificacao.service.ClassificationResult;
import com.venus.classificacao.service.explanation.Reason;
import org.mapstruct.Mapper;

@Mapper(config = VenusMapperConfig.class)
public interface ClassificationMapper {

    ClassificationResponse toResponse(ClassificationResult result);

    ClassificationBreakdownResponse toBreakdownResponse(ClassificationResult.Breakdown breakdown);

    ClassificationReasonResponse toReasonResponse(Reason reason);
}
