package com.se201411.project.mooncake.security.service;

/**
 * Delivers a password-reset link to the account owner.
 */
public interface PasswordResetNotifier {

    void sendResetLink(String recipientEmail, String recipientName, String resetLink, long validMinutes);
}
