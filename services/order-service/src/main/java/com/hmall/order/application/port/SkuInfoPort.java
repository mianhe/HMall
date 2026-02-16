package com.hmall.order.application.port;

/**
 * SKU 信息端口，供 Order 获取商品价格与展示名。
 * 单体时由 Catalog 实现；微服务时由 HTTP 调用 Catalog API 实现。
 */
public interface SkuInfoPort {

    /**
     * 按 skuId 获取 SKU 信息。
     * @throws IllegalArgumentException 当 SKU 不存在时（对应 404）
     */
    SkuInfo getById(Long skuId);

    record SkuInfo(Long id, long priceCents, String displayName) {}
}
