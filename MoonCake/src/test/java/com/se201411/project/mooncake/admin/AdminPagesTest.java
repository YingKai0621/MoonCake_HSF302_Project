package com.se201411.project.mooncake.admin;

import com.se201411.project.mooncake.product.controller.AdminCatalogController;
import com.se201411.project.mooncake.inventory.controller.AdminInventoryController;
import com.se201411.project.mooncake.order.controller.AdminOrderController;
import com.se201411.project.mooncake.product.service.*;
import com.se201411.project.mooncake.inventory.service.*;
import com.se201411.project.mooncake.order.service.OrderManagementService;
import com.se201411.project.mooncake.order.service.OrderCancellationService;
import com.se201411.project.mooncake.product.dto.*;
import com.se201411.project.mooncake.product.dto.AdminCatalogForms.*;
import com.se201411.project.mooncake.inventory.dto.AdminInventoryView.*;
import com.se201411.project.mooncake.order.dto.AdminOrderView;
import com.se201411.project.mooncake.enums.*;
import com.se201411.project.mooncake.common.exception.AdminExceptionAdvice;
import com.se201411.project.mooncake.security.config.SecurityConfig;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = {AdminPagesTest.Config.class, SecurityConfig.class})
class AdminPagesTest {
    @Autowired
    WebApplicationContext context;
    @Autowired
    ProductAdminService catalog;
    @Autowired
    PresetConfigurationAdminService presets;
    @Autowired
    InventoryAdminService inventory;
    @Autowired
    OrderManagementService orders;
    @Autowired
    OrderCancellationService cancellations;
    MockMvc mvc;

    @BeforeEach
    void setup() {
        reset(catalog, presets, inventory, orders, cancellations);
        mvc = webAppContextSetup(context).apply(springSecurity()).build();
        when(catalog.catalog()).thenReturn(new AdminCatalogView(
                List.of(new CategoryForm(1L, "Category", "category", true)),
                List.of(new ProductForm(1L, 1, ProductType.MOONCAKE, "Cake", "cake", "Description", null, null, null, null, true)),
                List.of(new VariantForm(1L, 1, "SKU", "Cake", 100, null, BigDecimal.TEN, true)),
                List.of(new BoxForm(1L, 1, 4, null, null, true)),
                List.of(new ImageForm(1L, 1, "/images/cake.png", "Cake", 0, true))));
        when(presets.presets()).thenReturn(List.of(
                new AdminPresetView(1, 1, "GIFT", 1, 4, ConfigurationStatus.DRAFT, List.of(new AdminPresetView.Component(1, "CAKE", 4))),
                new AdminPresetView(2, 2, "GIFT-READY", 1, 4, ConfigurationStatus.READY, List.of())));
        when(inventory.balances()).thenReturn(List.of(new Balance(1, "SKU", "Cake", 3, 5)));
        when(inventory.movements(0)).thenReturn(List.of(new Movement("SKU", StockMovementType.RECEIVE, 3, "Receive", "GROUP", 1L, LocalDateTime.now())));
        when(orders.orders(null, 0)).thenReturn(List.of(order(OrderStatus.PLACED)));
        when(orders.detail(1)).thenReturn(order(OrderStatus.PLACED));
    }

    @Test
    void rendersAllPagesWithRealThymeleafAndCsrf() throws Exception {
        for (String path : List.of("/admin/catalog", "/admin/presets", "/admin/inventory", "/admin/orders", "/admin/orders/1"))
            mvc.perform(get(path).with(user("admin").roles("ADMIN")))
                    .andExpect(status().isOk()).andExpect(content().string(containsString("_csrf")));
    }

    @Test
    void catalogRendersMarkersForUncheckedCheckboxes() throws Exception {
        mvc.perform(get("/admin/catalog").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("name=\"_active\"")))
            .andExpect(content().string(containsString("name=\"_primary\"")));
    }

    @Test
    void anonymousAndCustomerCannotReadAdminPages() throws Exception {
        mvc.perform(get("/admin/catalog")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/admin/catalog").with(user("customer").roles("CUSTOMER"))).andExpect(status().isForbidden());
        verifyNoInteractions(catalog);
    }

    @Test
    void postsRequireAdminAndCsrf() throws Exception {
        mvc.perform(post("/admin/orders/1/status").param("target", "PROCESSING").with(user("admin").roles("ADMIN"))).andExpect(status().isForbidden());
        mvc.perform(post("/admin/orders/1/status").param("target", "PROCESSING").with(csrf()).with(user("customer").roles("CUSTOMER"))).andExpect(status().isForbidden());
        verifyNoInteractions(orders);
    }

    @Test
    void cancellationPostUsesSharedCancellationService() throws Exception {
        mvc.perform(post("/admin/orders/1/status")
                .param("target", "CANCELLED")
                .with(user("admin").roles("ADMIN"))
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/orders/1"));

        verify(cancellations).cancelForAdmin(1L);
        verify(orders, never()).transition(1L, OrderStatus.CANCELLED);
    }

    @Test
    void bindsRecordFormAndRejectsInvalidInput() throws Exception {
        mvc.perform(post("/admin/categories").with(user("admin").roles("ADMIN")).with(csrf())
                        .param("name", "New Category").param("slug", "new-category").param("active", "true"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin/catalog#categories"));
        verify(catalog).saveCategory(new CategoryForm(null, "New Category", "new-category", true));
        mvc.perform(post("/admin/categories").with(user("admin").roles("ADMIN")).with(csrf())
                .param("name", "").param("slug", "bad slug")).andExpect(status().isBadRequest());
        verifyNoMoreInteractions(catalog);
    }

    @Test
    void uncheckedCatalogCheckboxesBindAsFalse() throws Exception {
        mvc.perform(post("/admin/categories")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .param("name", "Inactive Category")
                .param("slug", "inactive-category")
                .param("_active", "on"))
            .andExpect(status().is3xxRedirection());
        verify(catalog).saveCategory(new CategoryForm(
            null,
            "Inactive Category",
            "inactive-category",
            false
        ));

        mvc.perform(post("/admin/products")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .param("categoryId", "1")
                .param("productType", "MOONCAKE")
                .param("name", "Inactive Product")
                .param("slug", "inactive-product")
                .param("shortDescription", "Description")
                .param("_active", "on"))
            .andExpect(status().is3xxRedirection());
        verify(catalog).saveProduct(new ProductForm(
            null,
            1,
            ProductType.MOONCAKE,
            "Inactive Product",
            "inactive-product",
            "Description",
            null,
            null,
            null,
            null,
            false
        ));

        mvc.perform(post("/admin/variants")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .param("productId", "1")
                .param("sku", "INACTIVE-SKU")
                .param("name", "Inactive Variant")
                .param("unitPrice", "100000")
                .param("_active", "on"))
            .andExpect(status().is3xxRedirection());
        verify(catalog).saveVariant(new VariantForm(
            null,
            1,
            "INACTIVE-SKU",
            "Inactive Variant",
            null,
            null,
            BigDecimal.valueOf(100_000),
            false
        ));

        mvc.perform(post("/admin/boxes")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .param("variantId", "1")
                .param("capacityCakes", "4")
                .param("_active", "on"))
            .andExpect(status().is3xxRedirection());
        verify(catalog).saveBox(new BoxForm(null, 1, 4, null, null, false));

        mvc.perform(post("/admin/images")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .param("variantId", "1")
                .param("imageUrl", "/images/cake.png")
                .param("sortOrder", "0")
                .param("_primary", "on"))
            .andExpect(status().is3xxRedirection());
        verify(catalog).saveImage(new ImageForm(
            null,
            1,
            "/images/cake.png",
            null,
            0,
            false
        ));
    }

    @Test
    void shippingShowsOnlyDeliveryAction() throws Exception {
        when(orders.detail(1)).thenReturn(order(OrderStatus.SHIPPING));
        mvc.perform(get("/admin/orders/1").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(content().string(containsString("value=\"COMPLETED\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("value=\"CANCELLED\""))));
    }

    static AdminOrderView order(OrderStatus status) {
        return new AdminOrderView(1, "ORDER", status, "Recipient", "0901234567", "Address", null,
                BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.TEN, LocalDateTime.now(),
                List.of(new AdminOrderView.Item("Cake", "SKU", 1, BigDecimal.TEN, BigDecimal.TEN, List.of(new AdminOrderView.Component("Cake", 1, ComponentRole.CAKE)))),
                List.of(new AdminOrderView.Cod(PaymentStatus.UNPAID, BigDecimal.TEN, null)));
    }

    @Configuration
    @EnableWebMvc
    @Import({AdminCatalogController.class, AdminInventoryController.class, AdminOrderController.class, AdminExceptionAdvice.class})
    static class Config {
        @Bean
        ProductAdminService catalog() {
            return mock(ProductAdminService.class);
        }

        @Bean
        PresetConfigurationAdminService presets() {
            return mock(PresetConfigurationAdminService.class);
        }

        @Bean
        InventoryAdminService inventory() {
            return mock(InventoryAdminService.class);
        }

        @Bean
        PackingService packing() {
            return mock(PackingService.class);
        }

        @Bean
        OrderManagementService orders() {
            return mock(OrderManagementService.class);
        }

        @Bean
        OrderCancellationService cancellations() {
            return mock(OrderCancellationService.class);
        }

        @Bean
        SpringTemplateEngine engine() {
            var resolver = new ClassLoaderTemplateResolver();
            resolver.setPrefix("templates/");
            resolver.setSuffix(".html");
            resolver.setCharacterEncoding("UTF-8");
            var engine = new SpringTemplateEngine();
            engine.setTemplateResolver(resolver);
            return engine;
        }

        @Bean
        ThymeleafViewResolver viewResolver(SpringTemplateEngine engine) {
            var resolver = new ThymeleafViewResolver();
            resolver.setTemplateEngine(engine);
            resolver.setCharacterEncoding("UTF-8");
            return resolver;
        }
    }
}
