package com.se201411.project.mooncake.security.service;

import com.se201411.project.mooncake.security.exception.InvalidPasswordResetTokenException;

public interface PasswordResetService {

    void requestReset(String email);

    boolean isTokenUsable(String rawToken);

    void resetPassword(String rawToken, String newPassword);
}
