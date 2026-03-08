package com.hmall.inventory.domain;

import java.util.List;
import java.util.Optional;

/**
 * SkuStock 聚合根仓储。
 */
public interface SkuStockRepository {

    Optional<SkuStock> findBySkuId(Long skuId);

    List<SkuStock> findBySkuIdIn(List<Long> skuIds);

    List<SkuStock> findAll();

    SkuStock save(SkuStock stock);
}
