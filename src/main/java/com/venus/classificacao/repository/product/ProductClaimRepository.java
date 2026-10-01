package com.venus.classificacao.repository.product;

import com.venus.classificacao.entity.enums.ClaimType;
import com.venus.classificacao.entity.product.ProductClaim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductClaimRepository extends JpaRepository<ProductClaim, Long> {

    long countByProductVersionIdAndWasVerifiedTrueAndClaimClaimType(Long productVersionId, ClaimType claimType);
}
