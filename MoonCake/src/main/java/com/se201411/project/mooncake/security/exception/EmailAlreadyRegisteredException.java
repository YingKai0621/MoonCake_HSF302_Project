package com.se201411.project.mooncake.security.exception;

import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;

public class EmailAlreadyRegisteredException extends BusinessRuleViolationException {

    public EmailAlreadyRegisteredException() {
        super("Email này đã được đăng ký");
    }
}
