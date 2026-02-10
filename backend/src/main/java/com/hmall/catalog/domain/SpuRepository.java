package com.hmall.catalog.domain;

import java.util.List;
import java.util.Optional;

/**
 * SPU（商品）仓储：持久化与按类别/ID 查询。
 */
public interface SpuRepository {

    Spu save(Spu spu);

    Optional<Spu> findById(Long id);

    List<Spu> findByCategoryId(Long categoryId);

    void deleteById(Long id);
}
