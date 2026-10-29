package com.venus.classificacao.service.question;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ProfileTagLabelsTest {

    @Test
    void questionnaireTagsUseTheAnswerWords() {
        assertThat(ProfileTagLabels.labelOf("reativa", "Reativa")).isEqualTo("pele sensível");
        assertThat(ProfileTagLabels.labelOf("pele-acneica", "Pele Acneica")).isEqualTo("tendência a acne");
        assertThat(ProfileTagLabels.labelOf("controle-de-oleosidade", "Controle de Oleosidade"))
                .isEqualTo("pele oleosa");
        assertThat(ProfileTagLabels.labelOf("caspa", "Caspa")).isEqualTo("couro cabeludo com caspa");
    }

    @Test
    void otherTagsKeepTheirName() {
        assertThat(ProfileTagLabels.labelOf("sem-oleo", "Sem Óleo")).isEqualTo("Sem Óleo");
    }
}
