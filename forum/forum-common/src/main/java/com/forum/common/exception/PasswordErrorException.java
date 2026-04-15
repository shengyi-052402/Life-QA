package com.forum.common.exception;

/**
 * 密码错误异常
 */
public class PasswordErrorException extends BaseException {
    public PasswordErrorException() {
    }

    public PasswordErrorException(String message) {
        super(message);
    }
}
