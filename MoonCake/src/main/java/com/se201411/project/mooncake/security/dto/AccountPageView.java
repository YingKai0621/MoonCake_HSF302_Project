package com.se201411.project.mooncake.security.dto;

import com.se201411.project.mooncake.entity.Customer;

import java.time.format.DateTimeFormatter;

public record AccountPageView(
    String email,
    String fullName,
    String phone,
    String memberSince
) {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static AccountPageView entityToDto(Customer customer) {
        return new AccountPageView(
            customer.getEmail(),
            customer.getFullName(),
            customer.getPhone(),
            customer.getCreatedAt().format(DATE_FORMAT)
        );
    }
}
