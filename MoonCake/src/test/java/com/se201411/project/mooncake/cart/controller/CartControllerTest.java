package com.se201411.project.mooncake.cart.controller;

import com.se201411.project.mooncake.cart.dto.CustomerCartView;
import com.se201411.project.mooncake.cart.service.CustomerCartService;
import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
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

import static org.mockito.Mockito.never;
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
class CartControllerTest {

    private static final ShopperIdentity CUSTOMER = ShopperIdentity.forCustomer(10L);

    @Mock
    private CustomerCartService customerCartService;
    @Mock
    private CurrentCustomerService currentCustomerService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        CartController controller = new CartController(
            customerCartService,
            currentCustomerService
        );
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/views/");
        viewResolver.setSuffix(".html");
        mockMvc = MockMvcBuilders
            .standaloneSetup(controller)
            .setViewResolvers(viewResolver)
            .build();
    }

    @Test
    void rendersCurrentCustomerCart() throws Exception {
        CustomerCartView cart = CustomerCartView.empty();
        when(currentCustomerService.requireCurrentCustomer()).thenReturn(CUSTOMER);
        when(customerCartService.getCurrentCart(CUSTOMER)).thenReturn(cart);

        mockMvc.perform(get("/cart"))
            .andExpect(status().isOk())
            .andExpect(view().name("cart"))
            .andExpect(model().attribute("cart", cart));
    }

    @Test
    void validAddUsesAuthenticatedCustomerAndRedirects() throws Exception {
        when(currentCustomerService.requireCurrentCustomer()).thenReturn(CUSTOMER);

        mockMvc.perform(
            post("/cart/items")
                .param("variantId", "20")
                .param("quantity", "2")
        )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/cart"))
            .andExpect(flash().attributeExists("successMessage"));

        verify(customerCartService).addVariant(CUSTOMER, 20L, 2);
    }

    @Test
    void invalidQuantityDoesNotCallService() throws Exception {
        mockMvc.perform(
            post("/cart/items")
                .param("variantId", "20")
                .param("quantity", "0")
        )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/cart"))
            .andExpect(flash().attribute("errorMessage", "Số lượng phải lớn hơn 0"));

        verify(customerCartService, never()).addVariant(CUSTOMER, 20L, 0);
        verify(currentCustomerService, never()).requireCurrentCustomer();
    }

    @Test
    void businessErrorIsShownAfterRedirect() throws Exception {
        when(currentCustomerService.requireCurrentCustomer()).thenReturn(CUSTOMER);
        org.mockito.Mockito.doThrow(
            new BusinessRuleViolationException("Không thể thêm sản phẩm")
        ).when(customerCartService).addVariant(CUSTOMER, 20L, 1);

        mockMvc.perform(
            post("/cart/items")
                .param("variantId", "20")
                .param("quantity", "1")
        )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/cart"))
            .andExpect(flash().attribute("errorMessage", "Không thể thêm sản phẩm"));
    }

    @Test
    void loginContinuationOnlyAllowsLocalProductPage() throws Exception {
        when(currentCustomerService.requireCurrentCustomer()).thenReturn(CUSTOMER);

        mockMvc.perform(
            get("/cart/continue")
                .param("returnUrl", "/products/banh-lava")
        )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/products/banh-lava"));

        mockMvc.perform(
            get("/cart/continue")
                .param("returnUrl", "https://example.com")
        )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/products"));
    }
}
