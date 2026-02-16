package com.hmall.catalog.domain;

import java.util.List;
import java.util.Optional;

/**
 * 产品展示图仓储。
 */
public interface ProductImageRepository {

    ProductImage save(ProductImage productImage);

    Optional<ProductImage> findById(Long id);

    /** 产品级展示图（specOptionId 为空），按 sortOrder、id 排序 */
    List<ProductImage> findBySpuIdAndSpecOptionIdIsNull(Long spuId);

    /** 某选项的展示图，按 sortOrder、id 排序 */
    List<ProductImage> findBySpecOptionId(Long specOptionId);

    void deleteById(Long id);
}
