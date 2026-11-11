package com.se201411.project.mooncake.security.service.impl;

import com.se201411.project.mooncake.common.exception.ResourceNotFoundException;
import com.se201411.project.mooncake.entity.Customer;
import com.se201411.project.mooncake.security.dto.AccountPageView;
import com.se201411.project.mooncake.security.dto.CustomerProfile;
import com.se201411.project.mooncake.security.dto.ProfileUpdateForm;
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
        Customer customer = requireCustomer(customerIdentity);
        return CustomerProfile.entityToDto(customer);
    }

    @Override
    public Customer customerReference(long customerId) {
        return customerRepository.getReferenceById(customerId);
    }

    @Override
    @Transactional(readOnly = true)
    public long accountIdOf(long customerId) {
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin khách hàng"));
        return customer.getAccount().getId();
    }

    @Override
    @Transactional(readOnly = true)
    public AccountPageView accountPage(ShopperIdentity customerIdentity) {
        Customer customer = requireCustomer(customerIdentity);
        return AccountPageView.entityToDto(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileUpdateForm profileForm(ShopperIdentity customerIdentity) {
        Customer customer = requireCustomer(customerIdentity);
        return ProfileUpdateForm.entityToDto(customer);
    }

    @Override
    @Transactional
    public void updateProfile(ShopperIdentity customerIdentity, ProfileUpdateForm form) {
        Customer customer = requireCustomer(customerIdentity);
        form.dtoToEntity(customer);
        customerRepository.save(customer);
    }

    private Customer requireCustomer(ShopperIdentity customerIdentity) {
        if (customerIdentity == null) {
            throw new ResourceNotFoundException("Không tìm thấy thông tin khách hàng");
        }
        return customerRepository.findById(customerIdentity.customerId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin khách hàng"));
    }
}
