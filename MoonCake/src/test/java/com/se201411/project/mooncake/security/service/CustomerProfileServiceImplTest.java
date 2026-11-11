package com.se201411.project.mooncake.security.service;

import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import com.se201411.project.mooncake.common.exception.ResourceNotFoundException;
import com.se201411.project.mooncake.entity.Account;
import com.se201411.project.mooncake.entity.Customer;
import com.se201411.project.mooncake.enums.AccountStatus;
import com.se201411.project.mooncake.security.dto.AccountPageView;
import com.se201411.project.mooncake.security.dto.ProfileUpdateForm;
import com.se201411.project.mooncake.security.dto.ShopperIdentity;
import com.se201411.project.mooncake.security.repository.AccountRepository;
import com.se201411.project.mooncake.security.repository.CustomerRepository;
import com.se201411.project.mooncake.security.service.impl.CustomerProfileServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerProfileServiceImplTest {

    private static final ShopperIdentity CUSTOMER = ShopperIdentity.forCustomer(5L);

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private AccountRepository accountRepository;

    @Test
    void updatesNameAndPhoneOfTheSignedInCustomer() {
        Customer customer = customer();
        when(customerRepository.findById(5L)).thenReturn(Optional.of(customer));
        ProfileUpdateForm form = new ProfileUpdateForm();
        form.setFullName("  Trần Thị B  ");
        form.setPhone("0912345678");

        service().updateProfile(CUSTOMER, form);

        assertThat(customer.getFullName()).isEqualTo("Trần Thị B");
        assertThat(customer.getPhone()).isEqualTo("0912345678");
        assertThat(customer.getEmail()).isEqualTo("customer@example.com");
        verify(customerRepository).save(customer);
    }

    @Test
    void missingCustomerIsReported() {
        when(customerRepository.findById(5L)).thenReturn(Optional.empty());
        ProfileUpdateForm form = new ProfileUpdateForm();
        form.setFullName("Trần Thị B");
        form.setPhone("0912345678");

        CustomerProfileServiceImpl service = service();

        assertThatThrownBy(() -> service.updateProfile(CUSTOMER, form))
            .isInstanceOf(ResourceNotFoundException.class);
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void accountPageShowsMemberSinceInVietnamTime() {
        Customer customer = customer();
        customer.setCreatedAt(LocalDateTime.of(2026, 10, 20, 18, 30));
        when(customerRepository.findById(5L)).thenReturn(Optional.of(customer));

        AccountPageView page = service().accountPage(CUSTOMER);

        assertThat(page.email()).isEqualTo("customer@example.com");
        assertThat(page.fullName()).isEqualTo("Nguyễn Văn A");
        assertThat(page.phone()).isEqualTo("0901234567");
        assertThat(page.memberSince()).isEqualTo("21/10/2026");
    }

    @Test
    void profileFormIsPrefilledWithCurrentValues() {
        when(customerRepository.findById(5L)).thenReturn(Optional.of(customer()));

        ProfileUpdateForm form = service().profileForm(CUSTOMER);

        assertThat(form.getFullName()).isEqualTo("Nguyễn Văn A");
        assertThat(form.getPhone()).isEqualTo("0901234567");
    }

    @Test
    void requireActiveAccountRejectsDisabledAccount() {
        Customer customer = customer();
        Account account = new Account();
        account.setStatus(AccountStatus.DISABLED);
        customer.setAccount(account);
        when(customerRepository.findById(5L)).thenReturn(Optional.of(customer));

        CustomerProfileServiceImpl service = service();

        assertThatThrownBy(() -> service.requireActiveAccount(CUSTOMER))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessage("Tài khoản của bạn đã bị khóa. Vui lòng liên hệ cửa hàng để được hỗ trợ.");
    }

    @Test
    void disabledCustomerIdsWithEmptyInputReturnsEmpty() {
        assertThat(service().disabledCustomerIds(List.of())).isEmpty();

        verifyNoInteractions(customerRepository);
    }

    private CustomerProfileServiceImpl service() {
        return new CustomerProfileServiceImpl(customerRepository, accountRepository);
    }

    private Customer customer() {
        Customer customer = new Customer();
        customer.setFullName("Nguyễn Văn A");
        customer.setPhone("0901234567");
        customer.setEmail("customer@example.com");
        customer.setCreatedAt(LocalDateTime.of(2026, 10, 1, 3, 0));
        return customer;
    }
}
