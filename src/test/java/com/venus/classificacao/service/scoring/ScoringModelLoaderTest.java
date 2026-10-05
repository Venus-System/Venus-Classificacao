package com.venus.classificacao.service.scoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.venus.classificacao.entity.scoring.ScoringModel;
import com.venus.classificacao.exception.ClassificationErrorCode;
import com.venus.classificacao.exception.ResourceNotFoundException;
import com.venus.classificacao.repository.scoring.ScoringModelRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScoringModelLoaderTest {

    @Mock
    private ScoringModelRepository scoringModelRepository;

    private ScoringModelLoader loader;

    @BeforeEach
    void setUp() {
        loader = new ScoringModelLoader(scoringModelRepository);
    }

    @Test
    void withoutIdLoadsTheActiveModelWithTheHighestId() {
        ScoringModel activeModel = model(15L);
        when(scoringModelRepository.findFirstByIsActiveTrueOrderByIdDesc()).thenReturn(Optional.of(activeModel));

        assertThat(loader.load(null)).isSameAs(activeModel);
        verify(scoringModelRepository, never()).findById(any());
    }

    @Test
    void withoutIdAndWithoutActiveModelThrowsNoActiveScoringModel() {
        when(scoringModelRepository.findFirstByIsActiveTrueOrderByIdDesc()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loader.load(null))
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting("code")
                .isEqualTo(ClassificationErrorCode.NO_ACTIVE_SCORING_MODEL);
    }

    @Test
    void withIdLoadsThatModelEvenWhenItIsNotTheActiveOne() {
        ScoringModel oldModel = model(1L);
        when(scoringModelRepository.findById(1L)).thenReturn(Optional.of(oldModel));

        assertThat(loader.load(1L)).isSameAs(oldModel);
        verify(scoringModelRepository, never()).findFirstByIsActiveTrueOrderByIdDesc();
    }

    @Test
    void unknownIdThrowsScoringModelNotFound() {
        when(scoringModelRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loader.load(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting("code")
                .isEqualTo(ClassificationErrorCode.SCORING_MODEL_NOT_FOUND);
    }

    private ScoringModel model(Long id) {
        ScoringModel scoringModel = new ScoringModel();
        scoringModel.setId(id);
        return scoringModel;
    }
}
