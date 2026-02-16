package com.hmall.catalog.application;

/**
 * 同 SPU 内已存在同名维度时抛出，返回 400。
 */
public class DuplicateSpecDimensionNameException extends CatalogBadRequestException {

    public DuplicateSpecDimensionNameException(String message) {
        super(message);
    }
}
