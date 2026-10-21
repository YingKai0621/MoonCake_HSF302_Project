package com.se201411.project.mooncake.security.service.impl;

import com.se201411.project.mooncake.entity.Account;
import com.se201411.project.mooncake.entity.Customer;
import com.se201411.project.mooncake.enums.AccountRole;
import com.se201411.project.mooncake.security.auth.MoonCakeUserPrincipal;
import com.se201411.project.mooncake.security.repository.AccountRepository;
import com.se201411.project.mooncake.security.repository.CustomerRepository;
import com.se201411.project.mooncake.security.util.EmailNormalizer;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class DatabaseUserDetailsService implements UserDetailsService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;

    public DatabaseUserDetailsService(
        AccountRepository accountRepository,
        CustomerRepository customerRepository
    ) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String normalizedEmail = EmailNormalizer.normalize(email);
        Optional<Account> accountResult = accountRepository.findByEmail(normalizedEmail);
        if (accountResult.isEmpty()) {
            throw invalidCredentials();
        }

        Account account = accountResult.get();
        Long customerId = findCustomerId(account);

        return new MoonCakeUserPrincipal(
            account.getId(),
            customerId,
            account.getEmail(),
            account.getPasswordHash(),
            account.getRole(),
            account.getStatus()
        );
    }

    private Long findCustomerId(Account account) {
        if (account.getRole() != AccountRole.CUSTOMER) {
            return null;
        }

        Optional<Customer> customerResult = customerRepository.findByAccount_Id(account.getId());
        if (customerResult.isEmpty()) {
            throw invalidCredentials();
        }
        return customerResult.get().getId();
    }

    private UsernameNotFoundException invalidCredentials() {
        return new UsernameNotFoundException("Thông tin đăng nhập không hợp lệ");
    }
}
