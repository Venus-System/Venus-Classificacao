package com.venus.classificacao.repository.user;

import com.venus.classificacao.entity.user.UserProfileTag;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserProfileTagRepository extends JpaRepository<UserProfileTag, Long> {

    @EntityGraph(attributePaths = "profileTag")
    List<UserProfileTag> findByUserId(Long userId);
}
