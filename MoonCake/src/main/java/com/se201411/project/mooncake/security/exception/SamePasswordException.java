package com.se201411.project.mooncake.security.exception;

import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;

public class SamePasswordException extends BusinessRuleViolationException {

    public SamePasswordException() {
        super("Mật khẩu mới phải khác mật khẩu hiện tại.");
    }
}
