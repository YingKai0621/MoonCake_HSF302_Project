package com.se201411.project.mooncake.security.exception;

import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;

public class WrongCurrentPasswordException extends BusinessRuleViolationException {

    public WrongCurrentPasswordException() {
        super("Mật khẩu hiện tại không đúng.");
    }
}
