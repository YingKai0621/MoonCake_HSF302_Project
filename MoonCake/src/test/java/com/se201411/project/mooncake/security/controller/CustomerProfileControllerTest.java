package com.se201411.project.mooncake.security.controller;

import com.se201411.project.mooncake.security.dto.CustomerProfile;
import com.se201411.project.mooncake.security.dto.ShopperIdentity;
import com.se201411.project.mooncake.security.service.CurrentCustomerService;
import com.se201411.project.mooncake.security.service.CustomerProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerProfileControllerTest {

    private final CurrentCustomerService currentCustomerService = mock(CurrentCustomerService.class);
    private final CustomerProfileService customerProfileService = mock(CustomerProfileService.class);
    private final CustomerProfileController controller = new CustomerProfileController(
        currentCustomerService,
        customerProfileService
    );

    @Test
    void showsOnlyCurrentCustomersProfileFields() {
        ShopperIdentity identity = new ShopperIdentity(7L);
        CustomerProfile profile = new CustomerProfile(
            7L,
            "Nguyễn Văn A",
            "0901234567",
            "customer@example.com"
        );
        ExtendedModelMap model = new ExtendedModelMap();
        when(currentCustomerService.requireCurrentCustomer()).thenReturn(identity);
        when(customerProfileService.requireProfile(identity)).thenReturn(profile);

        String view = controller.showProfile(model);

        assertThat(view).isEqualTo("security/profile");
        assertThat(model.get("profile")).isEqualTo(profile);
        assertThat(model).hasSize(1);
        verify(customerProfileService).requireProfile(identity);
    }
}
