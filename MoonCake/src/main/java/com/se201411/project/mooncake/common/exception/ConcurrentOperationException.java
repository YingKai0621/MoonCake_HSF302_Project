package com.se201411.project.mooncake.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ConcurrentOperationException extends MoonCakeException {

    public ConcurrentOperationException(String message) {
        super(message);
    }
}
