package com.se201411.project.mooncake.security.controller;

import com.se201411.project.mooncake.security.dto.ForgotPasswordForm;
import com.se201411.project.mooncake.security.dto.ResetPasswordForm;
import com.se201411.project.mooncake.security.exception.InvalidPasswordResetTokenException;
import com.se201411.project.mooncake.security.service.PasswordResetService;
import com.se201411.project.mooncake.security.util.PasswordRules;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Objects;

@Controller
public class PasswordResetController {

    static final String FORGOT_VIEW = "security/forgot-password";
    static final String RESET_VIEW = "security/reset-password";

    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordForm(@ModelAttribute("forgotPasswordForm") ForgotPasswordForm form) {
        return FORGOT_VIEW;
    }

    @PostMapping("/forgot-password")
    public String requestReset(
        @Valid @ModelAttribute("forgotPasswordForm") ForgotPasswordForm form,
        BindingResult bindingResult
    ) {
        if (bindingResult.hasErrors()) {
            return FORGOT_VIEW;
        }
        passwordResetService.requestReset(form.getEmail());
        return "redirect:/forgot-password?sent";
    }

    @GetMapping("/reset-password")
    public String resetPasswordForm(
        @RequestParam(name = "token", required = false) String token,
        Model model,
        HttpServletResponse response
    ) {
        preventTokenLeak(response);
        if (!passwordResetService.isTokenUsable(token)) {
            model.addAttribute("invalidToken", true);
            return RESET_VIEW;
        }
        ResetPasswordForm form = new ResetPasswordForm();
        form.setToken(token);
        model.addAttribute("resetPasswordForm", form);
        return RESET_VIEW;
    }

    @PostMapping("/reset-password")
    public String resetPassword(
        @Valid @ModelAttribute("resetPasswordForm") ResetPasswordForm form,
        BindingResult bindingResult,
        Model model,
        HttpServletResponse response
    ) {
        preventTokenLeak(response);
        if (!Objects.equals(form.getPassword(), form.getConfirmPassword())) {
            bindingResult.rejectValue(
                "confirmPassword",
                "password.mismatch",
                "Mật khẩu xác nhận không khớp"
            );
        }
        if (PasswordRules.exceedsBcryptLimit(form.getPassword())) {
            bindingResult.rejectValue(
                "password",
                "password.tooLongForBcrypt",
                "Mật khẩu chứa ký tự đặc biệt quá dài, vui lòng rút ngắn"
            );
        }
        if (bindingResult.hasErrors()) {
            return RESET_VIEW;
        }

        try {
            passwordResetService.resetPassword(form.getToken(), form.getPassword());
        } catch (InvalidPasswordResetTokenException exception) {
            model.addAttribute("invalidToken", true);
            return RESET_VIEW;
        }
        return "redirect:/login?reset";
    }

    private static void preventTokenLeak(HttpServletResponse response) {
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("Cache-Control", "no-store");
    }
}
