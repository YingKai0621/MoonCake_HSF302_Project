package com.se201411.project.mooncake.order.controller;

import com.se201411.project.mooncake.enums.PaymentMethod;
import com.se201411.project.mooncake.order.dto.PaymentReturnResult;
import com.se201411.project.mooncake.order.service.CustomerOrderService;
import com.se201411.project.mooncake.order.service.OrderCancellationService;
import com.se201411.project.mooncake.order.service.PaymentProcessor;
import com.se201411.project.mooncake.order.service.PaymentProcessorRegistry;
import com.se201411.project.mooncake.security.service.CurrentCustomerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

class VnpayControllerTest {

    private final PaymentProcessor vnpay = mock(PaymentProcessor.class);
    private final OrderCancellationService orderCancellationService = mock(OrderCancellationService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        PaymentProcessorRegistry registry = mock(PaymentProcessorRegistry.class);
        when(registry.forMethod(PaymentMethod.VNPAY)).thenReturn(vnpay);
        VnpayController controller = new VnpayController(
            mock(CustomerOrderService.class),
            mock(CurrentCustomerService.class),
            registry,
            orderCancellationService
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void paidReturnCelebratesOnTheOrderPage() throws Exception {
        when(vnpay.handleReturn(anyMap())).thenReturn(new PaymentReturnResult(true, "MC-1", true, true));

        mockMvc.perform(get("/payments/vnpay/return"))
            .andExpect(redirectedUrl("/orders/MC-1"))
            .andExpect(flash().attribute("celebrate", true));
    }

    @Test
    void unpaidReturnDoesNotCelebrate() throws Exception {
        when(vnpay.handleReturn(anyMap())).thenReturn(new PaymentReturnResult(true, "MC-1", false, false));

        mockMvc.perform(get("/payments/vnpay/return"))
            .andExpect(redirectedUrl("/orders/MC-1"))
            .andExpect(flash().attributeCount(1));
    }

    @Test
    void payingAnExpiredOrderCancelsItAndExplainsWhy() throws Exception {
        when(orderCancellationService.cancelOwnOrderIfPaymentExpired("MC-1")).thenReturn(true);

        mockMvc.perform(post("/orders/MC-1/pay-vnpay"))
            .andExpect(redirectedUrl("/orders/MC-1"))
            .andExpect(flash().attribute("errorMessage", "Đơn hàng đã quá thời gian thanh toán nên đã được hủy."));
    }
}
