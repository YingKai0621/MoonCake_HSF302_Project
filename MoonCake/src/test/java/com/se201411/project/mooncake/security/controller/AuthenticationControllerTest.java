package com.se201411.project.mooncake.security.controller;

import com.se201411.project.mooncake.security.dto.CustomerRegistrationForm;
import com.se201411.project.mooncake.security.exception.EmailAlreadyRegisteredException;
import com.se201411.project.mooncake.security.service.AuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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
        mockMvc.perform(registrationRequest(
            "Nguyễn Văn A",
            "0901234567",
            "customer@example.com",
            "password123",
            "different123"
        ))
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
    void rejectsInvalidPhoneFormatsBeforeCallingService() throws Exception {
        List<String> invalidPhones = List.of(
            "1",
            "9012345678",
            "090123456",
            "09012345678",
            "090 123 4567",
            "09012abc67"
        );

        for (String invalidPhone : invalidPhones) {
            mockMvc.perform(registrationRequest(
                "Nguyễn Văn A",
                invalidPhone,
                "customer@example.com",
                "password123",
                "password123"
            ))
                .andExpect(status().isOk())
                .andExpect(view().name("security/register"))
                .andExpect(model().attributeHasFieldErrors("registrationForm", "phone"));
        }

        verify(authenticationService, never())
            .registerCustomer(any(CustomerRegistrationForm.class));
    }

    @Test
    void rejectsBlankNameInvalidEmailAndShortPassword() throws Exception {
        mockMvc.perform(registrationRequest(
            " ",
            "0901234567",
            "not-an-email",
            "short",
            "short"
        ))
            .andExpect(status().isOk())
            .andExpect(view().name("security/register"))
            .andExpect(model().attributeHasFieldErrors(
                "registrationForm",
                "fullName",
                "email",
                "password",
                "confirmPassword"
            ));

        verify(authenticationService, never())
            .registerCustomer(any(CustomerRegistrationForm.class));
    }

    @Test
    void rejectsFullNameWithNumbersSpecialCharactersOrExceeding40Characters() throws Exception {
        List<String> invalidNames = List.of(
            "Nguyễn Văn A123",
            "Trần Thị B @#$",
            "<script>",
            "a".repeat(41)
        );

        for (String invalidName : invalidNames) {
            mockMvc.perform(registrationRequest(
                invalidName,
                "0901234567",
                "customer@example.com",
                "password123",
                "password123"
            ))
                .andExpect(status().isOk())
                .andExpect(view().name("security/register"))
                .andExpect(model().attributeHasFieldErrors("registrationForm", "fullName"));
        }

        verify(authenticationService, never())
            .registerCustomer(any(CustomerRegistrationForm.class));
    }

    @Test
    void rejectsFieldsThatExceedDatabaseLengths() throws Exception {
        String longName = "a".repeat(161);
        String longEmail = "a".repeat(243) + "@example.com";
        String longPassword = "a".repeat(65);

        mockMvc.perform(registrationRequest(
            longName,
            "0901234567",
            longEmail,
            longPassword,
            longPassword
        ))
            .andExpect(status().isOk())
            .andExpect(view().name("security/register"))
            .andExpect(model().attributeHasFieldErrors(
                "registrationForm",
                "fullName",
                "email",
                "password",
                "confirmPassword"
            ));

        verify(authenticationService, never())
            .registerCustomer(any(CustomerRegistrationForm.class));
    }

    @Test
    void rejectsPasswordThatExceedsBcryptByteLimit() throws Exception {
        String passwordOver72Bytes = "😀".repeat(19);

        mockMvc.perform(registrationRequest(
            "Nguyễn Văn A",
            "0901234567",
            "customer@example.com",
            passwordOver72Bytes,
            passwordOver72Bytes
        ))
            .andExpect(status().isOk())
            .andExpect(view().name("security/register"))
            .andExpect(model().attributeHasFieldErrors("registrationForm", "password"));

        verify(authenticationService, never())
            .registerCustomer(any(CustomerRegistrationForm.class));
    }

    @Test
    void trimsEmailBeforeValidation() throws Exception {
        mockMvc.perform(registrationRequest(
            "Nguyễn Văn A",
            "0901234567",
            "  customer@example.com  ",
            "password123",
            "password123"
        ))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login?registered"));

        ArgumentCaptor<CustomerRegistrationForm> formCaptor =
            ArgumentCaptor.forClass(CustomerRegistrationForm.class);
        verify(authenticationService).registerCustomer(formCaptor.capture());
        assertThat(formCaptor.getValue().getEmail())
            .isEqualTo("customer@example.com");
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
        return registrationRequest(
            "Nguyễn Văn A",
            "0901234567",
            "customer@example.com",
            "password123",
            "password123"
        );
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
        registrationRequest(
            String fullName,
            String phone,
            String email,
            String password,
            String confirmPassword
        ) {
        return post("/register")
            .param("fullName", fullName)
            .param("phone", phone)
            .param("email", email)
            .param("password", password)
            .param("confirmPassword", confirmPassword);
    }
}
