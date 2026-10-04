package com.venus.classificacao.security;

import com.venus.classificacao.entity.enums.AdminRole;
import java.util.Arrays;
import java.util.List;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public class AdminJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        return new JwtAuthenticationToken(jwt, authoritiesFor(roleOf(jwt)), jwt.getSubject());
    }

    public static List<GrantedAuthority> authoritiesFor(AdminRole role) {
        if (role == null) {
            return List.of();
        }
        return switch (role) {
            case ADMIN -> authorities(SecurityRoles.ADMIN, SecurityRoles.MODERATOR, SecurityRoles.ANALYST);
            case MODERATOR -> authorities(SecurityRoles.MODERATOR, SecurityRoles.ANALYST);
            case ANALYST -> authorities(SecurityRoles.ANALYST);
        };
    }

    private static List<GrantedAuthority> authorities(String... roles) {
        return Arrays.stream(roles).<GrantedAuthority>map(SimpleGrantedAuthority::new).toList();
    }

    private AdminRole roleOf(Jwt jwt) {
        String role = jwt.getClaimAsString(AdminTokenService.ROLE_CLAIM);
        if (role == null) {
            return null;
        }
        try {
            return AdminRole.valueOf(role);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
