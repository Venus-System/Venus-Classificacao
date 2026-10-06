package com.venus.classificacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.venus.classificacao.security.SecurityRoles;
import java.net.URI;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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
class ClassificationApiIntegrationTest {

    private static final String NIACINAMIDE_REASON = "Niacinamida ajuda a pele com acne.";
    private static final String BY_PRODUCT = "/api/classifications/user/{userId}/product/{productId}";
    private static final String BY_VERSION = "/api/classifications/user/{userId}/product-version/{versionId}";

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
    void postCalculatesTheScoreAndRecordsTheAnalysis() throws Exception {
        long userId = insertAcneProneUser("uid-post-grava");
        long versionId = versionIdOf("niacinamida-v1");

        postClassification("uid-post-grava", userId, versionId)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith(
                        "/api/classifications/user/" + userId + "/product-version/" + versionId + "?scoringModelId=1")))
                .andExpect(jsonPath("$.finalScore").value(34))
                .andExpect(jsonPath("$.recommendationLevel").value("NOT_RECOMMENDED"))
                .andExpect(jsonPath("$.riskLevel").value("MEDIUM"))
                .andExpect(jsonPath("$.compatibilityPercentage").value(25.0))
                .andExpect(jsonPath("$.ingredientCount").value(1))
                .andExpect(jsonPath("$.unevaluatedIngredientCount").value(1))
                .andExpect(jsonPath("$.breakdown.qualityScore").value(50))
                .andExpect(jsonPath("$.breakdown.qualityPoints").value(17.5))
                .andExpect(jsonPath("$.breakdown.profilePoints").value(16.3))
                .andExpect(jsonPath("$.breakdown.healthScore").value(nullValue()))
                .andExpect(jsonPath("$.breakdown.ethicalScore").value(50))
                .andExpect(jsonPath("$.breakdown.performanceScore").value(nullValue()))
                .andExpect(jsonPath("$.reasons[0].text").value(NIACINAMIDE_REASON))
                .andExpect(jsonPath("$.summary").value("Nota 34 de 100 - Não recomendado. " + NIACINAMIDE_REASON));

        assertThat(countByUser("analysis_results", userId)).isEqualTo(1);
        assertThat(countByUser("personalized_scores", userId)).isEqualTo(1);
        assertThat(countRuleEvaluations(userId)).isEqualTo(1);
        assertThat(productScoreOf(versionId))
                .containsEntry("overall_score", 50)
                .containsEntry("health_score", null)
                .containsEntry("environmental_score", null)
                .containsEntry("ethical_score", 50)
                .containsEntry("performance_score", null);
    }

    @Test
    void secondPostForTheSameProductKeepsTheHistoryAndUpdatesThePersonalizedScore() throws Exception {
        long userId = insertAcneProneUser("uid-post-duas-vezes");
        long versionId = versionIdOf("niacinamida-v1");

        postClassification("uid-post-duas-vezes", userId, versionId).andExpect(status().isCreated());
        postClassification("uid-post-duas-vezes", userId, versionId).andExpect(status().isCreated());

        assertThat(countByUser("analysis_results", userId)).isEqualTo(2);
        assertThat(countByUser("personalized_scores", userId)).isEqualTo(1);
        assertThat(personalizedScoreAnalysisId(userId)).isEqualTo(latestAnalysisId(userId));
    }

    @Test
    void postOnVersionUnderReviewReturns422() throws Exception {
        long userId = insertAcneProneUser("uid-em-revisao");

        postClassification("uid-em-revisao", userId, versionIdOf("em-revisao-v1"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VERSION_UNDER_REVIEW"));
    }

    @Test
    void postOnVersionWithoutIngredientsReturns422() throws Exception {
        long userId = insertAcneProneUser("uid-sem-ingrediente");

        postClassification("uid-sem-ingrediente", userId, versionIdOf("sem-ingrediente-v1"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("NO_INGREDIENTS"));
    }

    @Test
    void postForUserWithoutProfileReturns404() throws Exception {
        long userId = insertUser("uid-sem-perfil");

        postClassification("uid-sem-perfil", userId, versionIdOf("niacinamida-v1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROFILE_NOT_FOUND"));
    }

    @Test
    void getReturnsTheSavedAnalysisThatThePostLocationPointsTo() throws Exception {
        long userId = insertAcneProneUser("uid-get-versao");
        long versionId = versionIdOf("niacinamida-v1");
        String location = postClassification("uid-get-versao", userId, versionId)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");

        mockMvc.perform(get(URI.create(location)).with(appUser("uid-get-versao")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.productVersionId").value(versionId))
                .andExpect(jsonPath("$.scoringModelId").value(1))
                .andExpect(jsonPath("$.finalScore").value(34))
                .andExpect(jsonPath("$.recommendationLevel").value("NOT_RECOMMENDED"))
                .andExpect(jsonPath("$.riskLevel").value("MEDIUM"))
                .andExpect(jsonPath("$.compatibilityPercentage").value(25.0))
                .andExpect(jsonPath("$.ingredientCount").value(nullValue()))
                .andExpect(jsonPath("$.unevaluatedIngredientCount").value(nullValue()))
                .andExpect(jsonPath("$.breakdown.qualityScore").value(nullValue()))
                .andExpect(jsonPath("$.breakdown.qualityPoints").value(nullValue()))
                .andExpect(jsonPath("$.breakdown.profilePoints").value(nullValue()))
                .andExpect(jsonPath("$.breakdown.ethicalScore").value(50))
                .andExpect(jsonPath("$.breakdown.performanceScore").value(nullValue()))
                .andExpect(jsonPath("$.reasons.length()").value(1))
                .andExpect(jsonPath("$.reasons[0].source").value("INGREDIENT_RULE"))
                .andExpect(jsonPath("$.reasons[0].text").value(NIACINAMIDE_REASON))
                .andExpect(jsonPath("$.reasons[0].scoreDelta").value(5))
                .andExpect(jsonPath("$.reasons[0].impact").value(5.0))
                .andExpect(jsonPath("$.reasons[0].ingredientName").value("Niacinamida"))
                .andExpect(jsonPath("$.reasons[0].profileTagSlug").value("pele-acneica"))
                .andExpect(jsonPath("$.summary").value("Nota 34 de 100 - Não recomendado. " + NIACINAMIDE_REASON))
                .andExpect(jsonPath("$.calculatedAt").exists());
    }

    @Test
    void getWithoutScoringModelUsesTheActiveModel() throws Exception {
        long userId = insertAcneProneUser("uid-get-modelo-ativo");
        long versionId = versionIdOf("niacinamida-v1");
        postClassification("uid-get-modelo-ativo", userId, versionId).andExpect(status().isCreated());

        mockMvc.perform(get(BY_VERSION, userId, versionId).with(appUser("uid-get-modelo-ativo")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scoringModelId").value(1));
    }

    @Test
    void getWithoutSavedAnalysisReturns404AnalysisNotFound() throws Exception {
        long userId = insertAcneProneUser("uid-nunca-analisou");

        mockMvc.perform(get(BY_VERSION, userId, versionIdOf("niacinamida-v1")).with(appUser("uid-nunca-analisou")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ANALYSIS_NOT_FOUND"));
    }

    @Test
    void getForMissingVersionReturns404VersionNotFound() throws Exception {
        long userId = insertAcneProneUser("uid-versao-inexistente");

        mockMvc.perform(get(BY_VERSION, userId, 999999L).with(appUser("uid-versao-inexistente")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VERSION_NOT_FOUND"));
    }

    @Test
    void getWithUnknownScoringModelReturns404ScoringModelNotFound() throws Exception {
        long userId = insertAcneProneUser("uid-modelo-inexistente");

        mockMvc.perform(get(BY_VERSION, userId, versionIdOf("niacinamida-v1"))
                        .param("scoringModelId", "999")
                        .with(appUser("uid-modelo-inexistente")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SCORING_MODEL_NOT_FOUND"));
    }

    @Test
    void getByProductReturnsTheAnalysisOfTheCurrentVersion() throws Exception {
        long userId = insertAcneProneUser("uid-get-produto");
        long versionId = versionIdOf("niacinamida-v1");
        postClassification("uid-get-produto", userId, versionId).andExpect(status().isCreated());

        mockMvc.perform(get(BY_PRODUCT, userId, productIdOf("serum-niacinamida")).with(appUser("uid-get-produto")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productVersionId").value(versionId))
                .andExpect(jsonPath("$.finalScore").value(34));
    }

    @Test
    void getByProductIgnoresTheAnalysisOfAnOldVersion() throws Exception {
        long userId = insertAcneProneUser("uid-versao-antiga");
        postClassification("uid-versao-antiga", userId, versionIdOf("reformulado-v1")).andExpect(status().isCreated());

        mockMvc.perform(get(BY_PRODUCT, userId, productIdOf("serum-reformulado")).with(appUser("uid-versao-antiga")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ANALYSIS_NOT_FOUND"));
    }

    @Test
    void getForMissingProductReturns404ProductNotFound() throws Exception {
        long userId = insertAcneProneUser("uid-produto-inexistente");

        mockMvc.perform(get(BY_PRODUCT, userId, 999999L).with(appUser("uid-produto-inexistente")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
    }

    private long productIdOf(String slug) {
        return jdbcTemplate.queryForObject("SELECT product_id FROM venus.products WHERE slug = ?", Long.class, slug);
    }

    private ResultActions postClassification(String firebaseUid, long userId, long versionId) throws Exception {
        String body = "{\"userId\": " + userId + ", \"productVersionId\": " + versionId + "}";
        return mockMvc.perform(post("/api/classifications").with(appUser(firebaseUid))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private long insertUser(String firebaseUid) {
        return jdbcTemplate.queryForObject("INSERT INTO venus.users (firebase_uid, name) VALUES (?, 'Usuario Teste') "
                + "RETURNING user_id", Long.class, firebaseUid);
    }

    private long insertAcneProneUser(String firebaseUid) {
        long userId = insertUser(firebaseUid);
        jdbcTemplate.update("INSERT INTO venus.user_profiles (fk_user_id, acne_prone) VALUES (?, TRUE)", userId);
        return userId;
    }

    private long versionIdOf(String formulaSignature) {
        return jdbcTemplate.queryForObject("SELECT product_version_id FROM venus.product_versions "
                + "WHERE formula_signature = ?", Long.class, formulaSignature);
    }

    private int countByUser(String table, long userId) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM venus." + table + " WHERE fk_user_id = ?",
                Integer.class, userId);
    }

    private int countRuleEvaluations(long userId) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM venus.rule_evaluations re "
                + "JOIN venus.analysis_results ar ON ar.analysis_result_id = re.fk_analysis_result_id "
                + "WHERE ar.fk_user_id = ?", Integer.class, userId);
    }

    private long personalizedScoreAnalysisId(long userId) {
        return jdbcTemplate.queryForObject("SELECT fk_analysis_result_id FROM venus.personalized_scores "
                + "WHERE fk_user_id = ?", Long.class, userId);
    }

    private Map<String, Object> productScoreOf(long versionId) {
        return jdbcTemplate.queryForMap("SELECT overall_score, health_score, environmental_score, ethical_score, "
                + "performance_score FROM venus.product_scores WHERE fk_product_version_id = ? AND fk_scoring_model_id = 1",
                versionId);
    }

    private long latestAnalysisId(long userId) {
        return jdbcTemplate.queryForObject("SELECT MAX(analysis_result_id) FROM venus.analysis_results "
                + "WHERE fk_user_id = ?", Long.class, userId);
    }

    private static RequestPostProcessor appUser(String firebaseUid) {
        return jwt().jwt(token -> token.subject(firebaseUid)).authorities(new SimpleGrantedAuthority(SecurityRoles.USER));
    }
}
