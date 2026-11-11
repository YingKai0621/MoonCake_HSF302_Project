package com.se201411.project.mooncake.security.controller;

import com.se201411.project.mooncake.cart.controller.CartHeaderViewAdvice;
import com.se201411.project.mooncake.cart.service.CustomerCartService;
import com.se201411.project.mooncake.security.config.SecurityConfig;
import com.se201411.project.mooncake.security.service.AuthenticationService;
import com.se201411.project.mooncake.security.service.CurrentCustomerService;
import com.se201411.project.mooncake.security.service.PasswordResetService;
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
import static org.mockito.Mockito.when;
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
    PasswordResetTemplateTest.Config.class,
    SecurityConfig.class
})
class PasswordResetTemplateTest {

    private static final String TOKEN = "B".repeat(43);

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private PasswordResetService passwordResetService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reset(passwordResetService);
        mockMvc = webAppContextSetup(context)
            .apply(springSecurity())
            .build();
    }

    @Test
    void loginLinksToForgotPasswordAndShowsResetSuccess() throws Exception {
        mockMvc.perform(get("/login").param("reset", ""))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("href=\"/forgot-password\"")))
            .andExpect(content().string(containsString("Đặt lại mật khẩu thành công")))
            .andExpect(content().string(containsString("data-flash-query-param=\"reset\"")));
    }

    @Test
    void forgotPasswordRendersFormAndNeutralSentMessage() throws Exception {
        mockMvc.perform(get("/forgot-password"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("action=\"/forgot-password\"")))
            .andExpect(content().string(containsString("name=\"_csrf\"")))
            .andExpect(content().string(not(containsString("đã được gửi"))));

        mockMvc.perform(get("/forgot-password").param("sent", ""))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Nếu email này đã đăng ký tài khoản")));
    }

    @Test
    void forgotPasswordShowsFieldErrorForInvalidEmail() throws Exception {
        mockMvc.perform(post("/forgot-password").with(csrf()).param("email", "sai-dinh-dang"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("id=\"email-error\"")))
            .andExpect(content().string(containsString("value=\"sai-dinh-dang\"")));
    }

    @Test
    void resetPasswordRendersFormWithHiddenTokenForUsableLink() throws Exception {
        when(passwordResetService.isTokenUsable(TOKEN)).thenReturn(true);

        mockMvc.perform(get("/reset-password").param("token", TOKEN))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("type=\"hidden\"")))
            .andExpect(content().string(containsString("value=\"" + TOKEN + "\"")))
            .andExpect(content().string(containsString("data-target=\"password\"")))
            .andExpect(content().string(containsString("data-target=\"confirmPassword\"")))
            .andExpect(content().string(containsString("name=\"_csrf\"")))
            .andExpect(content().string(containsString("name=\"referrer\" content=\"no-referrer\"")));
    }

    @Test
    void resetPasswordShowsInvalidLinkMessage() throws Exception {
        when(passwordResetService.isTokenUsable(TOKEN)).thenReturn(false);

        mockMvc.perform(get("/reset-password").param("token", TOKEN))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("role=\"alert\"")))
            .andExpect(content().string(containsString("href=\"/forgot-password\"")))
            .andExpect(content().string(not(containsString("action=\"/reset-password\""))));
    }

    @Test
    void resetValidationNeverRendersPasswordsBack() throws Exception {
        mockMvc.perform(post("/reset-password")
                .with(csrf())
                .param("token", TOKEN)
                .param("password", "newPassword1")
                .param("confirmPassword", "different99"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("id=\"confirmPassword-error\"")))
            .andExpect(content().string(containsString("value=\"" + TOKEN + "\"")))
            .andExpect(content().string(not(containsString("value=\"newPassword1\""))))
            .andExpect(content().string(not(containsString("value=\"different99\""))));
    }

    @Configuration
    @EnableWebMvc
    @Import({
        AuthenticationController.class,
        PasswordResetController.class,
        AuthenticationViewAdvice.class,
        CartHeaderViewAdvice.class
    })
    static class Config {

        @Bean
        AuthenticationService authenticationService() {
            return mock(AuthenticationService.class);
        }

        @Bean
        PasswordResetService passwordResetService() {
            return mock(PasswordResetService.class);
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
