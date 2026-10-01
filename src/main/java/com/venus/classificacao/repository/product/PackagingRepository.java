package com.venus.classificacao.repository.product;

import com.venus.classificacao.entity.product.Packaging;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PackagingRepository extends JpaRepository<Packaging, Long> {

    Optional<Packaging> findByProductVersionId(Long productVersionId);
}
