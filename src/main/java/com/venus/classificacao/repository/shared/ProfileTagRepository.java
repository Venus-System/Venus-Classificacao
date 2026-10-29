package com.venus.classificacao.repository.shared;

import com.venus.classificacao.entity.shared.ProfileTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProfileTagRepository extends JpaRepository<ProfileTag, Long> {
}
