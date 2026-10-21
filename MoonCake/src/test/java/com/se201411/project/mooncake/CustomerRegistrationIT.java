package com.se201411.project.mooncake;

import com.se201411.project.mooncake.entity.Account;
import com.se201411.project.mooncake.entity.Customer;
import com.se201411.project.mooncake.enums.AccountRole;
import com.se201411.project.mooncake.enums.AccountStatus;
import com.se201411.project.mooncake.security.dto.CustomerRegistrationForm;
import com.se201411.project.mooncake.security.repository.AccountRepository;
import com.se201411.project.mooncake.security.repository.CustomerRepository;
import com.se201411.project.mooncake.security.service.AuthenticationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("integration")
@ContextConfiguration(initializers = MoonCakeApplicationIT.TestDatabaseGuard.class)
@Transactional
class CustomerRegistrationIT {

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void registrationPersistsLinkedAccountAndCustomer() {
        String email = "customer-" + UUID.randomUUID() + "@example.com";
        CustomerRegistrationForm form = registrationForm(email);

        authenticationService.registerCustomer(form);

        Optional<Account> accountResult = accountRepository.findByEmail(email);
        assertThat(accountResult).isPresent();

        Account account = accountResult.get();
        assertThat(account.getRole()).isEqualTo(AccountRole.CUSTOMER);
        assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(passwordEncoder.matches("password123", account.getPasswordHash())).isTrue();

        Optional<Customer> customerResult = customerRepository.findByAccount_Id(account.getId());
        assertThat(customerResult).isPresent();
        assertThat(customerResult.get().getEmail()).isEqualTo(email);
        assertThat(customerResult.get().getFullName()).isEqualTo("Khách kiểm thử");
    }

    private CustomerRegistrationForm registrationForm(String email) {
        CustomerRegistrationForm form = new CustomerRegistrationForm();
        form.setEmail(email);
        form.setPassword("password123");
        form.setConfirmPassword("password123");
        form.setFullName("Khách kiểm thử");
        form.setPhone("0901234567");
        return form;
    }
}
