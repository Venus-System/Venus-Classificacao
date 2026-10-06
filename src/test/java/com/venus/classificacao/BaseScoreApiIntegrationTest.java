package com.venus.classificacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.venus.classificacao.entity.enums.AdminRole;
import com.venus.classificacao.security.AdminJwtAuthenticationConverter;
import java.time.OffsetDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

@SpringBootTest(properties = {
        "server.port=0",
        "venus.security.admin-token.secret=segredo-de-teste-com-mais-de-32-bytes!!",
        "venus.security.firebase.project-id=venus-81bac",
        "venus.scoring.base-model-id=1"
})
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class BaseScoreApiIntegrationTest {

    private static final String BY_VERSION = "/api/base-scores/product-version/{versionId}";
    private static final String ALL = "/api/base-scores";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withUrlParam("currentSchema", "venus")
            .withUrlParam("stringtype", "unspecified")
            .withCopyFileToContainer(MountableFile.forClasspathResource("venus-banco/00_schema.sql"),
                    "/docker-entrypoint-initdb.d/00_schema.sql")
            .withCopyFileToContainer(MountableFile.forClasspathResource("venus-banco/01_users_email_password_hash.sql"),
                    "/docker-entrypoint-initdb.d/01_users_email_password_hash.sql")
            .withCopyFileToContainer(MountableFile.forClasspathResource("venus-banco/02_seed_classificacao.sql"),
                    "/docker-entrypoint-initdb.d/02_seed_classificacao.sql");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void postByVersionCalculatesAndRecordsTheBaseScore() throws Exception {
        long versionId = versionIdOf("niacinamida-v1");

        mockMvc.perform(post(BY_VERSION, versionId).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productVersionId").value(versionId))
                .andExpect(jsonPath("$.scoringModelId").value(1))
                .andExpect(jsonPath("$.qualityScore").value(50))
                .andExpect(jsonPath("$.healthScore").value(nullValue()))
                .andExpect(jsonPath("$.environmentalScore").value(nullValue()))
                .andExpect(jsonPath("$.ethicalScore").value(50))
                .andExpect(jsonPath("$.performanceScore").value(nullValue()))
                .andExpect(jsonPath("$.ingredientCount").value(1))
                .andExpect(jsonPath("$.unevaluatedIngredientCount").value(1))
                .andExpect(jsonPath("$.calculatedAt").exists());

        assertThat(productScoreOf(versionId))
                .containsEntry("overall_score", 50)
                .containsEntry("health_score", null)
                .containsEntry("environmental_score", null)
                .containsEntry("ethical_score", 50)
                .containsEntry("performance_score", null);
    }

    @Test
    void postByVersionAcceptsAVersionThatIsNotCurrent() throws Exception {
        long versionId = versionIdOf("reformulado-v1");

        mockMvc.perform(post(BY_VERSION, versionId).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productVersionId").value(versionId));
    }

    @Test
    void postByVersionUnderReviewReturns422() throws Exception {
        mockMvc.perform(post(BY_VERSION, versionIdOf("em-revisao-v1")).with(admin()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VERSION_UNDER_REVIEW"));
    }

    @Test
    void postByVersionWithoutIngredientsReturns422() throws Exception {
        mockMvc.perform(post(BY_VERSION, versionIdOf("sem-ingrediente-v1")).with(admin()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("NO_INGREDIENTS"));
    }

    @Test
    void postByMissingVersionReturns404VersionNotFound() throws Exception {
        mockMvc.perform(post(BY_VERSION, 999999L).with(admin()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VERSION_NOT_FOUND"));
    }

    @Test
    void postWithUnknownScoringModelReturns404ScoringModelNotFound() throws Exception {
        mockMvc.perform(post(BY_VERSION, versionIdOf("niacinamida-v1")).param("scoringModelId", "999").with(admin()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SCORING_MODEL_NOT_FOUND"));
    }

    @Test
    void postAllRecalculatesTheCurrentVersionsAndListsTheSkippedOnes() throws Exception {
        long oldVersionId = versionIdOf("reformulado-v1");
        mockMvc.perform(post(BY_VERSION, oldVersionId).with(admin())).andExpect(status().isOk());
        OffsetDateTime oldVersionCalculatedAt = calculatedAtOf(oldVersionId);

        postAll()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scoringModelId").value(1))
                .andExpect(jsonPath("$.totalVersions").value(4))
                .andExpect(jsonPath("$.recalculatedCount").value(2))
                .andExpect(jsonPath("$.skipped.length()").value(2))
                .andExpect(jsonPath(skippedCodeOf("em-revisao-v1")).value(contains("VERSION_UNDER_REVIEW")))
                .andExpect(jsonPath(skippedCodeOf("sem-ingrediente-v1")).value(contains("NO_INGREDIENTS")))
                .andExpect(jsonPath("$.processingTimeMs").exists());

        assertThat(productScoreOf(versionIdOf("reformulado-v2"))).containsEntry("overall_score", 50);
        assertThat(calculatedAtOf(oldVersionId)).isEqualTo(oldVersionCalculatedAt);
    }

    @Test
    void swaggerDocumentsTheBaseScoreRoutesWith200AndTheirErrors() throws Exception {
        String byVersion = "$.paths['/api/base-scores/product-version/{versionId}'].post.responses";
        String all = "$.paths['/api/base-scores'].post.responses";

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(byVersion + "['200']").exists())
                .andExpect(jsonPath(byVersion + "['201']").doesNotExist())
                .andExpect(jsonPath(byVersion + "['404'].description").value(
                        "Não achou a versão do produto ou o modelo de score. O campo code diz qual."))
                .andExpect(jsonPath(byVersion + "['422']").exists())
                .andExpect(jsonPath(all + "['200']").exists())
                .andExpect(jsonPath(all + "['201']").doesNotExist())
                .andExpect(jsonPath(all + "['404'].description").value(
                        "Não achou o modelo de score pedido, ou não existe modelo ativo. O campo code diz qual."));
    }

    private ResultActions postAll() throws Exception {
        return mockMvc.perform(post(ALL).with(admin()));
    }

    private String skippedCodeOf(String formulaSignature) {
        return "$.skipped[?(@.productVersionId == " + versionIdOf(formulaSignature) + ")].code";
    }

    private long versionIdOf(String formulaSignature) {
        return jdbcTemplate.queryForObject("SELECT product_version_id FROM venus.product_versions "
                + "WHERE formula_signature = ?", Long.class, formulaSignature);
    }

    private Map<String, Object> productScoreOf(long versionId) {
        return jdbcTemplate.queryForMap("SELECT overall_score, health_score, environmental_score, ethical_score, "
                + "performance_score FROM venus.product_scores WHERE fk_product_version_id = ? AND fk_scoring_model_id = 1",
                versionId);
    }

    private OffsetDateTime calculatedAtOf(long versionId) {
        return jdbcTemplate.queryForObject("SELECT calculated_at FROM venus.product_scores "
                + "WHERE fk_product_version_id = ? AND fk_scoring_model_id = 1", OffsetDateTime.class, versionId);
    }

    private static RequestPostProcessor admin() {
        return jwt().jwt(token -> token.subject("3")).authorities(AdminJwtAuthenticationConverter.authoritiesFor(AdminRole.ADMIN));
    }
}
