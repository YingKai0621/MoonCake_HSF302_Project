package com.se201411.project.mooncake.security.controller;

import com.se201411.project.mooncake.cart.controller.CartHeaderViewAdvice;
import com.se201411.project.mooncake.cart.service.CustomerCartService;
import com.se201411.project.mooncake.security.config.SecurityConfig;
import com.se201411.project.mooncake.security.service.AuthenticationService;
import com.se201411.project.mooncake.security.service.CurrentCustomerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
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
    AuthenticationTemplateTest.Config.class,
    SecurityConfig.class
})
class AuthenticationTemplateTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private AuthenticationService authenticationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reset(authenticationService);
        mockMvc = webAppContextSetup(context)
            .apply(springSecurity())
            .build();
    }

    @Test
    void loginRendersAccessibleStatusMessagesAndPasswordToggle() throws Exception {
        mockMvc.perform(get("/login").param("registered", ""))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("role=\"status\"")))
            .andExpect(content().string(containsString("Đăng ký thành công")))
            .andExpect(content().string(containsString("data-password-toggle")))
            .andExpect(content().string(containsString("name=\"_csrf\"")))
            .andExpect(content().string(not(containsString("cdn.tailwindcss.com"))));

        mockMvc.perform(get("/login").param("error", ""))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("role=\"alert\"")))
            .andExpect(content().string(containsString("Email hoặc mật khẩu không đúng")));

        mockMvc.perform(get("/login").param("logout", ""))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Bạn đã đăng xuất thành công")));
    }

    @Test
    void registerRendersResponsiveFieldsPasswordTogglesAndCsrf() throws Exception {
        mockMvc.perform(get("/register"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("auth-form--register")))
            .andExpect(content().string(containsString("data-target=\"password\"")))
            .andExpect(content().string(containsString("data-target=\"confirmPassword\"")))
            .andExpect(content().string(containsString("name=\"_csrf\"")))
            .andExpect(content().string(not(containsString("cdn.tailwindcss.com"))));
    }

    @Test
    void validationKeepsNormalFieldsButNeverRendersPasswordsBack() throws Exception {
        mockMvc.perform(post("/register")
                .with(csrf())
                .param("fullName", "Nguyễn Văn A")
                .param("phone", "0901234567")
                .param("email", "customer@example.com")
                .param("password", "password123")
                .param("confirmPassword", "different123"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("value=\"Nguyễn Văn A\"")))
            .andExpect(content().string(containsString("value=\"0901234567\"")))
            .andExpect(content().string(containsString("value=\"customer@example.com\"")))
            .andExpect(content().string(containsString("id=\"confirmPassword-error\"")))
            .andExpect(content().string(containsString("aria-invalid=\"true\"")))
            .andExpect(content().string(not(containsString("value=\"password123\""))))
            .andExpect(content().string(not(containsString("value=\"different123\""))));
    }

    @Configuration
    @EnableWebMvc
    @Import({
        AuthenticationController.class,
        AuthenticationViewAdvice.class,
        CartHeaderViewAdvice.class
    })
    static class Config {

        @Bean
        AuthenticationService authenticationService() {
            return mock(AuthenticationService.class);
        }

        @Bean
        CustomerCartService customerCartService() {
            return mock(CustomerCartService.class);
        }

        @Bean
        CurrentCustomerService currentCustomerService() {
            return mock(CurrentCustomerService.class);
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
