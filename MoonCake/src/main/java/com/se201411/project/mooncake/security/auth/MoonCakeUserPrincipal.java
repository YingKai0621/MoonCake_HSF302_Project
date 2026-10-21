package com.se201411.project.mooncake.security.auth;

import com.se201411.project.mooncake.enums.AccountRole;
import com.se201411.project.mooncake.enums.AccountStatus;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class MoonCakeUserPrincipal implements UserDetails, CredentialsContainer {

    private final long accountId;
    private final Long customerId;
    private final String email;
    private String passwordHash;
    private final AccountRole role;
    private final AccountStatus status;

    public MoonCakeUserPrincipal(
        long accountId,
        Long customerId,
        String email,
        String passwordHash,
        AccountRole role,
        AccountStatus status
    ) {
        this.accountId = accountId;
        this.customerId = customerId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.status = status;
    }

    public long getAccountId() {
        return accountId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public AccountRole getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String authority = "ROLE_" + role.name();
        return List.of(new SimpleGrantedAuthority(authority));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == AccountStatus.ACTIVE;
    }

    @Override
    public void eraseCredentials() {
        passwordHash = null;
    }
}
