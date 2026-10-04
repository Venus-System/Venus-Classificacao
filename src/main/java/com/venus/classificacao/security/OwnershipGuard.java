package com.venus.classificacao.security;

import com.venus.classificacao.entity.user.User;
import java.util.Optional;
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
        if (currentUserProvider.isAdmin()) {
            return true;
        }
        return userId != null && currentUserId().filter(userId::equals).isPresent();
    }

    private Optional<Long> currentUserId() {
        return currentUserProvider.activeUser().map(User::getId);
    }
}
