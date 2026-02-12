package com.hmall.user.application;

/**
 * 用户名已存在时抛出。
 */
public class UsernameExistsException extends UserBadRequestException {

    public UsernameExistsException(String message) {
        super(message);
    }
}
