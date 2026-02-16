package com.hmall.inventory.domain;

import java.util.Optional;

/**
 * SkuStock 聚合根仓储。
 */
public interface SkuStockRepository {

    Optional<SkuStock> findBySkuId(Long skuId);

    SkuStock save(SkuStock stock);
}
