package com.cooksync_server.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service responsible for sending transactional emails to users, backed by
 * Gmail SMTP via {@link JavaMailSender} (see {@code spring.mail.*} in
 * application.properties).
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 05/08/2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImp implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    /**
     * Sends a password-reset email containing the given code to the given
     * address via Gmail SMTP.
     *
     * @param toEmail the recipient's email address
     * @param resetCode the one-time 6-digit password-reset code to include in
     * the email
     * @param validityMinutes number of minutes the code remains valid, included
     * in the email body
     */
    @Override
    public void sendPasswordResetEmail(String toEmail, String resetCode, int validityMinutes) {
        boolean sent = sendEmail(toEmail, "Reset your CookSync password",
                """
                We received a request to reset your CookSync password.
                
                Your password reset code is: """ + resetCode
                + "\n\nEnter this code in the app to choose a new password. It expires in "
                + validityMinutes + " minutes."
                + "\n\nIf you didn't request this, you can safely ignore this email.");
        if (sent) {
            log.info("Password reset email sent to {}", toEmail);
        }
    }

    /**
     * Sends a registration verification email containing the given one-time OTP
     * code via Gmail SMTP.
     *
     * @param toEmail the recipient's email address
     * @param code the 6-digit OTP code to include in the email
     * @param validityMinutes number of minutes the code remains valid, included
     * in the email body
     */
    @Override
    public void sendOtpEmail(String toEmail, String code, int validityMinutes) {
        boolean sent = sendEmail(toEmail, "Your CookSync verification code",
                "Your CookSync verification code is: " + code
                + "\n\nThis code expires in " + validityMinutes + " minutes."
                + "\n\nIf you didn't request this, you can safely ignore this email.");
        if (sent) {
            log.info("Registration OTP email sent to {}", toEmail);
        }
    }

    /**
     * Builds and sends a plain-text transactional email via Gmail SMTP. Shared
     * by every account-email method in this class so the
     * {@link SimpleMailMessage} construction lives in exactly one place.
     *
     * <p>
     * If {@code MAIL_USERNAME}/{@code MAIL_PASSWORD} are not configured, or
     * Gmail rejects them as invalid, the send is skipped: a warning is logged
     * and the email content (including the OTP/reset code) is logged at debug
     * level instead, so local/dev environments without mail credentials can
     * still read the code that would have been emailed.
     *
     * @param toEmail the recipient's email address
     * @param subject the email subject line
     * @param body the plain-text email body
     * @return {@code true} if the email was actually sent, {@code false} if the
     * send was skipped or failed (in which case a warning was logged instead)
     */
    private boolean sendEmail(String toEmail, String subject, String body) {
        if (!StringUtils.hasText(fromAddress) || !StringUtils.hasText(mailPassword)) {
            log.warn("MAIL_USERNAME/MAIL_PASSWORD are not set; skipping email send to {}", toEmail);
            log.debug("Email that would have been sent to {} - subject: {}, body: {}", toEmail, subject, body.replaceAll("\n", " "));
            return false;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);
        try {
            mailSender.send(message);
            return true;
        } catch (MailException e) {
            log.warn("Failed to send email to {} - mail credentials may be invalid: {}", toEmail, e.getMessage());
            log.debug("Email that would have been sent to {} - subject: {}, body: {}", toEmail, subject, body);
            return false;
        }
    }
}
