package com.venus.classificacao.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.venus.classificacao.entity.user.User;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OwnershipGuardTest {

    private static final long ANA_ID = 10L;
    private static final long BIA_ID = 11L;

    @Mock
    private CurrentUserProvider currentUserProvider;

    private OwnershipGuard guard;

    @BeforeEach
    void setUp() {
        guard = new OwnershipGuard(currentUserProvider);
    }

    @Test
    void ownerAccessesOwnData() {
        loggedInAs(ANA_ID);
        assertThat(guard.canAccessUser(ANA_ID)).isTrue();
    }

    @Test
    void userCannotAccessAnotherUsersData() {
        loggedInAs(ANA_ID);
        assertThat(guard.canAccessUser(BIA_ID)).isFalse();
    }

    @Test
    void adminAccessesAnyUser() {
        when(currentUserProvider.isAdmin()).thenReturn(true);
        assertThat(guard.canAccessUser(BIA_ID)).isTrue();
    }

    @Test
    void tokenWithoutActiveAccountAccessesNothing() {
        when(currentUserProvider.isAdmin()).thenReturn(false);
        when(currentUserProvider.activeUser()).thenReturn(Optional.empty());
        assertThat(guard.canAccessUser(ANA_ID)).isFalse();
    }

    @Test
    void missingUserIdIsRefused() {
        loggedInAs(ANA_ID);
        assertThat(guard.canAccessUser(null)).isFalse();
    }

    private void loggedInAs(long userId) {
        User user = new User();
        user.setId(userId);
        lenient().when(currentUserProvider.isAdmin()).thenReturn(false);
        lenient().when(currentUserProvider.activeUser()).thenReturn(Optional.of(user));
    }
}
