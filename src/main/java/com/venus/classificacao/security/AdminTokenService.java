package com.venus.classificacao.security;

import com.venus.classificacao.config.SecurityProperties;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;

@Service
public class AdminTokenService {

    public static final String ISSUER = "venus-crud";
    public static final String ROLE_CLAIM = "role";

    private static final String KEY_ALGORITHM = "HmacSHA256";

    private final SecretKey secretKey;

    public AdminTokenService(SecurityProperties securityProperties) {
        this.secretKey = new SecretKeySpec(
                securityProperties.adminToken().secret().getBytes(StandardCharsets.UTF_8), KEY_ALGORITHM);
    }

    public JwtDecoder decoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(ISSUER));
        return decoder;
    }
}
