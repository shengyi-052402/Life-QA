package com.forum.common.exception;

/**
 * 无权限异常
 */
public class ForbiddenException extends BaseException {
    public ForbiddenException() {
    }

    public ForbiddenException(String message) {
        super(message);
    }
}
