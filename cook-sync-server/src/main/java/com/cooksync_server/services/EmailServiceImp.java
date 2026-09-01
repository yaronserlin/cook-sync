package com.cooksync_server.services;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.extern.slf4j.Slf4j;

/**
 * Service responsible for sending transactional emails to users, backed by the Gmail API
 * (OAuth2, HTTPS) rather than raw SMTP: Gmail's SMTP ports (587/465) are unreachable from
 * Render's network (confirmed by a direct socket probe from inside the deployed service, while
 * outbound HTTPS worked fine), so mail is sent via a plain HTTPS call using a refresh token
 * pre-authorized for the sending account (see {@code GOOGLE_OAUTH_CLIENT_ID}/{@code
 * GOOGLE_OAUTH_CLIENT_SECRET}/{@code GOOGLE_OAUTH_REFRESH_TOKEN} below).
 *
 * @author Yaron Serlin
 * @version 3.0
 * @since 05/08/2026
 */
@Slf4j
@Service
public class EmailServiceImp implements EmailService {

    private static final int TIMEOUT_MS = 5000;

    private final RestClient tokenClient = RestClient.builder()
            .baseUrl("https://oauth2.googleapis.com")
            .requestFactory(timeoutRequestFactory())
            .build();

    private final RestClient gmailClient = RestClient.builder()
            .baseUrl("https://gmail.googleapis.com/gmail/v1")
            .requestFactory(timeoutRequestFactory())
            .build();

    /**
     * OAuth client credentials and a pre-authorized refresh token for the Gmail account emails
     * are sent from. All three default to empty so the app can still start without them; {@link
     * #sendEmail} detects the missing/invalid credentials at send time, logs a warning, and logs
     * the email content (incl. the OTP/reset code) at debug level instead of sending.
     */
    @Value("${GOOGLE_OAUTH_CLIENT_ID:}")
    private String clientId;

    @Value("${GOOGLE_OAUTH_CLIENT_SECRET:}")
    private String clientSecret;

    @Value("${GOOGLE_OAUTH_REFRESH_TOKEN:}")
    private String refreshToken;

    /**
     * Sends a password-reset email containing the given code to the given
     * address via the Gmail API.
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
     * code via the Gmail API.
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
     * Builds and sends a plain-text transactional email via the Gmail API. Shared
     * by every account-email method in this class so the request construction
     * lives in exactly one place.
     *
     * <p>
     * If the OAuth credentials are not fully configured, or Google rejects them
     * (e.g. an expired/revoked refresh token), the send is skipped: a warning is
     * logged and the email content (including the OTP/reset code) is logged at
     * debug level instead, so local/dev environments without credentials can
     * still read the code that would have been emailed.
     *
     * @param toEmail the recipient's email address
     * @param subject the email subject line
     * @param body the plain-text email body
     * @return {@code true} if the email was actually sent, {@code false} if the
     * send was skipped or failed (in which case a warning was logged instead)
     */
    private boolean sendEmail(String toEmail, String subject, String body) {
        if (!StringUtils.hasText(clientId) || !StringUtils.hasText(clientSecret) || !StringUtils.hasText(refreshToken)) {
            log.warn("GOOGLE_OAUTH_CLIENT_ID/GOOGLE_OAUTH_CLIENT_SECRET/GOOGLE_OAUTH_REFRESH_TOKEN are not fully set; skipping email send to {}", toEmail);
            log.debug("Email that would have been sent to {} - subject: {}, body: {}", toEmail, subject, body.replaceAll("\n", " "));
            return false;
        }

        try {
            String accessToken = fetchAccessToken();
            String rawMessage = buildRawMessage(toEmail, subject, body);
            gmailClient.post()
                    .uri("/users/me/messages/send")
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("raw", rawMessage))
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientException e) {
            log.warn("Failed to send email to {} via the Gmail API - the refresh token may be expired/revoked: {}", toEmail, e.getMessage());
            log.debug("Email that would have been sent to {} - subject: {}, body: {}", toEmail, subject, body);
            return false;
        }
    }

    /**
     * Exchanges the pre-authorized refresh token for a short-lived access token. Called on every
     * send rather than cached: OTP/reset emails aren't a hot path, so the extra round trip is
     * negligible, and it avoids having to track access-token expiry across concurrent requests.
     *
     * @return a short-lived Gmail API access token
     */
    private String fetchAccessToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("refresh_token", refreshToken);
        form.add("grant_type", "refresh_token");

        GoogleTokenResponse response = tokenClient.post()
                .uri("/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(GoogleTokenResponse.class);
        if (response == null) {
            throw new RestClientException("Empty response from Google's token endpoint");
        }
        return response.accessToken();
    }

    /**
     * Builds a plain-text RFC 2822 message and base64url-encodes it for the Gmail API's {@code
     * raw} field. The {@code From} header is deliberately omitted - the Gmail API always sends
     * as the OAuth-authenticated account regardless, and supplying a mismatched value would be
     * rejected or silently overridden.
     *
     * @param toEmail the recipient's email address
     * @param subject the email subject line
     * @param body the plain-text email body
     * @return the base64url-encoded (no padding) MIME message
     */
    private String buildRawMessage(String toEmail, String subject, String body) {
        String message = "To: " + toEmail + "\r\n"
                + "Subject: " + subject + "\r\n"
                + "MIME-Version: 1.0\r\n"
                + "Content-Type: text/plain; charset=\"UTF-8\"\r\n"
                + "\r\n"
                + body;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(message.getBytes(StandardCharsets.UTF_8));
    }

    private static SimpleClientHttpRequestFactory timeoutRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(TIMEOUT_MS);
        factory.setReadTimeout(TIMEOUT_MS);
        return factory;
    }

    /**
     * Shape of Google's OAuth2 token-endpoint response, deserialized for its {@code access_token}
     * field only - {@code expires_in}/{@code token_type} aren't used since the token isn't cached.
     */
    private record GoogleTokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") int expiresIn,
            @JsonProperty("token_type") String tokenType) {
    }
}
