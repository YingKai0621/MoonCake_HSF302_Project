package com.se201411.project.mooncake.security.controller;

import com.se201411.project.mooncake.security.dto.AccountPageView;
import com.se201411.project.mooncake.security.dto.ProfileUpdateForm;
import com.se201411.project.mooncake.security.dto.ShopperIdentity;
import com.se201411.project.mooncake.security.exception.WrongCurrentPasswordException;
import com.se201411.project.mooncake.security.service.AuthenticationService;
import com.se201411.project.mooncake.security.service.CurrentCustomerService;
import com.se201411.project.mooncake.security.service.CustomerProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import static org.mockito.ArgumentMatchers.any;
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

    @Mock
    private CurrentCustomerService currentCustomerService;

    @Mock
    private CustomerProfileService customerProfileService;

    @Mock
    private AuthenticationService authenticationService;

    private MockMvc mockMvc;
    private AccountPageView page;

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

        page = new AccountPageView(
            "customer@example.com",
            "Nguyễn Văn A",
            "0901234567",
            "21/10/2026"
        );
        ProfileUpdateForm profileForm = new ProfileUpdateForm();
        profileForm.setFullName("Nguyễn Văn A");
        profileForm.setPhone("0901234567");
        when(currentCustomerService.requireCurrentCustomer()).thenReturn(CUSTOMER);
        lenient().when(customerProfileService.accountPage(CUSTOMER)).thenReturn(page);
        lenient().when(customerProfileService.profileForm(CUSTOMER)).thenReturn(profileForm);
    }

    @Test
    void rendersAccountPageWithOnePageView() throws Exception {
        mockMvc.perform(get("/account"))
            .andExpect(status().isOk())
            .andExpect(view().name("security/account"))
            .andExpect(model().attribute("page", page))
            .andExpect(model().attributeExists("profileForm", "passwordForm"));
    }

    @Test
    void invalidPhoneShowsErrorWithoutCallingService() throws Exception {
        mockMvc.perform(post("/account/profile")
                .param("fullName", "Nguyễn Văn A")
                .param("phone", "123"))
            .andExpect(status().isOk())
            .andExpect(view().name("security/account"))
            .andExpect(model().attributeHasFieldErrors("profileForm", "phone"));

        verify(customerProfileService, never()).updateProfile(any(), any());
    }

    @Test
    void mismatchedConfirmationShowsErrorWithoutCallingService() throws Exception {
        mockMvc.perform(post("/account/password")
                .param("currentPassword", "password123")
                .param("newPassword", "newPassword123")
                .param("confirmPassword", "differentPassword"))
            .andExpect(status().isOk())
            .andExpect(view().name("security/account"))
            .andExpect(model().attributeHasFieldErrors(
                "passwordForm",
                "confirmPassword"
            ));

        verify(authenticationService, never()).changePassword(any(), any(), any());
    }

    @Test
    void wrongCurrentPasswordShowsFieldError() throws Exception {
        doThrow(new WrongCurrentPasswordException())
            .when(authenticationService)
            .changePassword(CUSTOMER, "wrongPassword", "newPassword123");

        mockMvc.perform(post("/account/password")
                .param("currentPassword", "wrongPassword")
                .param("newPassword", "newPassword123")
                .param("confirmPassword", "newPassword123"))
            .andExpect(status().isOk())
            .andExpect(view().name("security/account"))
            .andExpect(model().attributeHasFieldErrors(
                "passwordForm",
                "currentPassword"
            ));
    }

    @Test
    void successfulProfileUpdateRedirectsWithMessage() throws Exception {
        mockMvc.perform(post("/account/profile")
                .param("fullName", "Trần Thị B")
                .param("phone", "0912345678"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/account"))
            .andExpect(flash().attribute(
                "successMessage",
                "Đã cập nhật thông tin cá nhân."
            ));

        verify(customerProfileService).updateProfile(
            org.mockito.ArgumentMatchers.eq(CUSTOMER),
            any(ProfileUpdateForm.class)
        );
    }
}
