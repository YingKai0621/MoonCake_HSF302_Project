package com.se201411.project.mooncake.security.service.impl;

import com.se201411.project.mooncake.common.exception.ResourceNotFoundException;
import com.se201411.project.mooncake.entity.Customer;
import com.se201411.project.mooncake.security.dto.CustomerProfile;
import com.se201411.project.mooncake.security.dto.ShopperIdentity;
import com.se201411.project.mooncake.security.repository.CustomerRepository;
import com.se201411.project.mooncake.security.service.CustomerProfileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CustomerProfileServiceImpl implements CustomerProfileService {

    private final CustomerRepository customerRepository;

    public CustomerProfileServiceImpl(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public CustomerProfile requireProfile(ShopperIdentity customerIdentity) {
        if (customerIdentity == null) {
            throw new ResourceNotFoundException("Không tìm thấy thông tin khách hàng");
        }

        Customer customer = customerRepository.findById(customerIdentity.customerId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Không tìm thấy thông tin khách hàng"
            ));

        return new CustomerProfile(
            customer.getId(),
            customer.getFullName(),
            customer.getPhone(),
            customer.getEmail()
        );
    }
}
