package com.se201411.project.mooncake.security.dto;

public record CustomerProfile(
    long customerId,
    String fullName,
    String phone,
    String email
) {
}
