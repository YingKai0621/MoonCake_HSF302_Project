package com.se201411.project.mooncake.cart.controller;

import com.se201411.project.mooncake.cart.dto.CustomBoxOptionView;
import com.se201411.project.mooncake.cart.dto.CustomBoxPageView;
import com.se201411.project.mooncake.cart.dto.CustomCakeOptionView;
import com.se201411.project.mooncake.cart.dto.CustomCakeQuantity;
import com.se201411.project.mooncake.cart.dto.SaveCustomBoxCommand;
import com.se201411.project.mooncake.cart.service.CustomBoxService;
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
class CustomBoxControllerTest {

    private static final ShopperIdentity CUSTOMER = ShopperIdentity.forCustomer(10L);

    @Mock
    private CustomBoxService customBoxService;
    @Mock
    private CurrentCustomerService currentCustomerService;

    private MockMvc mockMvc;
    private CustomBoxPageView builder;

    @BeforeEach
    void setUp() {
        CustomBoxController controller = new CustomBoxController(
            customBoxService,
            currentCustomerService
        );
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/views/");
        viewResolver.setSuffix(".html");
        mockMvc = MockMvcBuilders
            .standaloneSetup(controller)
            .setViewResolvers(viewResolver)
            .build();

        builder = new CustomBoxPageView(
            List.of(new CustomBoxOptionView(
                20L,
                30L,
                "Vỏ hộp 4 bánh",
                "BOX-4",
                4,
                BigDecimal.valueOf(495_000),
                "495.000đ"
            )),
            List.of(new CustomCakeOptionView(
                40L,
                "Bánh hạt sen",
                "Bánh 120g",
                "MC-40",
                BigDecimal.valueOf(135_000),
                "135.000đ",
                "/images/cake.png",
                "Bánh hạt sen"
            ))
        );
    }

    @Test
    void rendersBuilderForAuthenticatedCustomer() throws Exception {
        when(currentCustomerService.requireCurrentCustomer()).thenReturn(CUSTOMER);
        when(customBoxService.getBuilderView()).thenReturn(builder);

        mockMvc.perform(get("/custom-boxes"))
            .andExpect(status().isOk())
            .andExpect(view().name("custom-box"))
            .andExpect(model().attribute("builder", builder))
            .andExpect(model().attributeExists("customBoxForm"));
    }

    @Test
    void validFormCreatesCustomBoxAndRedirectsToCart() throws Exception {
        when(currentCustomerService.requireCurrentCustomer()).thenReturn(CUSTOMER);

        mockMvc.perform(
            post("/custom-boxes")
                .param("boxId", "20")
                .param("boxQuantity", "2")
                .param("cakes[0].variantId", "40")
                .param("cakes[0].quantity", "4")
        )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/cart"))
            .andExpect(flash().attributeExists("successMessage"));

        verify(customBoxService).createAndAddToCart(
            CUSTOMER,
            new SaveCustomBoxCommand(
                20L,
                2,
                List.of(new CustomCakeQuantity(40L, 4))
            )
        );
    }

    @Test
    void invalidFormDoesNotCreateConfiguration() throws Exception {
        when(currentCustomerService.requireCurrentCustomer()).thenReturn(CUSTOMER);
        when(customBoxService.getBuilderView()).thenReturn(builder);

        mockMvc.perform(
            post("/custom-boxes")
                .param("boxId", "20")
                .param("boxQuantity", "0")
                .param("cakes[0].variantId", "40")
                .param("cakes[0].quantity", "4")
        )
            .andExpect(status().isOk())
            .andExpect(view().name("custom-box"))
            .andExpect(model().attributeHasFieldErrors("customBoxForm", "boxQuantity"));

        verify(customBoxService, never()).createAndAddToCart(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any()
        );
    }
}
