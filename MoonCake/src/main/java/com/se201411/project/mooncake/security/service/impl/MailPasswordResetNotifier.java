package com.se201411.project.mooncake.security.service.impl;

import com.se201411.project.mooncake.security.service.PasswordResetNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class MailPasswordResetNotifier implements PasswordResetNotifier {

    private static final Logger log = LoggerFactory.getLogger(MailPasswordResetNotifier.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String mailHost;
    private final String fromAddress;

    public MailPasswordResetNotifier(
        ObjectProvider<JavaMailSender> mailSender,
        @Value("${spring.mail.host:}") String mailHost,
        @Value("${mooncake.mail.from:no-reply@mooncake.local}") String fromAddress
    ) {
        this.mailSender = mailSender;
        this.mailHost = mailHost;
        this.fromAddress = fromAddress;
    }

    @Override
    public void sendResetLink(String recipientEmail, String recipientName, String resetLink, long validMinutes) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (mailHost == null || mailHost.isBlank() || sender == null) {
            log.warn("SMTP is not configured; password reset link for {}: {}", recipientEmail, resetLink);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(recipientEmail);
        message.setSubject("[MoonCake] Đặt lại mật khẩu");
        message.setText(buildBody(recipientName, resetLink, validMinutes));

        try {
            sender.send(message);
        } catch (MailException exception) {
            log.error("Could not send password reset mail to {}", recipientEmail, exception);
        }
    }

    private static String buildBody(String recipientName, String resetLink, long validMinutes) {
        String greeting = recipientName == null || recipientName.isBlank()
            ? "Xin chào,"
            : "Xin chào " + recipientName + ",";
        return greeting + "\n\n"
            + "Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản MoonCake của bạn.\n"
            + "Bấm vào đường link sau để đặt mật khẩu mới (hiệu lực " + validMinutes + " phút, chỉ dùng một lần):\n\n"
            + resetLink + "\n\n"
            + "Nếu bạn không yêu cầu, hãy bỏ qua email này; mật khẩu hiện tại vẫn giữ nguyên.\n\n"
            + "MoonCake";
    }
}
