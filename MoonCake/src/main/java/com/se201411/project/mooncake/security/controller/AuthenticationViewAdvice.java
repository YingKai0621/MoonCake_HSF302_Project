package com.se201411.project.mooncake.security.controller;

import com.se201411.project.mooncake.security.auth.MoonCakeUserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class AuthenticationViewAdvice {

    @ModelAttribute
    public void addAuthenticationAttributes(Model model, Authentication authentication) {
        boolean loggedIn = false;
        String currentUserEmail = null;

        if (authentication != null
            && authentication.isAuthenticated()
            && authentication.getPrincipal() instanceof MoonCakeUserPrincipal userPrincipal) {
            loggedIn = true;
            currentUserEmail = userPrincipal.getUsername();
        }

        model.addAttribute("loggedIn", loggedIn);
        model.addAttribute("currentUserEmail", currentUserEmail);
    }
}
