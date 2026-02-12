package com.hmall.user.application;

/**
 * 登录失败（用户不存在或密码错误）时抛出，由 ExceptionHandler 映射为 401。
 */
public class LoginFailedException extends RuntimeException {

    public LoginFailedException(String message) {
        super(message);
    }
}
