package com.se201411.project.mooncake.security.controller;

import com.se201411.project.mooncake.security.dto.CustomerRegistrationForm;
import com.se201411.project.mooncake.security.exception.EmailAlreadyRegisteredException;
import com.se201411.project.mooncake.security.service.AuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(MockitoExtension.class)
class AuthenticationControllerTest {

    @Mock
    private AuthenticationService authenticationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
            .standaloneSetup(new AuthenticationController(authenticationService))
            .setValidator(validator)
            .build();
    }

    @Test
    void rendersLoginAndRegistrationPages() throws Exception {
        mockMvc.perform(get("/login"))
            .andExpect(status().isOk())
            .andExpect(view().name("security/login"));

        mockMvc.perform(get("/register"))
            .andExpect(status().isOk())
            .andExpect(view().name("security/register"))
            .andExpect(model().attributeExists("registrationForm"));
    }

    @Test
    void registersValidCustomerAndRedirectsToLogin() throws Exception {
        mockMvc.perform(validRegistrationRequest())
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login?registered"));

        verify(authenticationService).registerCustomer(any(CustomerRegistrationForm.class));
    }

    @Test
    void rejectsMismatchedPasswordBeforeCallingService() throws Exception {
        mockMvc.perform(validRegistrationRequest().param("confirmPassword", "different123"))
            .andExpect(status().isOk())
            .andExpect(view().name("security/register"))
            .andExpect(model().attributeHasFieldErrors(
                "registrationForm",
                "confirmPassword"
            ));

        verify(authenticationService, never())
            .registerCustomer(any(CustomerRegistrationForm.class));
    }

    @Test
    void showsFriendlyErrorForDuplicateEmail() throws Exception {
        doThrow(new EmailAlreadyRegisteredException())
            .when(authenticationService)
            .registerCustomer(any(CustomerRegistrationForm.class));

        mockMvc.perform(validRegistrationRequest())
            .andExpect(status().isOk())
            .andExpect(view().name("security/register"))
            .andExpect(model().attributeHasFieldErrors("registrationForm", "email"));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
        validRegistrationRequest() {
        return post("/register")
            .param("fullName", "Nguyễn Văn A")
            .param("phone", "0901234567")
            .param("email", "customer@example.com")
            .param("password", "password123")
            .param("confirmPassword", "password123");
    }
}
