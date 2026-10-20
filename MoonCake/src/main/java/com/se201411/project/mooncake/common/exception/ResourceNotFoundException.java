package com.se201411.project.mooncake.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends MoonCakeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
