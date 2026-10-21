package com.se201411.project.mooncake.order.service;

import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import com.se201411.project.mooncake.order.service.impl.DeliveryAddressServiceImpl;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeliveryAddressServiceImplTest {

    private final DeliveryAddressService service = new DeliveryAddressServiceImpl(
        new ObjectMapper()
    );

    @Test
    void buildsAddressFromAValidHierarchy() {
        String address = service.buildShippingAddress(
            "123 Đường Trung Thu",
            "26740",
            "79"
        );

        assertThat(address)
            .isEqualTo("123 Đường Trung Thu, Sài Gòn, Hồ Chí Minh");
    }

    @Test
    void rejectsWardFromAnotherProvince() {
        assertThatThrownBy(() -> service.buildShippingAddress(
            "123 Đường Trung Thu",
            "00004",
            "79"
        ))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessage("Địa chỉ giao hàng không hợp lệ");
    }

    @Test
    void loadsTheCurrentTwoLevelAdministrativeDataset() {
        assertThat(service.getProvinces()).hasSize(34);
        assertThat(service.getWards()).hasSize(3321);
    }
}
