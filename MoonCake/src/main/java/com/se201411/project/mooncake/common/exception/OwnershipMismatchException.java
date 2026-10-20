package com.se201411.project.mooncake.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class OwnershipMismatchException extends MoonCakeException {

    public OwnershipMismatchException(String message) {
        super(message);
    }
}
