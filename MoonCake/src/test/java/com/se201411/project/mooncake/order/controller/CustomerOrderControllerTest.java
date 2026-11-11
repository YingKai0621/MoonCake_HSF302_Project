package com.se201411.project.mooncake.order.controller;

import com.se201411.project.mooncake.order.dto.OrderFulfilmentView;
import com.se201411.project.mooncake.order.dto.DeliveryEditPageView;
import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import com.se201411.project.mooncake.common.exception.ResourceNotFoundException;
import com.se201411.project.mooncake.order.dto.CustomerOrderDetailView;
import com.se201411.project.mooncake.order.dto.CustomerOrderSummaryView;
import com.se201411.project.mooncake.order.dto.OrderDeliveryUpdateForm;
import com.se201411.project.mooncake.order.service.CustomerOrderService;
import com.se201411.project.mooncake.order.service.DeliveryAddressService;
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

import static org.mockito.Mockito.doThrow;
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
    @Mock
    private DeliveryAddressService deliveryAddressService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        CustomerOrderController controller = new CustomerOrderController(
            customerOrderService,
            currentCustomerService,
            orderCancellationService,
            deliveryAddressService
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
            false,
            "21/10/2026 10:00",
            "620.000đ"
        ));
        when(currentCustomerService.requireCurrentCustomer()).thenReturn(CUSTOMER);
        when(customerOrderService.findOrders(CUSTOMER)).thenReturn(orders);

        mockMvc.perform(get("/orders"))
            .andExpect(status().isOk())
            .andExpect(view().name("order/orders"))
            .andExpect(model().attribute("orders", orders));
    }

    @Test
    void rendersOwnedOrderDetail() throws Exception {
        CustomerOrderDetailView order = new CustomerOrderDetailView(
            "MC-20261021-TEST",
            "Đã tiếp nhận",
            true,
            true,
            false,
            null,
            "21/10/2026 10:00",
            "Người nhận",
            "0901234567",
            "Địa chỉ",
            null,
            "Chưa thanh toán",
            "COD",
            false,
            null,
            List.of(),
            0,
            "590.000đ",
            "0đ",
            "30.000đ",
            null,
            "620.000đ",
            null,
            new OrderFulfilmentView(
                "10:00 21/10/2026",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
            )
        );
        when(currentCustomerService.requireCurrentCustomer()).thenReturn(CUSTOMER);
        when(customerOrderService.requireOrder("MC-20261021-TEST", CUSTOMER))
            .thenReturn(order);

        mockMvc.perform(get("/orders/MC-20261021-TEST"))
            .andExpect(status().isOk())
            .andExpect(view().name("order/order-detail"))
            .andExpect(model().attribute("order", order));
    }

    @Test
    void rendersDeliveryEditFormForOwnedPlacedOrder() throws Exception {
        OrderDeliveryUpdateForm form = deliveryForm();
        when(currentCustomerService.requireCurrentCustomer()).thenReturn(CUSTOMER);
        when(customerOrderService.requireDeliveryUpdateForm(
            "MC-20261021-TEST",
            CUSTOMER
        )).thenReturn(form);
        DeliveryEditPageView page = new DeliveryEditPageView("MC-20261021-TEST", List.of(), List.of());
        when(deliveryAddressService.deliveryEditPage("MC-20261021-TEST")).thenReturn(page);

        mockMvc.perform(get("/orders/MC-20261021-TEST/delivery/edit"))
            .andExpect(status().isOk())
            .andExpect(view().name("order/order-delivery-edit"))
            .andExpect(model().attribute("deliveryForm", form))
            .andExpect(model().attribute("page", page));
    }

    @Test
    void updatesDeliveryInformationAndRedirectsToOrderDetail() throws Exception {
        when(currentCustomerService.requireCurrentCustomer()).thenReturn(CUSTOMER);
        when(deliveryAddressService.buildShippingAddress(
            "25 Nguyễn Huệ",
            "ward-2",
            "province-2"
        )).thenReturn("25 Nguyễn Huệ, Phường Trung Tâm, Đà Nẵng");

        mockMvc.perform(post("/orders/MC-20261021-TEST/delivery")
                .param("recipientName", "Người nhận mới")
                .param("recipientPhone", "0912345678")
                .param("provinceCode", "province-2")
                .param("wardCode", "ward-2")
                .param("streetAddress", "25 Nguyễn Huệ")
                .param("orderNote", "Giao buổi sáng"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/orders/MC-20261021-TEST"))
            .andExpect(flash().attribute(
                "successMessage",
                "Đã cập nhật thông tin nhận hàng."
            ));

        verify(customerOrderService).updateDeliveryInformation(
            org.mockito.ArgumentMatchers.eq("MC-20261021-TEST"),
            org.mockito.ArgumentMatchers.any(OrderDeliveryUpdateForm.class),
            org.mockito.ArgumentMatchers.eq(CUSTOMER)
        );
    }

    @Test
    void receivedRedirectsBackWithMessage() throws Exception {
        when(currentCustomerService.requireCurrentCustomer()).thenReturn(CUSTOMER);

        mockMvc.perform(post("/orders/MC-20261021-TEST/received"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/orders/MC-20261021-TEST"))
            .andExpect(flash().attribute(
                "successMessage",
                "Cảm ơn bạn đã xác nhận nhận hàng."
            ));

        verify(customerOrderService).confirmReceived(
            "MC-20261021-TEST",
            CUSTOMER
        );
    }

    @Test
    void failedCancellationShowsMessageInsteadOfErrorPage() throws Exception {
        doThrow(new BusinessRuleViolationException("Đơn đã được thanh toán qua VNPay."))
            .when(orderCancellationService).cancelCurrentCustomerOrder("MC-20261021-TEST");

        mockMvc.perform(post("/orders/MC-20261021-TEST/cancel"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/orders/MC-20261021-TEST"))
            .andExpect(flash().attribute("errorMessage", "Đơn đã được thanh toán qua VNPay."));
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

    private OrderDeliveryUpdateForm deliveryForm() {
        OrderDeliveryUpdateForm form = new OrderDeliveryUpdateForm();
        form.setRecipientName("Người nhận");
        form.setRecipientPhone("0901234567");
        form.setProvinceCode("province-1");
        form.setWardCode("ward-1");
        form.setStreetAddress("12 Lê Lợi");
        form.setCurrentShippingAddress("12 Lê Lợi, Phường A, Thành phố B");
        return form;
    }
}
