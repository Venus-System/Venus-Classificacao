package com.venus.classificacao.service.question.preference;

import static com.venus.classificacao.service.product.ProductSnapshot.ClaimStatus.DECLARED;
import static com.venus.classificacao.service.product.ProductSnapshot.ClaimStatus.NOT_DECLARED;
import static com.venus.classificacao.service.product.ProductSnapshot.ClaimStatus.UNKNOWN;
import static org.assertj.core.api.Assertions.assertThat;

import com.venus.classificacao.service.product.ProductSnapshot;
import com.venus.classificacao.service.profile.ProfileSnapshot;
import com.venus.classificacao.service.question.QuestionScore;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BrandClaimStrategiesTest {

    private final PreferVeganStrategy vegan = new PreferVeganStrategy();
    private final PreferCrueltyFreeStrategy crueltyFree = new PreferCrueltyFreeStrategy();

    @Test
    void declaredVeganGetsPoints() {
        QuestionScore score = vegan.score(productWith(DECLARED, UNKNOWN), profilePreferring(true, false));

        assertThat(score.applicable()).isTrue();
        assertThat(score.earnedPoints()).isEqualTo(3);
    }

    @Test
    void undeclaredVeganGetsZero() {
        QuestionScore score = vegan.score(productWith(NOT_DECLARED, UNKNOWN), profilePreferring(true, false));

        assertThat(score.applicable()).isTrue();
        assertThat(score.earnedPoints()).isZero();
    }

    @Test
    void unknownVeganLeftOut() {
        QuestionScore score = vegan.score(productWith(UNKNOWN, UNKNOWN), profilePreferring(true, false));

        assertThat(score.applicable()).isFalse();
        assertThat(score.preferenceNotes())
                .containsExactly("A marca não informa se é vegana, então essa preferência não entrou na nota.");
    }

    @Test
    void unknownCrueltyFreeLeftOut() {
        QuestionScore score = crueltyFree.score(productWith(UNKNOWN, UNKNOWN), profilePreferring(false, true));

        assertThat(score.applicable()).isFalse();
        assertThat(score.preferenceNotes())
                .containsExactly("A marca não informa se testa em animais, então essa preferência não entrou na nota.");
    }

    @Test
    void declaredVeganSaysItAddedPoints() {
        QuestionScore score = vegan.score(productWith(DECLARED, UNKNOWN), profilePreferring(true, false));

        assertThat(score.preferenceNotes()).containsExactly(
                "A marca diz que é vegana, como você prefere, e isso somou pontos na nota.");
    }

    @Test
    void undeclaredVeganSaysItAddedNothing() {
        QuestionScore score = vegan.score(productWith(NOT_DECLARED, UNKNOWN), profilePreferring(true, false));

        assertThat(score.preferenceNotes()).containsExactly("A marca não diz que é vegana, e você prefere produtos "
                + "veganos, por isso essa parte não somou pontos.");
    }

    @Test
    void declaredCrueltyFreeSaysItAddedPoints() {
        QuestionScore score = crueltyFree.score(productWith(UNKNOWN, DECLARED), profilePreferring(false, true));

        assertThat(score.preferenceNotes()).containsExactly(
                "A marca diz que não testa em animais, como você prefere, e isso somou pontos na nota.");
    }

    @Test
    void undeclaredCrueltyFreeSaysItAddedNothing() {
        QuestionScore score = crueltyFree.score(productWith(UNKNOWN, NOT_DECLARED), profilePreferring(false, true));

        assertThat(score.preferenceNotes()).containsExactly("A marca não diz que é livre de testes em animais, e você "
                + "prefere produtos cruelty-free, por isso essa parte não somou pontos.");
    }

    private static ProductSnapshot productWith(ProductSnapshot.ClaimStatus veganClaim,
            ProductSnapshot.ClaimStatus crueltyFreeClaim) {
        return new ProductSnapshot(1L, new ProductSnapshot.BrandClaims(veganClaim, crueltyFreeClaim), List.of(),
                Optional.empty(), 0, Map.of(), Set.of(), List.of());
    }

    private static ProfileSnapshot profilePreferring(boolean prefersVegan, boolean prefersCrueltyFree) {
        ProfileSnapshot.Answers answers = new ProfileSnapshot.Answers(null, null, false, false, false, false, false,
                false, false, prefersVegan, prefersCrueltyFree, false, false, false);
        return new ProfileSnapshot(1L, answers, Map.of(), List.of(), List.of());
    }
}
