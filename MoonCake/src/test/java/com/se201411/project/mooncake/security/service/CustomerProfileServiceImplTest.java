package com.se201411.project.mooncake.security.service;

import com.se201411.project.mooncake.common.exception.ResourceNotFoundException;
import com.se201411.project.mooncake.entity.Customer;
import com.se201411.project.mooncake.security.dto.ProfileUpdateForm;
import com.se201411.project.mooncake.security.dto.ShopperIdentity;
import com.se201411.project.mooncake.security.repository.CustomerRepository;
import com.se201411.project.mooncake.security.service.impl.CustomerProfileServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerProfileServiceImplTest {

    @Mock
    private CustomerRepository customerRepository;

    @Test
    void updatesNameAndPhoneOfTheSignedInCustomer() {
        Customer customer = new Customer();
        customer.setFullName("Tên cũ");
        customer.setPhone("0901111111");
        when(customerRepository.findById(7L)).thenReturn(Optional.of(customer));
        ProfileUpdateForm form = new ProfileUpdateForm();
        form.setFullName("  Trần Thị B  ");
        form.setPhone("0912345678");

        service().updateProfile(ShopperIdentity.forCustomer(7L), form);

        assertThat(customer.getFullName()).isEqualTo("Trần Thị B");
        assertThat(customer.getPhone()).isEqualTo("0912345678");
        verify(customerRepository).save(customer);
    }

    @Test
    void missingCustomerIsReported() {
        when(customerRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().accountPage(ShopperIdentity.forCustomer(7L)))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Không tìm thấy thông tin khách hàng");
    }

    private CustomerProfileServiceImpl service() {
        return new CustomerProfileServiceImpl(customerRepository);
    }
}
