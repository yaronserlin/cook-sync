package com.cooksync_server.config;

import java.io.ByteArrayInputStream;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;

import lombok.extern.slf4j.Slf4j;

/**
 * Configuration class initializing the Firebase Admin SDK client bean used to send push
 * notifications (device registration, system announcements). Mirrors {@link CloudinaryConfig}'s
 * pattern: the service-account credential defaults to blank rather than failing application
 * startup, so the app still boots without a Firebase project configured — this bean simply isn't
 * created, and {@code PushNotificationServiceImp} (injecting it as
 * {@code Optional<FirebaseMessaging>}) detects the absence itself and skips send attempts,
 * logging a warning instead, exactly like {@code EmailServiceImp} already does for its own
 * optional external dependency.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/09/2026
 */
@Slf4j
@Configuration
public class FirebaseConfig {

    /**
     * The Firebase service account's credential JSON, <strong>base64-encoded</strong> — the full
     * contents of the key file downloaded from the Firebase console (Project settings &gt;
     * Service accounts &gt; Generate new private key), base64-encoded into one line — from the
     * {@code FIREBASE_SERVICE_ACCOUNT_JSON} env var. Left blank in any environment without a
     * Firebase project provisioned.
     *
     * <p>
     * Base64 rather than raw JSON deliberately: this app's local {@code .env} file is loaded by
     * Spring as a {@code .properties} file (see {@code application.properties}'s {@code
     * spring.config.import}), whose parser un-escapes backslash sequences like the literal
     * {@code \n} that appears dozens of times inside the JSON's {@code private_key} field —
     * silently turning them into real newline characters and corrupting the key before it ever
     * reaches this class. Base64 text contains no backslashes (or any other properties-file
     * escape trigger), so it survives that parse identically whether it comes from the local
     * {@code .env} file or a real platform env var (Render, Docker, ...).
     */
    @Value("${FIREBASE_SERVICE_ACCOUNT_JSON:}")
    private String serviceAccountJsonBase64;

    /**
     * Initializes the Firebase Admin SDK and exposes a {@link FirebaseMessaging} client bean —
     * or no bean at all if {@link #serviceAccountJsonBase64} is blank or fails to decode/parse as
     * valid credentials, in which case a {@code null} return here means Spring registers no bean
     * for this type at all.
     *
     * @return configured FirebaseMessaging client instance, or {@code null} if not configured
     */
    @Bean
    public FirebaseMessaging firebaseMessaging() {
        if (!StringUtils.hasText(serviceAccountJsonBase64)) {
            log.warn("FIREBASE_SERVICE_ACCOUNT_JSON is not set; push notifications are disabled");
            return null;
        }
        try {
            byte[] serviceAccountJson = Base64.getDecoder().decode(serviceAccountJsonBase64.trim());
            GoogleCredentials credentials = GoogleCredentials.fromStream(new ByteArrayInputStream(serviceAccountJson));

            // FirebaseOptions doesn't reliably auto-populate the project ID from setCredentials()
            // alone (observed: getProjectId() came back null despite valid credentials) — read it
            // directly from the same JSON instead of depending on that.
            JsonNode root = new ObjectMapper().readTree(serviceAccountJson);
            String projectId = root.path("project_id").asText(null);

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .setProjectId(projectId)
                    .build();
            FirebaseApp app = FirebaseApp.getApps().isEmpty()
                    ? FirebaseApp.initializeApp(options)
                    : FirebaseApp.getInstance();
            log.info("Firebase Admin SDK initialized for project '{}'; push notifications are enabled", projectId);
            return FirebaseMessaging.getInstance(app);
        } catch (Exception e) {
            log.warn("Failed to initialize the Firebase Admin SDK from FIREBASE_SERVICE_ACCOUNT_JSON; "
                    + "push notifications are disabled: {}", e.getMessage());
            return null;
        }
    }
}
