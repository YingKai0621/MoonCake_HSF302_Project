package com.se201411.project.mooncake.security.service.impl;

import com.se201411.project.mooncake.entity.Account;
import com.se201411.project.mooncake.entity.Customer;
import com.se201411.project.mooncake.enums.AccountRole;
import com.se201411.project.mooncake.enums.AccountStatus;
import com.se201411.project.mooncake.security.dto.CustomerRegistrationForm;
import com.se201411.project.mooncake.security.exception.EmailAlreadyRegisteredException;
import com.se201411.project.mooncake.security.repository.AccountRepository;
import com.se201411.project.mooncake.security.repository.CustomerRepository;
import com.se201411.project.mooncake.security.service.AuthenticationService;
import com.se201411.project.mooncake.security.util.EmailNormalizer;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public AuthenticationServiceImpl(
        AccountRepository accountRepository,
        CustomerRepository customerRepository,
        PasswordEncoder passwordEncoder,
        Clock clock
    ) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void registerCustomer(CustomerRegistrationForm form) {
        String normalizedEmail = EmailNormalizer.normalize(form.getEmail());
        if (accountRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyRegisteredException();
        }

        LocalDateTime createdAt = LocalDateTime.now(clock);

        Account account = new Account();
        account.setEmail(normalizedEmail);
        account.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        account.setRole(AccountRole.CUSTOMER);
        account.setStatus(AccountStatus.ACTIVE);
        account.setCreatedAt(createdAt);

        try {
            account = accountRepository.save(account);
        } catch (DataIntegrityViolationException exception) {
            throw new EmailAlreadyRegisteredException();
        }

        Customer customer = new Customer();
        customer.setAccount(account);
        customer.setFullName(form.getFullName().trim());
        customer.setPhone(form.getPhone().trim());
        customer.setEmail(normalizedEmail);
        customer.setCreatedAt(createdAt);
        customerRepository.save(customer);
    }
}
