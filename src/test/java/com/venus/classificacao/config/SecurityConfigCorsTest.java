package com.venus.classificacao.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;

class SecurityConfigCorsTest {

    private static final String WEB_ORIGIN = "https://venus-web-application.vercel.app";
    private static final String OTHER_ORIGIN = "https://site-qualquer.com";

    private final SecurityConfig securityConfig = new SecurityConfig();

    @Test
    void listedOriginIsAllowed() {
        CorsConfiguration config = corsFor(List.of(WEB_ORIGIN));

        assertThat(config.checkOrigin(WEB_ORIGIN)).isEqualTo(WEB_ORIGIN);
        assertThat(config.checkOrigin(OTHER_ORIGIN)).isNull();
    }

    @Test
    void emptyListAllowsAnyOrigin() {
        CorsConfiguration config = corsFor(List.of());

        assertThat(config.checkOrigin(OTHER_ORIGIN)).isEqualTo(OTHER_ORIGIN);
    }

    private CorsConfiguration corsFor(List<String> allowedOrigins) {
        return securityConfig.corsConfigurationSource(allowedOrigins)
                .getCorsConfiguration(new MockHttpServletRequest("GET", "/api/classifications"));
    }
}
