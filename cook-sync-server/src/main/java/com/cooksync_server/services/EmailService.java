package com.cooksync_server.services;

/**
 * Service interface for sending transactional account emails.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
public interface EmailService {

    /**
     * Sends a password-reset email containing the given reset code to the target address.
     *
     * @param toEmail recipient email address
     * @param resetCode single-use 6-digit password reset code to embed in the email
     * @param validityMinutes number of minutes the code remains valid, included in the email body
     */
    void sendPasswordResetEmail(String toEmail, String resetCode, int validityMinutes);

    /**
     * Sends a registration verification email containing the given one-time OTP code.
     *
     * @param toEmail recipient email address
     * @param code the 6-digit OTP code to embed in the email
     * @param validityMinutes number of minutes the code remains valid, included in the email body
     */
    void sendOtpEmail(String toEmail, String code, int validityMinutes);
}
