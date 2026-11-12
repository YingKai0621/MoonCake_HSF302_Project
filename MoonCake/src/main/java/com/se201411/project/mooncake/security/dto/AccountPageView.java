package com.se201411.project.mooncake.security.dto;

import com.se201411.project.mooncake.entity.Customer;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public record AccountPageView(
    String email,
    String fullName,
    String phone,
    String memberSince
) {

    private static final ZoneId VIETNAM_TIME_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static AccountPageView entityToDto(Customer customer) {
        String memberSince = customer.getCreatedAt()
            .atZone(ZoneOffset.UTC)
            .withZoneSameInstant(VIETNAM_TIME_ZONE)
            .format(DATE_FORMAT);
        return new AccountPageView(
            customer.getEmail(),
            customer.getFullName(),
            customer.getPhone(),
            memberSince
        );
    }
}
