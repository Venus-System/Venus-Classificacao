package com.venus.classificacao.repository.product;

import com.venus.classificacao.entity.product.ProductVersion;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductVersionRepository extends JpaRepository<ProductVersion, Long> {

    Optional<ProductVersion> findByProductIdAndIsCurrentTrue(Long productId);

    @EntityGraph(attributePaths = {"product", "product.brand"})
    Optional<ProductVersion> findWithBrandById(Long id);

    @EntityGraph(attributePaths = "product")
    Slice<ProductVersion> findByIsCurrentTrueOrderByIdAsc(Pageable pageable);
}
