package com.hmall.catalog.application;

/**
 * 业务校验失败（400）的公共基类，便于 ExceptionHandler 统一处理。
 */
public abstract class CatalogBadRequestException extends RuntimeException {

    protected CatalogBadRequestException(String message) {
        super(message);
    }
}
