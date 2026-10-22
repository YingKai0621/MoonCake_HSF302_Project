package com.se201411.project.mooncake.order.controller;

import com.se201411.project.mooncake.common.exception.ResourceNotFoundException;
import com.se201411.project.mooncake.order.dto.CustomerOrderDetailView;
import com.se201411.project.mooncake.order.dto.CustomerOrderSummaryView;
import com.se201411.project.mooncake.order.service.CustomerOrderService;
import com.se201411.project.mooncake.order.service.OrderCancellationService;
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

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(MockitoExtension.class)
class CustomerOrderControllerTest {

    private static final ShopperIdentity CUSTOMER = ShopperIdentity.forCustomer(7L);

    @Mock
    private CustomerOrderService customerOrderService;
    @Mock
    private CurrentCustomerService currentCustomerService;
    @Mock
    private OrderCancellationService orderCancellationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        CustomerOrderController controller = new CustomerOrderController(
            customerOrderService,
            currentCustomerService,
            orderCancellationService
        );
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/views/");
        viewResolver.setSuffix(".html");
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setViewResolvers(viewResolver)
            .build();
    }

    @Test
    void rendersOwnedOrderList() throws Exception {
        List<CustomerOrderSummaryView> orders = List.of(new CustomerOrderSummaryView(
            "MC-20261021-TEST",
            "Đã tiếp nhận",
            "21/10/2026 10:00",
            "620.000đ"
        ));
        when(currentCustomerService.requireCurrentCustomer()).thenReturn(CUSTOMER);
        when(customerOrderService.findOrders(CUSTOMER)).thenReturn(orders);

        mockMvc.perform(get("/orders"))
            .andExpect(status().isOk())
            .andExpect(view().name("orders"))
            .andExpect(model().attribute("orders", orders));
    }

    @Test
    void rendersOwnedOrderDetail() throws Exception {
        CustomerOrderDetailView order = new CustomerOrderDetailView(
            "MC-20261021-TEST",
            "Đã tiếp nhận",
            true,
            "21/10/2026 10:00",
            "Người nhận",
            "0901234567",
            "Địa chỉ",
            null,
            "Chưa thanh toán",
            List.of(),
            0,
            "590.000đ",
            "0đ",
            "30.000đ",
            "620.000đ"
        );
        when(currentCustomerService.requireCurrentCustomer()).thenReturn(CUSTOMER);
        when(customerOrderService.requireOrder("MC-20261021-TEST", CUSTOMER))
            .thenReturn(order);

        mockMvc.perform(get("/orders/MC-20261021-TEST"))
            .andExpect(status().isOk())
            .andExpect(view().name("order-detail"))
            .andExpect(model().attribute("order", order));
    }

    @Test
    void cancelsOwnedPlacedOrderAndRedirectsToDetail() throws Exception {
        mockMvc.perform(post("/orders/MC-20261021-TEST/cancel"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/orders/MC-20261021-TEST"))
            .andExpect(flash().attribute(
                "successMessage",
                "Đã hủy đơn hàng thành công."
            ));

        verify(orderCancellationService)
            .cancelCurrentCustomerOrder("MC-20261021-TEST");
    }

    @Test
    void returnsNotFoundForAnotherCustomersOrder() throws Exception {
        when(currentCustomerService.requireCurrentCustomer()).thenReturn(CUSTOMER);
        when(customerOrderService.requireOrder("MC-OTHER", CUSTOMER))
            .thenThrow(new ResourceNotFoundException("Không tìm thấy đơn hàng của bạn"));

        mockMvc.perform(get("/orders/MC-OTHER"))
            .andExpect(status().isNotFound());
    }
}
