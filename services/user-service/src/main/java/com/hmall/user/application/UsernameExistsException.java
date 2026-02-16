package com.hmall.user.application;

public class UsernameExistsException extends UserBadRequestException {

    public UsernameExistsException(String message) {
        super(message);
    }
}
