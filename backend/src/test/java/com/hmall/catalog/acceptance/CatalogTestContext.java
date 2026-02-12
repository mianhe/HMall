package com.hmall.catalog.acceptance;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 验收测试共享上下文：跨 Step Definition 类共享「商品名→id」「维度/选项→id」等，供 spec-dimension、sku 步骤解析「该 SPU」「维度 容量」等。
 * 状态码读写委托给 {@link LastResponseContext}，供 Catalog 与 User 共享断言步骤。
 */
public class CatalogTestContext {

    private final LastResponseContext lastResponseContext;
    private final ConcurrentHashMap<String, Long> productNameToId = new ConcurrentHashMap<>();
    /** key: productName:dimensionName -> dimensionId */
    private final ConcurrentHashMap<String, Long> dimensionIdByProductAndName = new ConcurrentHashMap<>();
    /** key: productName:dimensionName:optionValue -> optionId */
    private final ConcurrentHashMap<String, Long> optionIdByProductDimensionAndValue = new ConcurrentHashMap<>();
    /** productName -> last created skuId (for "该 SKU") */
    private final ConcurrentHashMap<String, Long> lastSkuIdByProductName = new ConcurrentHashMap<>();
    /** 当前场景中的 SPU 商品名（由 Given「已存在商品 xxx」设置，供「该 SPU」解析） */
    private volatile String lastProductName;

    public CatalogTestContext(LastResponseContext lastResponseContext) {
        this.lastResponseContext = lastResponseContext;
    }

    public int getLastStatusCode() {
        return lastResponseContext.getLastStatusCode();
    }

    public void setLastStatusCode(int statusCode) {
        lastResponseContext.setLastStatusCode(statusCode);
    }

    public String getLastProductName() {
        return lastProductName;
    }

    public void setLastProductName(String productName) {
        this.lastProductName = productName;
    }

    public Long getSpuId(String productName) {
        return productNameToId.get(productName);
    }

    public void putSpuId(String productName, Long spuId) {
        if (spuId != null) {
            productNameToId.put(productName, spuId);
        }
    }

    public Long getDimensionId(String productName, String dimensionName) {
        return dimensionIdByProductAndName.get(key(productName, dimensionName));
    }

    public void putDimensionId(String productName, String dimensionName, Long dimensionId) {
        if (dimensionId != null) {
            dimensionIdByProductAndName.put(key(productName, dimensionName), dimensionId);
        }
    }

    public Long getOptionId(String productName, String dimensionName, String optionValue) {
        return optionIdByProductDimensionAndValue.get(key(productName, dimensionName, optionValue));
    }

    public void putOptionId(String productName, String dimensionName, String optionValue, Long optionId) {
        if (optionId != null) {
            optionIdByProductDimensionAndValue.put(key(productName, dimensionName, optionValue), optionId);
        }
    }

    public Long getLastSkuId(String productName) {
        return lastSkuIdByProductName.get(productName);
    }

    public void putLastSkuId(String productName, Long skuId) {
        if (skuId != null) {
            lastSkuIdByProductName.put(productName, skuId);
        }
    }

    private static String key(String productName, String dimensionName) {
        return productName + ":" + dimensionName;
    }

    private static String key(String productName, String dimensionName, String optionValue) {
        return productName + ":" + dimensionName + ":" + optionValue;
    }
}
