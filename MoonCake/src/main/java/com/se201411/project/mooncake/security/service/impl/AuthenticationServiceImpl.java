package com.se201411.project.mooncake.security.service.impl;

import com.se201411.project.mooncake.common.exception.ResourceNotFoundException;
import com.se201411.project.mooncake.entity.Account;
import com.se201411.project.mooncake.entity.Customer;
import com.se201411.project.mooncake.enums.AccountRole;
import com.se201411.project.mooncake.enums.AccountStatus;
import com.se201411.project.mooncake.security.dto.CustomerRegistrationForm;
import com.se201411.project.mooncake.security.dto.ShopperIdentity;
import com.se201411.project.mooncake.security.exception.EmailAlreadyRegisteredException;
import com.se201411.project.mooncake.security.exception.SamePasswordException;
import com.se201411.project.mooncake.security.exception.WrongCurrentPasswordException;
import com.se201411.project.mooncake.security.repository.AccountRepository;
import com.se201411.project.mooncake.security.repository.CustomerRepository;
import com.se201411.project.mooncake.security.repository.PasswordResetTokenRepository;
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
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public AuthenticationServiceImpl(
        AccountRepository accountRepository,
        CustomerRepository customerRepository,
        PasswordResetTokenRepository passwordResetTokenRepository,
        PasswordEncoder passwordEncoder,
        Clock clock
    ) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
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
        form.dtoToEntity(customer);
        customer.setEmail(normalizedEmail);
        customer.setCreatedAt(createdAt);
        customerRepository.save(customer);
    }

    @Override
    @Transactional
    public void changePassword(ShopperIdentity customerIdentity, String currentPassword, String newPassword) {
        Customer customer = customerRepository.findById(customerIdentity.customerId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin khách hàng"));
        Account account = customer.getAccount();

        if (!passwordEncoder.matches(currentPassword, account.getPasswordHash())) {
            throw new WrongCurrentPasswordException();
        }
        if (passwordEncoder.matches(newPassword, account.getPasswordHash())) {
            throw new SamePasswordException();
        }

        account.setPasswordHash(passwordEncoder.encode(newPassword));
        accountRepository.save(account);
        passwordResetTokenRepository.invalidateOpenTokens(account.getId(), LocalDateTime.now(clock));
    }
}
