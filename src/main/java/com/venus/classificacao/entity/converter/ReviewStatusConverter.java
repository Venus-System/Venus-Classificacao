package com.venus.classificacao.entity.converter;

import com.venus.classificacao.entity.enums.ReviewStatus;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ReviewStatusConverter extends LowercaseEnumConverter<ReviewStatus> {
    public ReviewStatusConverter() {
        super(ReviewStatus.class);
    }
}
