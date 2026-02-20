package com.hmall.cart.application.port;

import java.math.BigDecimal;

/**
 * 从 Catalog 获取的 SKU 展示信息。
 */
public record SkuInfo(Long skuId, String name, BigDecimal price, String imageUrl, boolean available) {}
