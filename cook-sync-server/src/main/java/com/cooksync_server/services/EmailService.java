package com.cooksync_server.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service responsible for sending transactional emails to users, backed by Gmail SMTP via
 * {@link JavaMailSender} (see {@code spring.mail.*} in application.properties).
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 05/08/2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService implements IEmailService{

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    /**
     * Sends a password-reset email containing the given code to the given address via Gmail
     * SMTP.
     *
     * Complexity:
     * Time: O(1)
     * Space: O(1)
     *
     * @param toEmail the recipient's email address
     * @param resetCode the one-time 6-digit password-reset code to include in the email
     * @param validityMinutes number of minutes the code remains valid, included in the email body
     */
    public void sendPasswordResetEmail(String toEmail, String resetCode, int validityMinutes) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("Reset your CookSync password");
        message.setText("We received a request to reset your CookSync password."
                + "\n\nYour password reset code is: " + resetCode
                + "\n\nEnter this code in the app to choose a new password. It expires in "
                + validityMinutes + " minutes."
                + "\n\nIf you didn't request this, you can safely ignore this email.");
        mailSender.send(message);
        log.info("Password reset email sent to {}", toEmail);
    }

    /**
     * Sends a registration verification email containing the given one-time OTP code via Gmail
     * SMTP.
     *
     * Complexity:
     * Time: O(1)
     * Space: O(1)
     *
     * @param toEmail the recipient's email address
     * @param code the 6-digit OTP code to include in the email
     * @param validityMinutes number of minutes the code remains valid, included in the email body
     */
    public void sendOtpEmail(String toEmail, String code, int validityMinutes) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("Your CookSync verification code");
        message.setText("Your CookSync verification code is: " + code
                + "\n\nThis code expires in " + validityMinutes + " minutes."
                + "\n\nIf you didn't request this, you can safely ignore this email.");
        mailSender.send(message);
        log.info("Registration OTP email sent to {}", toEmail);
    }
}
