package com.se201411.project.mooncake.security.service;

import com.se201411.project.mooncake.security.exception.InvalidPasswordResetTokenException;

public interface PasswordResetService {

    /**
     * Sends a reset link when the email belongs to an ACTIVE customer. Silently does nothing
     * otherwise, so callers must always show the same neutral message.
     */
    void requestReset(String email);

    /** Returns true when the raw token can still be used to set a new password. */
    boolean isTokenUsable(String rawToken);

    /**
     * Consumes the token once and stores the new password hash in the same transaction.
     *
     * @throws InvalidPasswordResetTokenException when the token is unknown, used or expired
     */
    void resetPassword(String rawToken, String newPassword);
}
