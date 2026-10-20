package com.se201411.project.mooncake.common.exception;

public abstract class MoonCakeException extends RuntimeException {

    protected MoonCakeException(String message) {
        super(message);
    }

    protected MoonCakeException(String message, Throwable cause) {
        super(message, cause);
    }
}
