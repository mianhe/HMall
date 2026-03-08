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

    List<Sku> findBySpuIdIn(List<Long> spuIds);

    /** 是否存在任意 SKU 使用了该规格选项 */
    boolean existsBySpecOptionId(Long optionId);

    void deleteById(Long id);
}
