package com.venus.classificacao.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.venus.classificacao.config.SecurityConfig;
import com.venus.classificacao.entity.enums.AdminRole;
import com.venus.classificacao.repository.user.UserRepository;
import com.venus.classificacao.security.AdminJwtAuthenticationConverter;
import com.venus.classificacao.security.AdminTokenService;
import com.venus.classificacao.security.CurrentUserProvider;
import com.venus.classificacao.security.OwnershipGuard;
import com.venus.classificacao.security.SecurityErrorHandler;
import com.venus.classificacao.security.SecurityRoles;
import com.venus.classificacao.service.BaseScoreService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(controllers = BaseScoreController.class, properties = {
        "venus.security.admin-token.secret=segredo-de-teste-com-mais-de-32-bytes!!",
        "venus.security.firebase.project-id=venus-81bac",
        "server.port=0"
})
@Import({SecurityConfig.class, AdminTokenService.class, CurrentUserProvider.class, OwnershipGuard.class,
        SecurityErrorHandler.class})
class BaseScoreControllerSecurityTest {

    private static final long VERSION_ID = 118L;
    private static final String BY_VERSION = "/api/base-scores/product-version/{versionId}";
    private static final String ALL = "/api/base-scores";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BaseScoreService baseScoreService;

    @MockBean
    private UserRepository userRepository;

    @Test
    void withoutTokenReturns401InTheErrorFormat() throws Exception {
        mockMvc.perform(byVersion())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(SecurityErrorHandler.MISSING_TOKEN_MESSAGE));
        mockMvc.perform(all())
                .andExpect(status().isUnauthorized());
    }

    @Test
    void appUserCannotRecalculateBaseScores() throws Exception {
        expectForbiddenWithoutRecalculating(appUser());
    }

    @Test
    void moderatorCannotRecalculateBaseScores() throws Exception {
        expectForbiddenWithoutRecalculating(admin(AdminRole.MODERATOR));
    }

    @Test
    void analystCannotRecalculateBaseScores() throws Exception {
        expectForbiddenWithoutRecalculating(admin(AdminRole.ANALYST));
    }

    @Test
    void adminRecalculatesBaseScores() throws Exception {
        mockMvc.perform(byVersion().with(admin(AdminRole.ADMIN)))
                .andExpect(status().isOk());
        mockMvc.perform(all().with(admin(AdminRole.ADMIN)))
                .andExpect(status().isOk());
    }

    private void expectForbiddenWithoutRecalculating(RequestPostProcessor token) throws Exception {
        mockMvc.perform(byVersion().with(token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(SecurityErrorHandler.ACCESS_DENIED_MESSAGE));
        mockMvc.perform(all().with(token))
                .andExpect(status().isForbidden());
        verifyNoInteractions(baseScoreService);
    }

    private static MockHttpServletRequestBuilder byVersion() {
        return post(BY_VERSION, VERSION_ID);
    }

    private static MockHttpServletRequestBuilder all() {
        return post(ALL);
    }

    private static RequestPostProcessor appUser() {
        return jwt().jwt(token -> token.subject("uid-ana")).authorities(new SimpleGrantedAuthority(SecurityRoles.USER));
    }

    private static RequestPostProcessor admin(AdminRole role) {
        return jwt().jwt(token -> token.subject("3")).authorities(AdminJwtAuthenticationConverter.authoritiesFor(role));
    }
}
