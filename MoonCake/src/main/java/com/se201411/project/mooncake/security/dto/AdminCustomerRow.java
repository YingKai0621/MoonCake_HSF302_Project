package com.se201411.project.mooncake.security.dto;

import com.se201411.project.mooncake.entity.Customer;
import com.se201411.project.mooncake.enums.AccountStatus;
import com.se201411.project.mooncake.order.dto.CustomerOrderCount;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public record AdminCustomerRow(
    long customerId,
    String email,
    String fullName,
    String phone,
    String memberSince,
    boolean active,
    long orderCount,
    long cancelledCount
) {

    private static final ZoneId VIETNAM_TIME_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static AdminCustomerRow entityToDto(Customer customer, CustomerOrderCount count) {
        String memberSince = customer.getCreatedAt()
            .atZone(ZoneOffset.UTC)
            .withZoneSameInstant(VIETNAM_TIME_ZONE)
            .format(DATE_FORMAT);
        boolean active = customer.getAccount().getStatus() == AccountStatus.ACTIVE;
        return new AdminCustomerRow(
            customer.getId(),
            customer.getEmail(),
            customer.getFullName(),
            customer.getPhone(),
            memberSince,
            active,
            count.total(),
            count.cancelled()
        );
    }
}
