package com.hmall.catalog.application;

/**
 * 规格选项已被 SKU 使用时无法删除，返回 400。
 */
public class SpecOptionInUseException extends CatalogBadRequestException {

    public SpecOptionInUseException(String message) {
        super(message);
    }
}
