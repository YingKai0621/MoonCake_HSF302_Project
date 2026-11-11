package com.se201411.project.mooncake.security.dto;

import java.util.List;

public record AccountLockResult(
    int cancelledCount,
    List<String> manualOrderCodes
) {

    public AccountLockResult {
        manualOrderCodes = List.copyOf(manualOrderCodes);
    }

    public String message() {
        StringBuilder message = new StringBuilder("Đã khóa tài khoản.");
        if (cancelledCount > 0) {
            message.append(" Đã hủy ");
            message.append(cancelledCount);
            message.append(" đơn chưa thanh toán và hoàn kho.");
        }
        if (!manualOrderCodes.isEmpty()) {
            message.append(" Cần xử lý thủ công: ");
            message.append(String.join(", ", manualOrderCodes));
        }
        return message.toString();
    }
}
