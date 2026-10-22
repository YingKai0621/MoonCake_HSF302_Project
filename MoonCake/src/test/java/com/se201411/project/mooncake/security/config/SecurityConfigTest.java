package com.se201411.project.mooncake.security.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = {
    SecurityConfig.class,
    SecurityConfigTest.TestWebConfig.class
})
class SecurityConfigTest {

    @Autowired
    private WebApplicationContext applicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = webAppContextSetup(applicationContext)
            .apply(springSecurity())
            .build();
    }

    @Test
    void allowsPublicCatalogWithoutLogin() throws Exception {
        mockMvc.perform(get("/"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/products/mooncakes"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/business"))
            .andExpect(status().isOk());
    }

    @Test
    void redirectsAnonymousCustomerRouteToLogin() throws Exception {
        mockMvc.perform(get("/cart"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"));

        mockMvc.perform(get("/custom-boxes"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"));

        mockMvc.perform(get("/checkout"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"));

        mockMvc.perform(get("/orders"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"));

        mockMvc.perform(get("/orders/MC-TEST"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"));
    }

    @Test
    void customerCanUseCustomerRouteButNotAdminRoute() throws Exception {
        mockMvc.perform(get("/cart").with(user("customer").roles("CUSTOMER")))
            .andExpect(status().isOk());

        mockMvc.perform(get("/orders/MC-TEST/success")
                .with(user("customer").roles("CUSTOMER")))
            .andExpect(status().isOk());

        mockMvc.perform(get("/orders").with(user("customer").roles("CUSTOMER")))
            .andExpect(status().isOk());

        mockMvc.perform(get("/orders/MC-TEST").with(user("customer").roles("CUSTOMER")))
            .andExpect(status().isOk());

        mockMvc.perform(get("/admin/dashboard").with(user("customer").roles("CUSTOMER")))
            .andExpect(status().isForbidden());
    }

    @Test
    void adminCanUseAdminRouteButNotCustomerRoute() throws Exception {
        mockMvc.perform(get("/admin/dashboard").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk());

        mockMvc.perform(get("/cart").with(user("admin").roles("ADMIN")))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/orders").with(user("admin").roles("ADMIN")))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/orders/MC-TEST").with(user("admin").roles("ADMIN")))
            .andExpect(status().isForbidden());
    }

    @Test
    void requiresCsrfForCustomerPost() throws Exception {
        mockMvc.perform(post("/cart/items").with(user("customer").roles("CUSTOMER")))
            .andExpect(status().isForbidden());

        mockMvc.perform(
            post("/cart/items")
                .with(user("customer").roles("CUSTOMER"))
                .with(csrf())
        ).andExpect(status().isOk());
    }

    @Test
    void requiresCustomerRoleAndCsrfForOrderCancellation() throws Exception {
        mockMvc.perform(post("/orders/MC-TEST/cancel"))
            .andExpect(status().isForbidden());

        mockMvc.perform(
            post("/orders/MC-TEST/cancel")
                .with(user("customer").roles("CUSTOMER"))
        ).andExpect(status().isForbidden());

        mockMvc.perform(
            post("/orders/MC-TEST/cancel")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
        ).andExpect(status().isForbidden());

        mockMvc.perform(
            post("/orders/MC-TEST/cancel")
                .with(user("customer").roles("CUSTOMER"))
                .with(csrf())
        ).andExpect(status().isOk());
    }

    @Test
    void authenticatesWithEmailAndPassword() throws Exception {
        mockMvc.perform(
            formLogin()
                .user("email", "customer@example.com")
                .password("password123")
        ).andExpect(authenticated().withUsername("customer@example.com"));
    }

    @Test
    void rejectsDisabledAccount() throws Exception {
        mockMvc.perform(
            formLogin()
                .user("email", "disabled@example.com")
                .password("password123")
        ).andExpect(unauthenticated());
    }

    @Test
    void requiresCsrfForRegistrationAndLogout() throws Exception {
        mockMvc.perform(post("/register"))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/register").with(csrf()))
            .andExpect(status().isOk());

        mockMvc.perform(
            post("/logout").with(user("customer").roles("CUSTOMER"))
        ).andExpect(status().isForbidden());

        mockMvc.perform(
            post("/logout")
                .with(user("customer").roles("CUSTOMER"))
                .with(csrf())
        ).andExpect(status().is3xxRedirection());
    }

    @Configuration
    @EnableWebMvc
    static class TestWebConfig {

        @Bean
        TestRouteController testRouteController() {
            return new TestRouteController();
        }

        @Bean
        UserDetailsService testUserDetailsService(PasswordEncoder passwordEncoder) {
            UserDetails customer = User.withUsername("customer@example.com")
                .password(passwordEncoder.encode("password123"))
                .roles("CUSTOMER")
                .build();
            UserDetails disabled = User.withUsername("disabled@example.com")
                .password(passwordEncoder.encode("password123"))
                .roles("CUSTOMER")
                .disabled(true)
                .build();
            return new InMemoryUserDetailsManager(customer, disabled);
        }
    }

    @RestController
    static class TestRouteController {

        @GetMapping("/")
        String homepage() {
            return "home";
        }

        @GetMapping("/products/mooncakes")
        String products() {
            return "products";
        }

        @GetMapping("/business")
        String business() {
            return "business";
        }

        @GetMapping("/cart")
        String cart() {
            return "cart";
        }

        @GetMapping("/custom-boxes")
        String customBoxes() {
            return "custom-boxes";
        }

        @GetMapping("/checkout")
        String checkout() {
            return "checkout";
        }

        @GetMapping("/orders/{orderCode}/success")
        String orderSuccess() {
            return "order-success";
        }

        @GetMapping("/orders")
        String orders() {
            return "orders";
        }

        @GetMapping("/orders/{orderCode}")
        String orderDetail() {
            return "order-detail";
        }

        @PostMapping("/cart/items")
        String addCartItem() {
            return "added";
        }

        @PostMapping("/orders/{orderCode}/cancel")
        String cancelOrder() {
            return "cancelled";
        }

        @PostMapping("/register")
        String register() {
            return "registered";
        }

        @GetMapping("/admin/dashboard")
        String admin() {
            return "admin";
        }
    }
}
