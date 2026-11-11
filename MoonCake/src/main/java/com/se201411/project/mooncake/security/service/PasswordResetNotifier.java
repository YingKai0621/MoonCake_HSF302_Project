package com.se201411.project.mooncake.security.service;

public interface PasswordResetNotifier {

    void sendResetLink(String recipientEmail, String recipientName, String resetLink, long validMinutes);
}
