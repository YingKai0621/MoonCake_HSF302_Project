package com.se201411.project.mooncake;

import com.se201411.project.mooncake.entity.Account;
import com.se201411.project.mooncake.entity.Customer;
import com.se201411.project.mooncake.security.dto.CustomerRegistrationForm;
import com.se201411.project.mooncake.security.dto.ProfileUpdateForm;
import com.se201411.project.mooncake.security.dto.ShopperIdentity;
import com.se201411.project.mooncake.security.repository.AccountRepository;
import com.se201411.project.mooncake.security.repository.CustomerRepository;
import com.se201411.project.mooncake.security.service.AuthenticationService;
import com.se201411.project.mooncake.security.service.CustomerProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("integration")
@ContextConfiguration(initializers = MoonCakeApplicationIT.TestDatabaseGuard.class)
@Transactional
class AccountIT {

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private CustomerProfileService customerProfileService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void updatesProfileAndPasswordInMoonCakeTestDatabase() {
        Customer customer = registerCustomer();
        ShopperIdentity identity = ShopperIdentity.forCustomer(customer.getId());
        ProfileUpdateForm profileForm = new ProfileUpdateForm();
        profileForm.setFullName("Trần Thị Kiểm Thử");
        profileForm.setPhone("0912345678");

        customerProfileService.updateProfile(identity, profileForm);
        authenticationService.changePassword(
            identity,
            "password123",
            "newPassword123"
        );

        Customer updatedCustomer = customerRepository.findById(customer.getId()).orElseThrow();
        Account updatedAccount = accountRepository.findById(
            updatedCustomer.getAccount().getId()
        ).orElseThrow();
        assertThat(updatedCustomer.getFullName()).isEqualTo("Trần Thị Kiểm Thử");
        assertThat(updatedCustomer.getPhone()).isEqualTo("0912345678");
        assertThat(passwordEncoder.matches(
            "newPassword123",
            updatedAccount.getPasswordHash()
        )).isTrue();
        assertThat(passwordEncoder.matches(
            "password123",
            updatedAccount.getPasswordHash()
        )).isFalse();
    }

    private Customer registerCustomer() {
        String email = "account-" + UUID.randomUUID() + "@example.com";
        CustomerRegistrationForm form = new CustomerRegistrationForm();
        form.setEmail(email);
        form.setPassword("password123");
        form.setConfirmPassword("password123");
        form.setFullName("Khách kiểm thử tài khoản");
        form.setPhone("0901234567");
        authenticationService.registerCustomer(form);

        Account account = accountRepository.findByEmail(email).orElseThrow();
        return customerRepository.findByAccount_Id(account.getId()).orElseThrow();
    }
}
