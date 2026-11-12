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
    void customerUpdatesProfileAndChangesPassword() {
        String email = "account-" + UUID.randomUUID() + "@example.com";
        authenticationService.registerCustomer(registrationForm(email));
        Account account = accountRepository.findByEmail(email).orElseThrow();
        Customer customer = customerRepository.findByAccount_Id(account.getId()).orElseThrow();
        ShopperIdentity identity = ShopperIdentity.forCustomer(customer.getId());

        ProfileUpdateForm profileForm = new ProfileUpdateForm();
        profileForm.setFullName("  Trần Thị B  ");
        profileForm.setPhone("0912345678");
        customerProfileService.updateProfile(identity, profileForm);

        Customer updated = customerRepository.findById(customer.getId()).orElseThrow();
        assertThat(updated.getFullName()).isEqualTo("Trần Thị B");
        assertThat(updated.getPhone()).isEqualTo("0912345678");
        assertThat(updated.getEmail()).isEqualTo(email);

        authenticationService.changePassword(identity, "password123", "brandNew456");

        Account changed = accountRepository.findById(account.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("brandNew456", changed.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches("password123", changed.getPasswordHash())).isFalse();
    }

    private CustomerRegistrationForm registrationForm(String email) {
        CustomerRegistrationForm form = new CustomerRegistrationForm();
        form.setEmail(email);
        form.setPassword("password123");
        form.setConfirmPassword("password123");
        form.setFullName("Nguyễn Văn A");
        form.setPhone("0901234567");
        return form;
    }
}
