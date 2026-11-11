package com.se201411.project.mooncake.security.exception;

import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;

public class InvalidPasswordResetTokenException extends BusinessRuleViolationException {

    public InvalidPasswordResetTokenException() {
        super("Đường link đặt lại mật khẩu không hợp lệ hoặc đã hết hạn. Vui lòng yêu cầu link mới.");
    }
}
