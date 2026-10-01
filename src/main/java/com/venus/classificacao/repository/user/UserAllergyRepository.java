package com.venus.classificacao.repository.user;

import com.venus.classificacao.entity.user.UserAllergy;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserAllergyRepository extends JpaRepository<UserAllergy, Long> {

    @EntityGraph(attributePaths = "allergy")
    List<UserAllergy> findByUserId(Long userId);
}
