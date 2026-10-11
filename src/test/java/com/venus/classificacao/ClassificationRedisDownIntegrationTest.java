package com.venus.classificacao;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.venus.classificacao.security.SecurityRoles;
import java.io.IOException;
import java.net.ServerSocket;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

@SpringBootTest(properties = {
        "server.port=0",
        "venus.security.admin-token.secret=segredo-de-teste-com-mais-de-32-bytes!!",
        "venus.security.firebase.project-id=venus-81bac",
        "venus.scoring.base-model-id=1",
        "spring.data.redis.host=localhost"
})
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class ClassificationRedisDownIntegrationTest {

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

    @DynamicPropertySource
    static void closedRedisPort(DynamicPropertyRegistry registry) throws IOException {
        int closedPort = freePort();
        registry.add("spring.data.redis.port", () -> closedPort);
    }

    @Test
    void redisDownUsesDatabase() throws Exception {
        long userId = insertAcneProneUser("uid-redis-fora");
        long versionId = versionIdOf("niacinamida-v1");
        String body = "{\"userId\": " + userId + ", \"productVersionId\": " + versionId + "}";

        mockMvc.perform(post("/api/classifications").with(appUser("uid-redis-fora"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.finalScore").value(34));

        mockMvc.perform(get(BY_VERSION, userId, versionId).with(appUser("uid-redis-fora")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.finalScore").value(34));
    }

    private static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private long insertAcneProneUser(String firebaseUid) {
        long userId = jdbcTemplate.queryForObject("INSERT INTO venus.users (firebase_uid, name) "
                + "VALUES (?, 'Usuario Teste') RETURNING user_id", Long.class, firebaseUid);
        jdbcTemplate.update("INSERT INTO venus.user_profiles (fk_user_id, acne_prone) VALUES (?, TRUE)", userId);
        return userId;
    }

    private long versionIdOf(String formulaSignature) {
        return jdbcTemplate.queryForObject("SELECT product_version_id FROM venus.product_versions "
                + "WHERE formula_signature = ?", Long.class, formulaSignature);
    }

    private static RequestPostProcessor appUser(String firebaseUid) {
        return jwt().jwt(token -> token.subject(firebaseUid)).authorities(new SimpleGrantedAuthority(SecurityRoles.USER));
    }
}
