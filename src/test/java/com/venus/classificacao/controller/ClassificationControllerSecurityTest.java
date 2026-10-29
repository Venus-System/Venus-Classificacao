package com.venus.classificacao.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.venus.classificacao.config.SecurityConfig;
import com.venus.classificacao.entity.enums.AdminRole;
import com.venus.classificacao.entity.enums.UserStatus;
import com.venus.classificacao.entity.user.User;
import com.venus.classificacao.repository.user.UserRepository;
import com.venus.classificacao.security.AdminJwtAuthenticationConverter;
import com.venus.classificacao.security.AdminTokenService;
import com.venus.classificacao.security.CurrentUserProvider;
import com.venus.classificacao.security.OwnershipGuard;
import com.venus.classificacao.security.SecurityErrorHandler;
import com.venus.classificacao.security.SecurityRoles;
import com.venus.classificacao.service.ClassificationService;
import com.venus.classificacao.service.SavedClassificationService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(controllers = ClassificationController.class, properties = {
        "venus.security.admin-token.secret=segredo-de-teste-com-mais-de-32-bytes!!",
        "venus.security.firebase.project-id=venus-81bac",
        "server.port=0"
})
@Import({SecurityConfig.class, AdminTokenService.class, CurrentUserProvider.class, OwnershipGuard.class,
        SecurityErrorHandler.class})
class ClassificationControllerSecurityTest {

    private static final long ANA_ID = 10L;
    private static final long BIA_ID = 11L;
    private static final long VERSION_ID = 118L;
    private static final long PRODUCT_ID = 7L;
    private static final String ANA_UID = "uid-ana";
    private static final String BY_VERSION = "/api/classifications/user/{userId}/product-version/{versionId}";
    private static final String BY_PRODUCT = "/api/classifications/user/{userId}/product/{productId}";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ClassificationService classificationService;

    @MockBean
    private SavedClassificationService savedClassificationService;

    @MockBean
    private UserRepository userRepository;

    @Test
    void withoutTokenReturns401InTheErrorFormat() throws Exception {
        mockMvc.perform(get(BY_VERSION, ANA_ID, VERSION_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value(SecurityErrorHandler.MISSING_TOKEN_MESSAGE));
    }

    @Test
    void malformedTokenReturns401() throws Exception {
        mockMvc.perform(get(BY_VERSION, ANA_ID, VERSION_ID).header(HttpHeaders.AUTHORIZATION, "Bearer abc"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(SecurityErrorHandler.INVALID_TOKEN_MESSAGE));
    }

    @Test
    void ownerReadsOwnClassification() throws Exception {
        registeredAna(UserStatus.ACTIVE);

        mockMvc.perform(get(BY_VERSION, ANA_ID, VERSION_ID).with(appUser()))
                .andExpect(status().isOk());
        mockMvc.perform(get(BY_PRODUCT, ANA_ID, PRODUCT_ID).with(appUser()))
                .andExpect(status().isOk());
    }

    @Test
    void anotherUsersClassificationReturns403WithoutReadingIt() throws Exception {
        registeredAna(UserStatus.ACTIVE);

        mockMvc.perform(get(BY_VERSION, BIA_ID, VERSION_ID).with(appUser()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(SecurityErrorHandler.ACCESS_DENIED_MESSAGE));
        mockMvc.perform(get(BY_PRODUCT, BIA_ID, PRODUCT_ID).with(appUser()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(savedClassificationService);
    }

    @Test
    void postForAnotherUserReturns403WithoutClassifying() throws Exception {
        registeredAna(UserStatus.ACTIVE);

        mockMvc.perform(post("/api/classifications").with(appUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\": " + BIA_ID + ", \"productVersionId\": " + VERSION_ID + "}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(classificationService);
    }

    @Test
    void blockedUserReturns403() throws Exception {
        registeredAna(UserStatus.BLOCKED);

        mockMvc.perform(get(BY_VERSION, ANA_ID, VERSION_ID).with(appUser()))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminReadsAnyUsersClassification() throws Exception {
        mockMvc.perform(get(BY_VERSION, BIA_ID, VERSION_ID).with(admin(AdminRole.ADMIN)))
                .andExpect(status().isOk());
    }

    @Test
    void moderatorCannotReadClassifications() throws Exception {
        mockMvc.perform(get(BY_VERSION, BIA_ID, VERSION_ID).with(admin(AdminRole.MODERATOR)))
                .andExpect(status().isForbidden());
    }

    @Test
    void textInTheUserIdReturns400() throws Exception {
        registeredAna(UserStatus.ACTIVE);

        mockMvc.perform(get(BY_VERSION, "abc", VERSION_ID).with(appUser()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void swaggerDoesNotAskForToken() throws Exception {
        int status = mockMvc.perform(get("/v3/api-docs")).andReturn().getResponse().getStatus();

        assertThat(status).isNotEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    private void registeredAna(UserStatus status) {
        User user = new User();
        user.setId(ANA_ID);
        user.setFirebaseUid(ANA_UID);
        user.setStatus(status);
        when(userRepository.findByFirebaseUid(ANA_UID)).thenReturn(Optional.of(user));
    }

    private static RequestPostProcessor appUser() {
        return jwt().jwt(token -> token.subject(ANA_UID)).authorities(new SimpleGrantedAuthority(SecurityRoles.USER));
    }

    private static RequestPostProcessor admin(AdminRole role) {
        return jwt().jwt(token -> token.subject("3")).authorities(AdminJwtAuthenticationConverter.authoritiesFor(role));
    }
}
