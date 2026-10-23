package com.se201411.project.mooncake.order.controller;

import com.se201411.project.mooncake.cart.dto.CheckoutQuote;
import com.se201411.project.mooncake.order.dto.CheckoutPreparation;
import com.se201411.project.mooncake.order.dto.CheckoutResult;
import com.se201411.project.mooncake.order.service.CheckoutService;
import com.se201411.project.mooncake.order.service.DeliveryAddressService;
import com.se201411.project.mooncake.security.dto.CustomerProfile;
import com.se201411.project.mooncake.security.dto.ShopperIdentity;
import com.se201411.project.mooncake.security.service.CurrentCustomerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(MockitoExtension.class)
class CheckoutControllerTest {

    private static final ShopperIdentity CUSTOMER = ShopperIdentity.forCustomer(7L);

    @Mock
    private CheckoutService checkoutService;
    @Mock
    private CurrentCustomerService currentCustomerService;
    @Mock
    private DeliveryAddressService deliveryAddressService;

    private MockMvc mockMvc;
    private CheckoutPreparation preparation;

    @BeforeEach
    void setUp() {
        CheckoutController controller = new CheckoutController(
            checkoutService,
            currentCustomerService,
            deliveryAddressService
        );
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/views/");
        viewResolver.setSuffix(".html");
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setViewResolvers(viewResolver)
            .build();

        preparation = new CheckoutPreparation(
            9L,
            new CustomerProfile(7L, "Khách hàng", "0901234567", "test@example.com"),
            new CheckoutQuote(
                List.of(),
                List.of(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.valueOf(30_000),
                BigDecimal.ZERO,
                BigDecimal.valueOf(30_000)
            )
        );
    }

    @Test
    void rendersCheckoutWithCustomerDefaults() throws Exception {
        when(currentCustomerService.requireCurrentCustomer()).thenReturn(CUSTOMER);
        when(checkoutService.prepareCheckout(CUSTOMER)).thenReturn(preparation);

        mockMvc.perform(get("/checkout"))
            .andExpect(status().isOk())
            .andExpect(view().name("checkout"))
            .andExpect(model().attributeExists(
                "checkout",
                "checkoutForm",
                "customer",
                "provinces",
                "wards"
            ));
    }

    @Test
    void validFormPlacesOrderAndRedirectsToOwnedSuccessRoute() throws Exception {
        when(currentCustomerService.requireCurrentCustomer()).thenReturn(CUSTOMER);
        when(checkoutService.placeOrder(any(), any())).thenReturn(
            new CheckoutResult("MC-20261021-ABC12345")
        );

        mockMvc.perform(post("/checkout")
                .param("cartId", "9")
                .param("recipientName", "Người nhận")
                .param("recipientPhone", "0901234567")
                .param("provinceCode", "79")
                .param("wardCode", "26740")
                .param("streetAddress", "123 Đường Trung Thu"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/orders/MC-20261021-ABC12345/success"));
    }

    @Test
    void invalidPhoneRendersFormWithoutPlacingOrder() throws Exception {
        when(currentCustomerService.requireCurrentCustomer()).thenReturn(CUSTOMER);
        when(checkoutService.prepareCheckout(9L, CUSTOMER)).thenReturn(preparation);

        mockMvc.perform(post("/checkout")
                .param("cartId", "9")
                .param("recipientName", "Người nhận")
                .param("recipientPhone", "1")
                .param("provinceCode", "79")
                .param("wardCode", "26740")
                .param("streetAddress", "123 Đường Trung Thu"))
            .andExpect(status().isOk())
            .andExpect(view().name("checkout"))
            .andExpect(model().attributeHasFieldErrors("checkoutForm", "recipientPhone"));

        verify(checkoutService, never()).placeOrder(any(), any());
    }

    @Test
    void invalidAddressHierarchyRendersFormWithoutPlacingOrder() throws Exception {
        when(currentCustomerService.requireCurrentCustomer()).thenReturn(CUSTOMER);
        when(checkoutService.prepareCheckout(9L, CUSTOMER)).thenReturn(preparation);
        when(deliveryAddressService.buildShippingAddress(
            "123 Đường Trung Thu",
            "00004",
            "79"
        )).thenThrow(new com.se201411.project.mooncake.common.exception.BusinessRuleViolationException(
            "Địa chỉ giao hàng không hợp lệ"
        ));

        mockMvc.perform(post("/checkout")
                .param("cartId", "9")
                .param("recipientName", "Người nhận")
                .param("recipientPhone", "0901234567")
                .param("provinceCode", "79")
                .param("wardCode", "00004")
                .param("streetAddress", "123 Đường Trung Thu"))
            .andExpect(status().isOk())
            .andExpect(view().name("checkout"))
            .andExpect(model().attributeHasFieldErrors("checkoutForm", "wardCode"));

        verify(checkoutService, never()).placeOrder(any(), any());
    }

    @Test
    void invalidRecipientNameRendersFormWithoutPlacingOrder() throws Exception {
        when(currentCustomerService.requireCurrentCustomer()).thenReturn(CUSTOMER);
        when(checkoutService.prepareCheckout(9L, CUSTOMER)).thenReturn(preparation);

        mockMvc.perform(post("/checkout")
                .param("cartId", "9")
                .param("recipientName", "Người nhận 123 #")
                .param("recipientPhone", "0901234567")
                .param("provinceCode", "79")
                .param("wardCode", "26740")
                .param("streetAddress", "123 Đường Trung Thu"))
            .andExpect(status().isOk())
            .andExpect(view().name("checkout"))
            .andExpect(model().attributeHasFieldErrors("checkoutForm", "recipientName"));

        verify(checkoutService, never()).placeOrder(any(), any());
    }

    @Test
    void rejectsStreetAddressAndOrderNoteExceedingLimits() throws Exception {
        when(currentCustomerService.requireCurrentCustomer()).thenReturn(CUSTOMER);
        when(checkoutService.prepareCheckout(9L, CUSTOMER)).thenReturn(preparation);

        mockMvc.perform(post("/checkout")
                .param("cartId", "9")
                .param("recipientName", "Người nhận")
                .param("recipientPhone", "0901234567")
                .param("provinceCode", "79")
                .param("wardCode", "26740")
                .param("streetAddress", "a".repeat(121))
                .param("orderNote", "b".repeat(201)))
            .andExpect(status().isOk())
            .andExpect(view().name("checkout"))
            .andExpect(model().attributeHasFieldErrors("checkoutForm", "streetAddress", "orderNote"));

        verify(checkoutService, never()).placeOrder(any(), any());
    }
}
