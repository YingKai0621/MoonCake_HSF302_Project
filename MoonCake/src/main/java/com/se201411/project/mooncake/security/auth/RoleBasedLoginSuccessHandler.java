package com.se201411.project.mooncake.security.auth;

import java.net.URI;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;

import java.io.IOException;

public class RoleBasedLoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private static final String ADMIN_ROLE = "ROLE_ADMIN";
    private static final String ADMIN_HOME = "/admin/overview";

    private final RequestCache requestCache = new HttpSessionRequestCache();

    public RoleBasedLoginSuccessHandler() {
        setDefaultTargetUrl("/");
        setRequestCache(requestCache);
    }

    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest request,
        HttpServletResponse response,
        Authentication authentication
    ) throws ServletException, IOException {
        if (isAdmin(authentication) && !wasOpeningAdminPage(request, response)) {
            requestCache.removeRequest(request, response);
            clearAuthenticationAttributes(request);
            getRedirectStrategy().sendRedirect(request, response, ADMIN_HOME);
            return;
        }
        super.onAuthenticationSuccess(request, response, authentication);
    }

    private boolean isAdmin(Authentication authentication) {
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (ADMIN_ROLE.equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    private boolean wasOpeningAdminPage(HttpServletRequest request, HttpServletResponse response) {
        SavedRequest savedRequest = requestCache.getRequest(request, response);
        if (savedRequest == null) {
            return false;
        }
        String path = URI.create(savedRequest.getRedirectUrl()).getPath();
        return path.startsWith(request.getContextPath() + "/admin");
    }
}
