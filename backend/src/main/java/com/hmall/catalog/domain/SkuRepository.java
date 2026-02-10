package com.hmall.catalog.domain;

import java.util.List;
import java.util.Optional;

/**
 * SKU 仓储。
 */
public interface SkuRepository {

    Sku save(Sku sku);

    Optional<Sku> findById(Long id);

    List<Sku> findBySpuId(Long spuId);
}
