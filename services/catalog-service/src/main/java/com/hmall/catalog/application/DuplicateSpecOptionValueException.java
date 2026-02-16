package com.hmall.catalog.application;

/**
 * 同维度内已存在同值选项时抛出，返回 400。
 */
public class DuplicateSpecOptionValueException extends CatalogBadRequestException {

    public DuplicateSpecOptionValueException(String message) {
        super(message);
    }
}
