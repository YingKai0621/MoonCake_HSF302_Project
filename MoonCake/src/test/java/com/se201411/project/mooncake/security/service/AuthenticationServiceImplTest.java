package com.se201411.project.mooncake.security.service;

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
import com.se201411.project.mooncake.security.service.impl.AuthenticationServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceImplTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
        Instant.parse("2026-10-21T10:00:00Z"),
        ZoneOffset.UTC
    );

    private static final ShopperIdentity CUSTOMER = ShopperIdentity.forCustomer(5L);
    private static final Long ACCOUNT_ID = 9L;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void registersActiveCustomerWithNormalizedEmail() {
        CustomerRegistrationForm form = validForm();
        when(accountRepository.existsByEmail("customer@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(accountRepository.save(any(Account.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        AuthenticationServiceImpl service = service();
        service.registerCustomer(form);

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(accountCaptor.capture());
        Account account = accountCaptor.getValue();
        assertThat(account.getEmail()).isEqualTo("customer@example.com");
        assertThat(account.getPasswordHash()).isEqualTo("encoded-password");
        assertThat(account.getRole()).isEqualTo(AccountRole.CUSTOMER);
        assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(account.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 10, 21, 10, 0));

        ArgumentCaptor<Customer> customerCaptor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(customerCaptor.capture());
        Customer customer = customerCaptor.getValue();
        assertThat(customer.getAccount()).isSameAs(account);
        assertThat(customer.getEmail()).isEqualTo("customer@example.com");
        assertThat(customer.getFullName()).isEqualTo("Nguyễn Văn A");
        assertThat(customer.getPhone()).isEqualTo("0901234567");
        assertThat(customer.getCreatedAt()).isEqualTo(account.getCreatedAt());
    }

    @Test
    void rejectsEmailThatAlreadyExists() {
        CustomerRegistrationForm form = validForm();
        when(accountRepository.existsByEmail("customer@example.com")).thenReturn(true);

        AuthenticationServiceImpl service = service();

        assertThatThrownBy(() -> service.registerCustomer(form))
            .isInstanceOf(EmailAlreadyRegisteredException.class)
            .hasMessage("Email này đã được đăng ký");

        verify(accountRepository, never()).save(any(Account.class));
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void translatesConcurrentDuplicateEmailIntoFriendlyError() {
        CustomerRegistrationForm form = validForm();
        when(accountRepository.existsByEmail("customer@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(accountRepository.save(any(Account.class)))
            .thenThrow(new DataIntegrityViolationException("duplicate email"));

        AuthenticationServiceImpl service = service();

        assertThatThrownBy(() -> service.registerCustomer(form))
            .isInstanceOf(EmailAlreadyRegisteredException.class)
            .hasMessage("Email này đã được đăng ký");

        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void wrongCurrentPasswordIsRejectedAndHashUnchanged() {
        Account account = signedInCustomerAccount();
        when(passwordEncoder.matches("wrong-password", "old-hash")).thenReturn(false);

        AuthenticationServiceImpl service = service();

        assertThatThrownBy(() -> service.changePassword(CUSTOMER, "wrong-password", "newPassword1"))
            .isInstanceOf(WrongCurrentPasswordException.class)
            .hasMessage("Mật khẩu hiện tại không đúng.");

        assertThat(account.getPasswordHash()).isEqualTo("old-hash");
        verify(accountRepository, never()).save(any(Account.class));
        verify(passwordResetTokenRepository, never()).invalidateOpenTokens(any(), any());
    }

    @Test
    void newPasswordEqualToCurrentIsRejected() {
        Account account = signedInCustomerAccount();
        when(passwordEncoder.matches("password123", "old-hash")).thenReturn(true);

        AuthenticationServiceImpl service = service();

        assertThatThrownBy(() -> service.changePassword(CUSTOMER, "password123", "password123"))
            .isInstanceOf(SamePasswordException.class)
            .hasMessage("Mật khẩu mới phải khác mật khẩu hiện tại.");

        assertThat(account.getPasswordHash()).isEqualTo("old-hash");
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void changePasswordStoresNewHashAndInvalidatesResetTokens() {
        Account account = signedInCustomerAccount();
        when(passwordEncoder.matches("password123", "old-hash")).thenReturn(true);
        when(passwordEncoder.matches("newPassword1", "old-hash")).thenReturn(false);
        when(passwordEncoder.encode("newPassword1")).thenReturn("new-hash");

        AuthenticationServiceImpl service = service();
        service.changePassword(CUSTOMER, "password123", "newPassword1");

        assertThat(account.getPasswordHash()).isEqualTo("new-hash");
        verify(accountRepository).save(account);
        verify(passwordResetTokenRepository).invalidateOpenTokens(
            ACCOUNT_ID,
            LocalDateTime.of(2026, 10, 21, 10, 0)
        );
    }

    private Account signedInCustomerAccount() {
        Account account = new Account();
        ReflectionTestUtils.setField(account, "id", ACCOUNT_ID);
        account.setPasswordHash("old-hash");
        Customer customer = new Customer();
        customer.setAccount(account);
        when(customerRepository.findById(CUSTOMER.customerId())).thenReturn(Optional.of(customer));
        return account;
    }

    private AuthenticationServiceImpl service() {
        return new AuthenticationServiceImpl(
            accountRepository,
            customerRepository,
            passwordResetTokenRepository,
            passwordEncoder,
            FIXED_CLOCK
        );
    }

    private CustomerRegistrationForm validForm() {
        CustomerRegistrationForm form = new CustomerRegistrationForm();
        form.setEmail("  Customer@Example.COM  ");
        form.setPassword("password123");
        form.setConfirmPassword("password123");
        form.setFullName("  Nguyễn Văn A  ");
        form.setPhone("0901234567");
        return form;
    }
}
