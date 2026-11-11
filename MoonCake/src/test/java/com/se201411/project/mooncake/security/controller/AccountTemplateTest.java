package com.se201411.project.mooncake.security.controller;

import com.se201411.project.mooncake.cart.controller.CartHeaderViewAdvice;
import com.se201411.project.mooncake.cart.service.CustomerCartService;
import com.se201411.project.mooncake.enums.AccountRole;
import com.se201411.project.mooncake.enums.AccountStatus;
import com.se201411.project.mooncake.security.auth.MoonCakeUserPrincipal;
import com.se201411.project.mooncake.security.config.SecurityConfig;
import com.se201411.project.mooncake.security.dto.AccountPageView;
import com.se201411.project.mooncake.security.dto.ProfileUpdateForm;
import com.se201411.project.mooncake.security.dto.ShopperIdentity;
import com.se201411.project.mooncake.security.service.AuthenticationService;
import com.se201411.project.mooncake.security.service.CurrentCustomerService;
import com.se201411.project.mooncake.security.service.CustomerProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = {
    AccountTemplateTest.Config.class,
    SecurityConfig.class
})
class AccountTemplateTest {

    private static final ShopperIdentity CUSTOMER = ShopperIdentity.forCustomer(7L);

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private CurrentCustomerService currentCustomerService;

    @Autowired
    private CustomerProfileService customerProfileService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reset(currentCustomerService, customerProfileService);
        mockMvc = webAppContextSetup(context)
            .apply(springSecurity())
            .build();
        ProfileUpdateForm form = new ProfileUpdateForm();
        form.setFullName("Nguyễn Văn A");
        form.setPhone("0901234567");
        when(currentCustomerService.requireCurrentCustomer()).thenReturn(CUSTOMER);
        when(customerProfileService.accountPage(CUSTOMER)).thenReturn(new AccountPageView(
            "customer@example.com",
            "Nguyễn Văn A",
            "0901234567",
            "01/10/2026"
        ));
        when(customerProfileService.profileForm(CUSTOMER)).thenReturn(form);
    }

    @Test
    void rendersEmailProfileFormPasswordFormAndHeaderLink() throws Exception {
        mockMvc.perform(get("/account").with(authentication(customerLogin())))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Tài khoản của tôi")))
            .andExpect(content().string(containsString("Thành viên từ 01/10/2026")))
            .andExpect(content().string(containsString("customer@example.com")))
            .andExpect(content().string(containsString("value=\"Nguyễn Văn A\"")))
            .andExpect(content().string(containsString("value=\"0901234567\"")))
            .andExpect(content().string(containsString("action=\"/account/profile\"")))
            .andExpect(content().string(containsString("action=\"/account/password\"")))
            .andExpect(content().string(containsString("href=\"/account\"")))
            .andExpect(content().string(containsString("name=\"_csrf\"")))
            .andExpect(content().string(not(containsString("name=\"email\""))));
    }

    @Test
    void passwordErrorsNeverRenderPasswordsBack() throws Exception {
        mockMvc.perform(post("/account/password")
                .with(authentication(customerLogin()))
                .with(csrf())
                .param("currentPassword", "oldSecret11")
                .param("newPassword", "newSecret22")
                .param("confirmPassword", "otherSecret33"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Mật khẩu xác nhận không khớp")))
            .andExpect(content().string(not(containsString("oldSecret11"))))
            .andExpect(content().string(not(containsString("newSecret22"))))
            .andExpect(content().string(not(containsString("otherSecret33"))));
    }

    private static UsernamePasswordAuthenticationToken customerLogin() {
        MoonCakeUserPrincipal principal = new MoonCakeUserPrincipal(
            3L,
            7L,
            "customer@example.com",
            "hash",
            AccountRole.CUSTOMER,
            AccountStatus.ACTIVE
        );
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    @Configuration
    @EnableWebMvc
    @Import({
        AccountController.class,
        AuthenticationViewAdvice.class,
        CartHeaderViewAdvice.class
    })
    static class Config {

        @Bean
        CurrentCustomerService currentCustomerService() {
            return mock(CurrentCustomerService.class);
        }

        @Bean
        CustomerProfileService customerProfileService() {
            return mock(CustomerProfileService.class);
        }

        @Bean
        AuthenticationService authenticationService() {
            return mock(AuthenticationService.class);
        }

        @Bean
        CustomerCartService customerCartService() {
            return mock(CustomerCartService.class);
        }

        @Bean
        UserDetailsService userDetailsService() {
            return new InMemoryUserDetailsManager(
                User.withUsername("customer@mooncake.test")
                    .password("{noop}password123")
                    .roles("CUSTOMER")
                    .build()
            );
        }

        @Bean
        LocalValidatorFactoryBean validator() {
            return new LocalValidatorFactoryBean();
        }

        @Bean
        SpringTemplateEngine templateEngine() {
            ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
            resolver.setPrefix("templates/");
            resolver.setSuffix(".html");
            resolver.setCharacterEncoding("UTF-8");

            SpringTemplateEngine engine = new SpringTemplateEngine();
            engine.setTemplateResolver(resolver);
            return engine;
        }

        @Bean
        ThymeleafViewResolver viewResolver(SpringTemplateEngine templateEngine) {
            ThymeleafViewResolver resolver = new ThymeleafViewResolver();
            resolver.setTemplateEngine(templateEngine);
            resolver.setCharacterEncoding("UTF-8");
            return resolver;
        }
    }
}
