package com.venus.classificacao.service.scoring;

import com.venus.classificacao.entity.scoring.ScoringModel;
import com.venus.classificacao.exception.ClassificationErrorCode;
import com.venus.classificacao.exception.DataAccessFailureTranslator;
import com.venus.classificacao.exception.DataIntegrityViolationTranslator;
import com.venus.classificacao.exception.ResourceNotFoundException;
import com.venus.classificacao.repository.scoring.ScoringModelRepository;
import java.util.Optional;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
public class ScoringModelLoader {

    private static final Logger log = LoggerFactory.getLogger(ScoringModelLoader.class);

    private final ScoringModelRepository scoringModelRepository;

    public ScoringModelLoader(ScoringModelRepository scoringModelRepository) {
        this.scoringModelRepository = scoringModelRepository;
    }

    public ScoringModel load(Long scoringModelId) {
        if (scoringModelId == null) {
            return getActiveModelOrThrow();
        }
        return getModelOrThrow(scoringModelId);
    }

    private ScoringModel getActiveModelOrThrow() {
        Optional<ScoringModel> activeModel = executeOrFail(scoringModelRepository::findFirstByIsActiveTrueOrderByIdDesc,
                "Falha ao consultar modelo de scoring ativo");

        return activeModel.orElseThrow(() -> new ResourceNotFoundException(ClassificationErrorCode.NO_ACTIVE_SCORING_MODEL,
                "Nenhum modelo de score ativo"));
    }

    private ScoringModel getModelOrThrow(Long scoringModelId) {
        Optional<ScoringModel> scoringModel = executeOrFail(() -> scoringModelRepository.findById(scoringModelId),
                "Falha ao consultar modelo de scoring no banco de dados");

        return scoringModel.orElseThrow(() -> new ResourceNotFoundException(ClassificationErrorCode.SCORING_MODEL_NOT_FOUND,
                "Modelo de score nao encontrado com id " + scoringModelId));
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
