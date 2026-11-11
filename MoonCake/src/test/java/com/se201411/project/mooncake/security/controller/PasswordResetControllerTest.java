package com.se201411.project.mooncake.security.controller;

import com.se201411.project.mooncake.security.exception.InvalidPasswordResetTokenException;
import com.se201411.project.mooncake.security.service.PasswordResetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(MockitoExtension.class)
class PasswordResetControllerTest {

    private static final String TOKEN = "A".repeat(43);

    @Mock
    private PasswordResetService passwordResetService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders
            .standaloneSetup(new PasswordResetController(passwordResetService))
            .setValidator(validator)
            .build();
    }

    @Test
    void rendersForgotPasswordPage() throws Exception {
        mockMvc.perform(get("/forgot-password"))
            .andExpect(status().isOk())
            .andExpect(view().name("security/forgot-password"))
            .andExpect(model().attributeExists("forgotPasswordForm"));
    }

    @Test
    void forgotPasswordAlwaysRedirectsWithNeutralMessage() throws Exception {
        mockMvc.perform(post("/forgot-password").param("email", " customer@example.com "))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/forgot-password?sent"));

        verify(passwordResetService).requestReset("customer@example.com");
    }

    @Test
    void forgotPasswordRejectsInvalidEmail() throws Exception {
        mockMvc.perform(post("/forgot-password").param("email", "not-an-email"))
            .andExpect(status().isOk())
            .andExpect(view().name("security/forgot-password"))
            .andExpect(model().attributeHasFieldErrors("forgotPasswordForm", "email"));

        verify(passwordResetService, never()).requestReset(anyString());
    }

    @Test
    void resetPageShowsFormForUsableToken() throws Exception {
        when(passwordResetService.isTokenUsable(TOKEN)).thenReturn(true);

        mockMvc.perform(get("/reset-password").param("token", TOKEN))
            .andExpect(status().isOk())
            .andExpect(view().name("security/reset-password"))
            .andExpect(model().attributeExists("resetPasswordForm"))
            .andExpect(model().attributeDoesNotExist("invalidToken"))
            .andExpect(header().string("Referrer-Policy", "no-referrer"))
            .andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test
    void resetPageReportsUnusableToken() throws Exception {
        when(passwordResetService.isTokenUsable(TOKEN)).thenReturn(false);

        mockMvc.perform(get("/reset-password").param("token", TOKEN))
            .andExpect(status().isOk())
            .andExpect(view().name("security/reset-password"))
            .andExpect(model().attribute("invalidToken", true));
    }

    @Test
    void resetSucceedsAndRedirectsToLogin() throws Exception {
        mockMvc.perform(resetRequest("newPassword1", "newPassword1"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login?reset"));

        verify(passwordResetService).resetPassword(TOKEN, "newPassword1");
    }

    @Test
    void resetRejectsMismatchedConfirmation() throws Exception {
        mockMvc.perform(resetRequest("newPassword1", "different1"))
            .andExpect(status().isOk())
            .andExpect(view().name("security/reset-password"))
            .andExpect(model().attributeHasFieldErrors("resetPasswordForm", "confirmPassword"));

        verify(passwordResetService, never()).resetPassword(anyString(), anyString());
    }

    @Test
    void resetRejectsShortPassword() throws Exception {
        mockMvc.perform(resetRequest("short", "short"))
            .andExpect(status().isOk())
            .andExpect(model().attributeHasFieldErrors("resetPasswordForm", "password"));

        verify(passwordResetService, never()).resetPassword(anyString(), anyString());
    }

    @Test
    void resetRejectsPasswordOverBcryptByteLimit() throws Exception {
        String longPassword = "ă".repeat(40);
        mockMvc.perform(resetRequest(longPassword, longPassword))
            .andExpect(status().isOk())
            .andExpect(model().attributeHasFieldErrorCode(
                "resetPasswordForm",
                "password",
                "password.tooLongForBcrypt"
            ));

        verify(passwordResetService, never()).resetPassword(anyString(), anyString());
    }

    @Test
    void resetShowsInvalidTokenWhenServiceRejects() throws Exception {
        doThrow(new InvalidPasswordResetTokenException())
            .when(passwordResetService).resetPassword(TOKEN, "newPassword1");

        mockMvc.perform(resetRequest("newPassword1", "newPassword1"))
            .andExpect(status().isOk())
            .andExpect(view().name("security/reset-password"))
            .andExpect(model().attribute("invalidToken", true));
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder resetRequest(
        String password,
        String confirmPassword
    ) {
        return post("/reset-password")
            .param("token", TOKEN)
            .param("password", password)
            .param("confirmPassword", confirmPassword);
    }
}
