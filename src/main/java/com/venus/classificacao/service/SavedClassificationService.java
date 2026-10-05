package com.venus.classificacao.service;

import com.venus.classificacao.dto.response.ClassificationResponse;
import com.venus.classificacao.entity.scoring.ScoringModel;
import com.venus.classificacao.exception.ClassificationErrorCode;
import com.venus.classificacao.exception.DataAccessFailureTranslator;
import com.venus.classificacao.exception.DataIntegrityViolationTranslator;
import com.venus.classificacao.exception.ResourceNotFoundException;
import com.venus.classificacao.mapper.ClassificationMapper;
import com.venus.classificacao.repository.product.ProductVersionRepository;
import com.venus.classificacao.service.recording.SavedClassificationLoader;
import com.venus.classificacao.service.scoring.ScoringModelLoader;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SavedClassificationService {

    private static final Logger log = LoggerFactory.getLogger(SavedClassificationService.class);

    private final ScoringModelLoader scoringModelLoader;
    private final SavedClassificationLoader savedClassificationLoader;
    private final ProductVersionRepository productVersionRepository;
    private final ClassificationMapper classificationMapper;

    public SavedClassificationService(ScoringModelLoader scoringModelLoader,
            SavedClassificationLoader savedClassificationLoader,
            ProductVersionRepository productVersionRepository,
            ClassificationMapper classificationMapper) {
        this.scoringModelLoader = scoringModelLoader;
        this.savedClassificationLoader = savedClassificationLoader;
        this.productVersionRepository = productVersionRepository;
        this.classificationMapper = classificationMapper;
    }

    @Transactional(readOnly = true)
    public ClassificationResponse findLatestByVersion(Long userId, Long productVersionId, Long scoringModelId) {
        ScoringModel scoringModel = scoringModelLoader.load(scoringModelId);
        ClassificationTarget target = new ClassificationTarget(userId, productVersionId, scoringModel.getId());

        ClassificationResult savedResult = savedClassificationLoader.load(target)
                .orElseThrow(() -> analysisNotFound(target));
        return classificationMapper.toResponse(savedResult);
    }

    private ResourceNotFoundException analysisNotFound(ClassificationTarget target) {
        boolean versionExists = executeOrFail(() -> productVersionRepository.existsById(target.productVersionId()),
                "Falha ao consultar versao de produto no banco de dados");
        if (!versionExists) {
            return new ResourceNotFoundException(ClassificationErrorCode.VERSION_NOT_FOUND,
                    "Versao de produto nao encontrada com id " + target.productVersionId());
        }
        return new ResourceNotFoundException(ClassificationErrorCode.ANALYSIS_NOT_FOUND,
                "Nenhuma analise encontrada para o usuario " + target.userId() + " na versao "
                        + target.productVersionId() + " com o modelo " + target.scoringModelId());
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
