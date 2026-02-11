package com.hmall.user.application;

/**
 * 业务校验失败（400）的公共基类，便于 ExceptionHandler 统一处理。
 */
public abstract class UserBadRequestException extends RuntimeException {

    protected UserBadRequestException(String message) {
        super(message);
    }
}
