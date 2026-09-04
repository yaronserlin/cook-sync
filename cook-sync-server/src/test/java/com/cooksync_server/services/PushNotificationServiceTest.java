package com.cooksync_server.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cooksync_server.entities.DeviceToken;
import com.cooksync_server.repositories.DeviceTokenRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;

/**
 * Unit test suite verifying push-notification sending in PushNotificationServiceImp, especially
 * its degrade-gracefully behavior when Firebase isn't configured (mirrors EmailServiceImp's
 * pattern for its own optional external dependency) and its cleanup of no-longer-valid tokens.
 * {@link #notificationExecutor} is a same-thread executor rather than a mock, so
 * {@link PushNotificationServiceImp#broadcast} runs synchronously and deterministically in tests.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/09/2026
 */
@ExtendWith(MockitoExtension.class)
class PushNotificationServiceTest {

    @Mock
    private DeviceTokenRepository deviceTokenRepository;
    @Mock
    private FirebaseMessaging firebaseMessaging;

    private final Executor notificationExecutor = Runnable::run;

    @Test
    void sendToDevice_ShouldReturnFalse_WhenFirebaseNotConfigured() throws Exception {
        PushNotificationServiceImp service = new PushNotificationServiceImp(
                Optional.empty(), deviceTokenRepository, notificationExecutor);

        boolean sent = service.sendToDevice("token-abc", "Title", "Body");

        assertFalse(sent);
        verify(deviceTokenRepository, never()).deleteByPushToken(any());
    }

    @Test
    void sendToDevice_ShouldReturnTrue_WhenSendSucceeds() throws Exception {
        when(firebaseMessaging.send(any(Message.class))).thenReturn("message-id-1");
        PushNotificationServiceImp service = new PushNotificationServiceImp(
                Optional.of(firebaseMessaging), deviceTokenRepository, notificationExecutor);

        boolean sent = service.sendToDevice("token-abc", "Title", "Body");

        assertTrue(sent);
        verify(deviceTokenRepository, never()).deleteByPushToken(any());
    }

    @Test
    void sendToDevice_ShouldRemoveToken_WhenTokenIsUnregistered() throws Exception {
        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
        when(exception.getMessagingErrorCode()).thenReturn(MessagingErrorCode.UNREGISTERED);
        when(firebaseMessaging.send(any(Message.class))).thenThrow(exception);
        PushNotificationServiceImp service = new PushNotificationServiceImp(
                Optional.of(firebaseMessaging), deviceTokenRepository, notificationExecutor);

        boolean sent = service.sendToDevice("token-abc", "Title", "Body");

        assertFalse(sent);
        verify(deviceTokenRepository).deleteByPushToken("token-abc");
    }

    @Test
    void sendToDevice_ShouldNotRemoveToken_WhenFailureIsNotAnInvalidTokenError() throws Exception {
        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
        when(exception.getMessagingErrorCode()).thenReturn(MessagingErrorCode.INTERNAL);
        when(firebaseMessaging.send(any(Message.class))).thenThrow(exception);
        PushNotificationServiceImp service = new PushNotificationServiceImp(
                Optional.of(firebaseMessaging), deviceTokenRepository, notificationExecutor);

        boolean sent = service.sendToDevice("token-abc", "Title", "Body");

        assertFalse(sent);
        verify(deviceTokenRepository, never()).deleteByPushToken(any());
    }

    @Test
    void broadcast_ShouldSendToEveryEligibleDevice() throws Exception {
        DeviceToken deviceOne = DeviceToken.builder().pushToken("token-1").platform("ANDROID").build();
        DeviceToken deviceTwo = DeviceToken.builder().pushToken("token-2").platform("ANDROID").build();
        when(deviceTokenRepository.findAllEligibleForBroadcast()).thenReturn(List.of(deviceOne, deviceTwo));
        when(firebaseMessaging.send(any(Message.class))).thenReturn("message-id");
        PushNotificationServiceImp service = new PushNotificationServiceImp(
                Optional.of(firebaseMessaging), deviceTokenRepository, notificationExecutor);

        service.broadcast("Title", "Body");

        verify(deviceTokenRepository).findAllEligibleForBroadcast();
        verify(firebaseMessaging, times(2)).send(any(Message.class));
    }

    @Test
    void broadcast_ShouldNotThrow_WhenFirebaseNotConfigured() {
        // Broadcasting with no Firebase configured should degrade gracefully (per-device skip +
        // warning log) rather than fail the whole broadcast.
        DeviceToken deviceOne = DeviceToken.builder().pushToken("token-1").platform("ANDROID").build();
        when(deviceTokenRepository.findAllEligibleForBroadcast()).thenReturn(List.of(deviceOne));
        PushNotificationServiceImp service = new PushNotificationServiceImp(
                Optional.empty(), deviceTokenRepository, notificationExecutor);

        service.broadcast("Title", "Body");

        verify(deviceTokenRepository).findAllEligibleForBroadcast();
    }
}
