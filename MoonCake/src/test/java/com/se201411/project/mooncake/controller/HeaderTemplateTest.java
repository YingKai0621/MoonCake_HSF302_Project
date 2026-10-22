package com.se201411.project.mooncake.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = HeaderTemplateTest.Config.class)
class HeaderTemplateTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = webAppContextSetup(context).build();
    }

    @Test
    void anonymousHeaderHasCompactNavigationWithoutSearch() throws Exception {
        mockMvc.perform(get("/header-test"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Điều hướng chính")))
            .andExpect(content().string(containsString("Đăng nhập")))
            .andExpect(content().string(containsString("Đăng ký")))
            .andExpect(content().string(containsString("Giỏ hàng")))
            .andExpect(content().string(not(containsString("Tìm kiếm vị bánh"))))
            .andExpect(content().string(not(containsString("Đơn hàng của tôi"))));
    }

    @Test
    void customerHeaderKeepsAccountOrdersCartAndPostLogout() throws Exception {
        mockMvc.perform(get("/header-test").param("state", "customer"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("customer@mooncake.test")))
            .andExpect(content().string(containsString("Đơn hàng của tôi")))
            .andExpect(content().string(containsString("Giỏ hàng")))
            .andExpect(content().string(containsString("action=\"/logout\"")))
            .andExpect(content().string(containsString("method=\"post\"")))
            .andExpect(content().string(not(containsString("Trang quản trị"))));
    }

    @Test
    void adminHeaderShowsAdminEntryWithoutCustomerCart() throws Exception {
        mockMvc.perform(get("/header-test").param("state", "admin"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("admin@mooncake.test")))
            .andExpect(content().string(containsString("Trang quản trị")))
            .andExpect(content().string(not(containsString("Đơn hàng của tôi"))))
            .andExpect(content().string(not(containsString("Giỏ hàng"))));
    }

    @Controller
    static class HeaderTestController {

        @GetMapping("/header-test")
        String header(
            @RequestParam(defaultValue = "anonymous") String state,
            Model model
        ) {
            boolean customer = "customer".equals(state);
            boolean admin = "admin".equals(state);

            model.addAttribute("activePage", "home");
            model.addAttribute("loggedIn", customer || admin);
            model.addAttribute("customerLoggedIn", customer);
            model.addAttribute("headerCartQuantity", customer ? 2 : 0);

            if (customer) {
                model.addAttribute("currentUserEmail", "customer@mooncake.test");
            } else if (admin) {
                model.addAttribute("currentUserEmail", "admin@mooncake.test");
            }

            return "header-test-page";
        }
    }

    @Configuration
    @EnableWebMvc
    @Import(HeaderTestController.class)
    static class Config {

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
