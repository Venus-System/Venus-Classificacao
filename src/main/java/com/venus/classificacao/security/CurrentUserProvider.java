package com.venus.classificacao.security;

import com.venus.classificacao.entity.enums.UserStatus;
import com.venus.classificacao.entity.user.User;
import com.venus.classificacao.exception.DataAccessFailureTranslator;
import com.venus.classificacao.repository.user.UserRepository;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

@Component
public class CurrentUserProvider {

    private static final Logger log = LoggerFactory.getLogger(CurrentUserProvider.class);

    private static final String CURRENT_USER_ATTRIBUTE = CurrentUserProvider.class.getName() + ".currentUser";
    private static final Set<UserStatus> ACTIVE_STATUSES = EnumSet.of(UserStatus.ACTIVE, UserStatus.PENDING);

    private final UserRepository userRepository;

    public CurrentUserProvider(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean isAdmin() {
        Authentication authentication = currentAuthentication();
        return authentication != null && hasAuthority(authentication, SecurityRoles.ADMIN);
    }

    public Optional<Long> adminUserId() {
        Authentication authentication = currentAuthentication();
        if (authentication == null || !hasAuthority(authentication, SecurityRoles.ANALYST)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.valueOf(authentication.getName()));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    public Optional<String> firebaseUid() {
        Authentication authentication = currentAuthentication();
        if (authentication == null || !hasAuthority(authentication, SecurityRoles.USER)) {
            return Optional.empty();
        }
        return Optional.ofNullable(authentication.getName());
    }

    public Optional<User> activeUser() {
        return firebaseUid()
                .flatMap(this::findUser)
                .filter(user -> ACTIVE_STATUSES.contains(user.getStatus()));
    }

    private Optional<User> findUser(String firebaseUid) {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return loadUser(firebaseUid);
        }
        Object cached = attributes.getAttribute(CURRENT_USER_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        if (cached instanceof CachedUser cachedUser) {
            return cachedUser.user();
        }
        Optional<User> user = loadUser(firebaseUid);
        attributes.setAttribute(CURRENT_USER_ATTRIBUTE, new CachedUser(user), RequestAttributes.SCOPE_REQUEST);
        return user;
    }

    private Optional<User> loadUser(String firebaseUid) {
        try {
            return userRepository.findByFirebaseUid(firebaseUid);
        } catch (DataAccessException ex) {
            log.error("Falha ao consultar o usuario do token", ex);
            throw DataAccessFailureTranslator.translate(ex, "Falha ao consultar o usuario do token");
        }
    }

    private Authentication currentAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private boolean hasAuthority(Authentication authentication, String authority) {
        return authentication.getAuthorities().stream()
                .anyMatch(granted -> authority.equals(granted.getAuthority()));
    }

    private record CachedUser(Optional<User> user) {
    }
}
