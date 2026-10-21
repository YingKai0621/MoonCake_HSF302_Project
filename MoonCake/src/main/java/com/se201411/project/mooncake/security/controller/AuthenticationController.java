package com.se201411.project.mooncake.security.controller;

import com.se201411.project.mooncake.security.dto.CustomerRegistrationForm;
import com.se201411.project.mooncake.security.exception.EmailAlreadyRegisteredException;
import com.se201411.project.mooncake.security.service.AuthenticationService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Objects;

@Controller
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    public AuthenticationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @GetMapping("/login")
    public String login() {
        return "security/login";
    }

    @GetMapping("/register")
    public String registrationForm(
        @ModelAttribute("registrationForm") CustomerRegistrationForm registrationForm
    ) {
        return "security/register";
    }

    @PostMapping("/register")
    public String register(
        @Valid @ModelAttribute("registrationForm") CustomerRegistrationForm registrationForm,
        BindingResult bindingResult
    ) {
        if (!Objects.equals(
            registrationForm.getPassword(),
            registrationForm.getConfirmPassword()
        )) {
            bindingResult.rejectValue(
                "confirmPassword",
                "password.mismatch",
                "Mật khẩu xác nhận không khớp"
            );
        }

        if (bindingResult.hasErrors()) {
            return "security/register";
        }

        try {
            authenticationService.registerCustomer(registrationForm);
        } catch (EmailAlreadyRegisteredException exception) {
            bindingResult.rejectValue(
                "email",
                "email.duplicate",
                exception.getMessage()
            );
            return "security/register";
        }

        return "redirect:/login?registered";
    }
}
