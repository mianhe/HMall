package com.hmall.user.application;

public class UserBadRequestException extends RuntimeException {

    public UserBadRequestException(String message) {
        super(message);
    }
}
