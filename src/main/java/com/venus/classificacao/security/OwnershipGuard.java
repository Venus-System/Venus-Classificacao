package com.venus.classificacao.security;

import com.venus.classificacao.entity.user.User;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component("ownership")
@Transactional(readOnly = true)
public class OwnershipGuard {

    private final CurrentUserProvider currentUserProvider;

    public OwnershipGuard(CurrentUserProvider currentUserProvider) {
        this.currentUserProvider = currentUserProvider;
    }

    public boolean canAccessUser(Long userId) {
        return userId != null && currentUserProvider.activeUser()
                .map(User::getId)
                .filter(userId::equals)
                .isPresent();
    }
}
