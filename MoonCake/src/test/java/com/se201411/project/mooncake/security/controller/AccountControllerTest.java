package com.se201411.project.mooncake.security.controller;

import com.se201411.project.mooncake.security.dto.AccountPageView;
import com.se201411.project.mooncake.security.dto.ProfileUpdateForm;
import com.se201411.project.mooncake.security.dto.ShopperIdentity;
import com.se201411.project.mooncake.security.exception.SamePasswordException;
import com.se201411.project.mooncake.security.exception.WrongCurrentPasswordException;
import com.se201411.project.mooncake.security.service.AuthenticationService;
import com.se201411.project.mooncake.security.service.CurrentCustomerService;
import com.se201411.project.mooncake.security.service.CustomerProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
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
class AccountControllerTest {

    private static final ShopperIdentity CUSTOMER = ShopperIdentity.forCustomer(7L);
    private static final AccountPageView PAGE = new AccountPageView(
        "customer@example.com",
        "Nguyễn Văn A",
        "0901234567",
        "21/10/2026"
    );

    @Mock
    private CurrentCustomerService currentCustomerService;

    @Mock
    private CustomerProfileService customerProfileService;

    @Mock
    private AuthenticationService authenticationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AccountController controller = new AccountController(
            currentCustomerService,
            customerProfileService,
            authenticationService
        );
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/views/");
        viewResolver.setSuffix(".html");
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setValidator(validator)
            .setViewResolvers(viewResolver)
            .build();

        when(currentCustomerService.requireCurrentCustomer()).thenReturn(CUSTOMER);
        lenient().when(customerProfileService.accountPage(CUSTOMER)).thenReturn(PAGE);
        lenient().when(customerProfileService.profileForm(CUSTOMER)).thenReturn(new ProfileUpdateForm());
    }

    @Test
    void rendersAccountPageWithOnePageView() throws Exception {
        mockMvc.perform(get("/account"))
            .andExpect(status().isOk())
            .andExpect(view().name("account"))
            .andExpect(model().attribute("page", PAGE))
            .andExpect(model().attributeExists("profileForm"))
            .andExpect(model().attributeExists("passwordForm"));
    }

    @Test
    void invalidPhoneShowsErrorWithoutCallingService() throws Exception {
        mockMvc.perform(post("/account/profile")
                .param("fullName", "Trần Thị B")
                .param("phone", "091234"))
            .andExpect(status().isOk())
            .andExpect(view().name("account"))
            .andExpect(model().attributeHasFieldErrors("profileForm", "phone"))
            .andExpect(model().attribute("page", PAGE))
            .andExpect(model().attributeExists("passwordForm"));

        verify(customerProfileService, never()).updateProfile(any(), any());
    }

    @Test
    void successfulProfileUpdateRedirectsWithMessage() throws Exception {
        mockMvc.perform(post("/account/profile")
                .param("fullName", "  Trần Thị B  ")
                .param("phone", "0912345678"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/account"))
            .andExpect(flash().attribute("successMessage", "Đã cập nhật thông tin cá nhân."));

        verify(customerProfileService).updateProfile(eq(CUSTOMER), any(ProfileUpdateForm.class));
    }

    @Test
    void mismatchedConfirmationShowsErrorWithoutCallingService() throws Exception {
        mockMvc.perform(passwordRequest("password123", "newPassword1", "different99"))
            .andExpect(status().isOk())
            .andExpect(view().name("account"))
            .andExpect(model().attributeHasFieldErrors("passwordForm", "confirmPassword"))
            .andExpect(model().attributeExists("profileForm"));

        verify(authenticationService, never()).changePassword(any(), anyString(), anyString());
    }

    @Test
    void wrongCurrentPasswordShowsFieldError() throws Exception {
        doThrow(new WrongCurrentPasswordException())
            .when(authenticationService)
            .changePassword(CUSTOMER, "wrong-password", "newPassword1");

        mockMvc.perform(passwordRequest("wrong-password", "newPassword1", "newPassword1"))
            .andExpect(status().isOk())
            .andExpect(view().name("account"))
            .andExpect(model().attributeHasFieldErrorCode("passwordForm", "currentPassword", "password.wrong"));
    }

    @Test
    void sameNewPasswordShowsFieldError() throws Exception {
        doThrow(new SamePasswordException())
            .when(authenticationService)
            .changePassword(CUSTOMER, "password123", "password123");

        mockMvc.perform(passwordRequest("password123", "password123", "password123"))
            .andExpect(status().isOk())
            .andExpect(model().attributeHasFieldErrorCode("passwordForm", "newPassword", "password.same"));
    }

    @Test
    void successfulPasswordChangeRedirectsWithMessage() throws Exception {
        mockMvc.perform(passwordRequest("password123", "newPassword1", "newPassword1"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/account"))
            .andExpect(flash().attribute("successMessage", "Đã đổi mật khẩu."));

        verify(authenticationService).changePassword(CUSTOMER, "password123", "newPassword1");
    }

    private static MockHttpServletRequestBuilder passwordRequest(
        String currentPassword,
        String newPassword,
        String confirmPassword
    ) {
        return post("/account/password")
            .param("currentPassword", currentPassword)
            .param("newPassword", newPassword)
            .param("confirmPassword", confirmPassword);
    }
}
