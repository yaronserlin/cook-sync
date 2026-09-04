package com.cooksync_server.services;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;

import org.springframework.stereotype.Service;

import com.cooksync_server.entities.DeviceToken;
import com.cooksync_server.repositories.DeviceTokenRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service implementation sending push notifications via the Firebase Admin SDK. Mirrors
 * {@code EmailServiceImp}'s degrade-gracefully approach: {@link #firebaseMessaging} is injected
 * as an {@link Optional} (see {@code FirebaseConfig}), and every send is skipped with a warning
 * log — never thrown — when it's empty, i.e. when Firebase isn't configured.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/09/2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PushNotificationServiceImp implements PushNotificationService {

    private final Optional<FirebaseMessaging> firebaseMessaging;
    private final DeviceTokenRepository deviceTokenRepository;

    /**
     * The bounded broadcast executor from {@code AsyncConfig} — currently the only
     * {@link Executor}-typed bean in the context, so type-based injection resolves it
     * unambiguously without needing a {@code @Qualifier}.
     */
    private final Executor notificationExecutor;

    @Override
    public boolean sendToDevice(String pushToken, String title, String body) {
        if (firebaseMessaging.isEmpty()) {
            log.warn("Firebase is not configured; skipping push send to {}", maskToken(pushToken));
            return false;
        }
        Message message = Message.builder()
                .setToken(pushToken)
                .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                .build();
        try {
            firebaseMessaging.get().send(message);
            return true;
        } catch (FirebaseMessagingException e) {
            if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED
                    || e.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT) {
                log.info("Push token no longer valid; removing it: {}", maskToken(pushToken));
                deviceTokenRepository.deleteByPushToken(pushToken);
            } else {
                log.warn("Failed to send push to {}: {}", maskToken(pushToken), e.getMessage());
            }
            return false;
        }
    }

    @Override
    public void broadcast(String title, String body) {
        notificationExecutor.execute(() -> {
            List<DeviceToken> targets = deviceTokenRepository.findAllEligibleForBroadcast();
            log.info("Broadcasting '{}' to {} device(s)", title, targets.size());
            for (DeviceToken deviceToken : targets) {
                sendToDevice(deviceToken.getPushToken(), title, body);
            }
        });
    }

    /**
     * Redacts a push token down to its first/last few characters for safe logging.
     *
     * @param pushToken the token to mask
     * @return a masked representation safe to write to logs
     */
    private String maskToken(String pushToken) {
        if (pushToken == null || pushToken.length() < 8) {
            return "***";
        }
        return pushToken.substring(0, 4) + "…" + pushToken.substring(pushToken.length() - 4);
    }
}
