package com.hmall.cart.application.port;

import java.util.List;
import java.util.Optional;

/**
 * 查询 SKU 信息的出站端口（对接 Catalog BC）。
 */
public interface SkuQueryPort {

    /** 校验 SKU 是否存在 */
    boolean exists(Long skuId);

    /** 批量查询 SKU 展示信息 */
    List<SkuInfo> queryByIds(List<Long> skuIds);

    /** 查询单个 SKU 信息 */
    Optional<SkuInfo> queryById(Long skuId);
}
