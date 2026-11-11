package com.se201411.project.mooncake.security.service.impl;

import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import com.se201411.project.mooncake.common.exception.ResourceNotFoundException;
import com.se201411.project.mooncake.entity.Account;
import com.se201411.project.mooncake.entity.Customer;
import com.se201411.project.mooncake.enums.AccountRole;
import com.se201411.project.mooncake.enums.AccountStatus;
import com.se201411.project.mooncake.security.dto.AccountPageView;
import com.se201411.project.mooncake.security.dto.CustomerProfile;
import com.se201411.project.mooncake.security.dto.ProfileUpdateForm;
import com.se201411.project.mooncake.security.dto.ShopperIdentity;
import com.se201411.project.mooncake.security.repository.AccountRepository;
import com.se201411.project.mooncake.security.repository.CustomerRepository;
import com.se201411.project.mooncake.security.service.CustomerProfileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class CustomerProfileServiceImpl implements CustomerProfileService {

    private static final String LOCKED_ACCOUNT_MESSAGE =
        "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ cửa hàng để được hỗ trợ.";

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

    @Override
    @Transactional(readOnly = true)
    public Set<Long> disabledCustomerIds(Collection<Long> customerIds) {
        if (customerIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(customerRepository.findIdsWithAccountStatus(customerIds, AccountStatus.DISABLED));
    }

    @Override
    @Transactional(readOnly = true)
    public void requireActiveAccount(ShopperIdentity customerIdentity) {
        Customer customer = requireCustomer(customerIdentity);
        if (customer.getAccount().getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessRuleViolationException(LOCKED_ACCOUNT_MESSAGE);
        }
    }

    @Override
    @Transactional
    public boolean disableAccount(long customerId) {
        return changeStatus(customerId, AccountStatus.ACTIVE, AccountStatus.DISABLED);
    }

    @Override
    @Transactional
    public boolean enableAccount(long customerId) {
        return changeStatus(customerId, AccountStatus.DISABLED, AccountStatus.ACTIVE);
    }

    private boolean changeStatus(long customerId, AccountStatus currentStatus, AccountStatus newStatus) {
        Account account = requireCustomerById(customerId).getAccount();
        int changedRows = accountRepository.updateStatus(
            account.getId(),
            AccountRole.CUSTOMER,
            currentStatus,
            newStatus
        );
        return changedRows == 1;
    }

    private Customer requireCustomerById(long customerId) {
        return customerRepository.findById(customerId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin khách hàng"));
    }

    private Customer requireCustomer(ShopperIdentity customerIdentity) {
        if (customerIdentity == null) {
            throw new ResourceNotFoundException("Không tìm thấy thông tin khách hàng");
        }
        return requireCustomerById(customerIdentity.customerId());
    }
}
