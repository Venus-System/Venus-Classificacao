package com.venus.classificacao.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.venus.classificacao.dto.response.ClassificationReasonResponse;
import com.venus.classificacao.service.explanation.Reason;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class ClassificationMapperTest {

    private final ClassificationMapper classificationMapper = Mappers.getMapper(ClassificationMapper.class);

    @Test
    void reasonResponseKeepsIngredientNames() {
        Reason allergyReason = Reason.ofAllergy("Tem LIMONENO e LINALOL.", List.of("LIMONENO", "LINALOL"));

        ClassificationReasonResponse response = classificationMapper.toReasonResponse(allergyReason);

        assertThat(response.ingredientNames()).containsExactly("LIMONENO", "LINALOL");
    }
}
