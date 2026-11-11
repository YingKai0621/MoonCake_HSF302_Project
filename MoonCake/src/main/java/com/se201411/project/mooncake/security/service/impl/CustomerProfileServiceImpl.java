package com.se201411.project.mooncake.security.service.impl;

import com.se201411.project.mooncake.common.exception.ResourceNotFoundException;
import com.se201411.project.mooncake.entity.Customer;
import com.se201411.project.mooncake.security.dto.CustomerProfile;
import com.se201411.project.mooncake.security.dto.ShopperIdentity;
import com.se201411.project.mooncake.security.exception.EmailAlreadyRegisteredException;
import com.se201411.project.mooncake.security.repository.AccountRepository;
import com.se201411.project.mooncake.security.repository.CustomerRepository;
import com.se201411.project.mooncake.security.service.CustomerProfileService;
import com.se201411.project.mooncake.security.util.EmailNormalizer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CustomerProfileServiceImpl implements CustomerProfileService {

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;

    public CustomerProfileServiceImpl(
        CustomerRepository customerRepository,
        AccountRepository accountRepository
    ) {
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
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

    @Override
    @Transactional
    public void updateFullName(ShopperIdentity customerIdentity, String fullName) {
        Customer customer = requireCustomer(customerIdentity);
        customer.setFullName(fullName.trim());
    }

    @Override
    @Transactional
    public void updateEmail(ShopperIdentity customerIdentity, String email) {
        Customer customer = requireCustomer(customerIdentity);
        String normalizedEmail = EmailNormalizer.normalize(email);
        Long accountId = customer.getAccount().getId();
        if (accountRepository.existsByEmailAndIdNot(normalizedEmail, accountId)) {
            throw new EmailAlreadyRegisteredException();
        }
        customer.getAccount().setEmail(normalizedEmail);
        customer.setEmail(normalizedEmail);
    }

    @Override
    @Transactional
    public void updatePhone(ShopperIdentity customerIdentity, String phone) {
        Customer customer = requireCustomer(customerIdentity);
        customer.setPhone(phone);
    }

    private Customer requireCustomer(ShopperIdentity customerIdentity) {
        if (customerIdentity == null) {
            throw new ResourceNotFoundException("Không tìm thấy thông tin khách hàng");
        }
        return customerRepository.findById(customerIdentity.customerId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin khách hàng"));
    }
}
