package com.hmall.catalog.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductImageJpaRepository extends JpaRepository<ProductImageEntity, Long> {

    List<ProductImageEntity> findBySpuIdAndSpecOptionIdIsNullOrderBySortOrderAscIdAsc(Long spuId);

    List<ProductImageEntity> findBySpecOptionIdOrderBySortOrderAscIdAsc(Long specOptionId);
}
