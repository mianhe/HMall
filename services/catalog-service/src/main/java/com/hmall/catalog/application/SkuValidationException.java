package com.hmall.catalog.application;

/**
 * SKU 业务校验失败时抛出（Option 不属于该 SPU、未选齐必填维度、价格不能为负等），返回 400。
 */
public class SkuValidationException extends CatalogBadRequestException {

    public SkuValidationException(String message) {
        super(message);
    }
}
