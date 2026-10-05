package com.venus.classificacao.service.verdict;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.venus.classificacao.service.quality.QualityResult;
import com.venus.classificacao.service.question.ProfileResult;
import com.venus.classificacao.service.question.QuestionScore;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CombinedScoreTest {

    @Test
    void qualityIsWorthThirtyFiveAndProfileSixtyFive() {
        CombinedScore combinedScore = CombinedScore.of(quality(80), profileWithRatio(0.5));

        assertThat(combinedScore.qualityPoints()).isCloseTo(28.0, within(1e-9));
        assertThat(combinedScore.profilePoints().getAsDouble()).isCloseTo(32.5, within(1e-9));
        assertThat(combinedScore.total()).isCloseTo(60.5, within(1e-9));
    }

    @Test
    void withoutProfileQuestionsTheTotalIsTheQualityAlone() {
        CombinedScore combinedScore = CombinedScore.of(quality(74), new ProfileResult(List.of()));

        assertThat(combinedScore.profilePoints()).isEmpty();
        assertThat(combinedScore.total()).isEqualTo(74.0);
    }

    private static QualityResult quality(double qualityScore) {
        return new QualityResult(Map.of(), qualityScore);
    }

    private static ProfileResult profileWithRatio(double ratio) {
        return new ProfileResult(List.of(new QuestionScore("pergunta", 10, true, 10 * ratio, List.of(), List.of())));
    }
}
