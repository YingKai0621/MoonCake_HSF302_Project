package com.se201411.project.mooncake.security.dto;

import com.se201411.project.mooncake.entity.Customer;

public record CustomerProfile(
    long customerId,
    String fullName,
    String phone,
    String email
) {

    /** Entity -> DTO */
    public static CustomerProfile entityToDto(Customer customer) {
        return new CustomerProfile(
            customer.getId(),
            customer.getFullName(),
            customer.getPhone(),
            customer.getEmail()
        );
    }
}
