package com.venus.classificacao.repository.user;

import com.venus.classificacao.entity.user.UserProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    @EntityGraph(attributePaths = "user")
    Optional<UserProfile> findByUserId(Long userId);
}
