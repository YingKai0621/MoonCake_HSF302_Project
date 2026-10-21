package com.se201411.project.mooncake.security.service;

import com.se201411.project.mooncake.entity.Account;
import com.se201411.project.mooncake.entity.Customer;
import com.se201411.project.mooncake.enums.AccountRole;
import com.se201411.project.mooncake.enums.AccountStatus;
import com.se201411.project.mooncake.security.auth.MoonCakeUserPrincipal;
import com.se201411.project.mooncake.security.repository.AccountRepository;
import com.se201411.project.mooncake.security.repository.CustomerRepository;
import com.se201411.project.mooncake.security.service.impl.DatabaseUserDetailsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatabaseUserDetailsServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Test
    void loadsCustomerPrincipalByNormalizedEmail() throws Exception {
        Account account = account(11L, AccountRole.CUSTOMER, AccountStatus.ACTIVE);
        Customer customer = customer(21L, account);
        when(accountRepository.findByEmail("customer@example.com"))
            .thenReturn(Optional.of(account));
        when(customerRepository.findByAccount_Id(11L)).thenReturn(Optional.of(customer));

        DatabaseUserDetailsService service = service();
        MoonCakeUserPrincipal principal = (MoonCakeUserPrincipal) service
            .loadUserByUsername(" Customer@Example.COM ");

        assertThat(principal.getAccountId()).isEqualTo(11L);
        assertThat(principal.getCustomerId()).isEqualTo(21L);
        assertThat(principal.getUsername()).isEqualTo("customer@example.com");
        assertThat(principal.isEnabled()).isTrue();
        assertThat(principal.getAuthorities())
            .extracting("authority")
            .containsExactly("ROLE_CUSTOMER");
    }

    @Test
    void loadsAdminWithoutCustomerProfile() throws Exception {
        Account account = account(12L, AccountRole.ADMIN, AccountStatus.ACTIVE);
        when(accountRepository.findByEmail("admin@example.com"))
            .thenReturn(Optional.of(account));

        DatabaseUserDetailsService service = service();
        MoonCakeUserPrincipal principal = (MoonCakeUserPrincipal) service
            .loadUserByUsername("admin@example.com");

        assertThat(principal.getCustomerId()).isNull();
        assertThat(principal.getAuthorities())
            .extracting("authority")
            .containsExactly("ROLE_ADMIN");
        verify(customerRepository, never()).findByAccount_Id(12L);
    }

    @Test
    void rejectsUnknownEmail() {
        when(accountRepository.findByEmail("missing@example.com"))
            .thenReturn(Optional.empty());

        DatabaseUserDetailsService service = service();

        assertThatThrownBy(() -> service.loadUserByUsername("missing@example.com"))
            .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void marksDisabledAccountAsDisabled() throws Exception {
        Account account = account(13L, AccountRole.ADMIN, AccountStatus.DISABLED);
        when(accountRepository.findByEmail("admin@example.com"))
            .thenReturn(Optional.of(account));

        DatabaseUserDetailsService service = service();
        MoonCakeUserPrincipal principal = (MoonCakeUserPrincipal) service
            .loadUserByUsername("admin@example.com");

        assertThat(principal.isEnabled()).isFalse();
    }

    private DatabaseUserDetailsService service() {
        return new DatabaseUserDetailsService(accountRepository, customerRepository);
    }

    private Account account(long id, AccountRole role, AccountStatus status) throws Exception {
        Account account = new Account();
        setId(account, "id", id);
        account.setEmail(role == AccountRole.ADMIN ? "admin@example.com" : "customer@example.com");
        account.setPasswordHash("encoded-password");
        account.setRole(role);
        account.setStatus(status);
        return account;
    }

    private Customer customer(long id, Account account) throws Exception {
        Customer customer = new Customer();
        setId(customer, "id", id);
        customer.setAccount(account);
        customer.setFullName("Nguyễn Văn A");
        customer.setPhone("0901234567");
        return customer;
    }

    private void setId(Object target, String fieldName, long value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
