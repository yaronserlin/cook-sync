package com.cooksync_server.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cooksync_server.entities.DeviceToken;
import com.cooksync_server.entities.User;
import com.cooksync_server.exceptions.ResourceNotFoundException;
import com.cooksync_server.repositories.DeviceTokenRepository;
import com.cooksync_server.repositories.UserRepository;

/**
 * Unit test suite verifying per-device push-notification token registration and removal in
 * DeviceTokenServiceImp — in particular the upsert-by-token behavior that distinguishes it from
 * the one-row-per-user pattern used elsewhere (e.g. refresh tokens).
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/09/2026
 */
@ExtendWith(MockitoExtension.class)
class DeviceTokenServiceTest {

    @Mock
    private DeviceTokenRepository deviceTokenRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DeviceTokenServiceImp deviceTokenService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id("user-1").email("gordon@cooksync.com").build();
    }

    @Test
    void register_ShouldThrowResourceNotFoundException_WhenUserMissing() {
        when(userRepository.findByEmail("missing@cooksync.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> deviceTokenService.register("missing@cooksync.com", "token-abc", "ANDROID"));

        verify(deviceTokenRepository, never()).save(any());
    }

    @Test
    void register_ShouldCreateNewDeviceToken_WhenTokenNotAlreadyRegistered() {
        when(userRepository.findByEmail("gordon@cooksync.com")).thenReturn(Optional.of(user));
        when(deviceTokenRepository.findByPushToken("token-abc")).thenReturn(Optional.empty());

        deviceTokenService.register("gordon@cooksync.com", "token-abc", "ANDROID");

        ArgumentCaptor<DeviceToken> captor = ArgumentCaptor.forClass(DeviceToken.class);
        verify(deviceTokenRepository).save(captor.capture());
        DeviceToken saved = captor.getValue();
        assertEquals(user, saved.getUser());
        assertEquals("token-abc", saved.getPushToken());
        assertEquals("ANDROID", saved.getPlatform());
    }

    @Test
    void register_ShouldUpdateExistingRow_WhenTokenAlreadyRegistered() {
        // Covers the case a token is re-registered under a different user (e.g. a shared/reset
        // device, or a fresh login after logout) — must UPSERT by pushToken, never insert a
        // second row for the same token, since pushToken is the unique key (not user_id).
        User otherUser = User.builder().id("user-2").email("julia@cooksync.com").build();
        DeviceToken existing = DeviceToken.builder().id("dt-1").user(otherUser).pushToken("token-abc").platform("ANDROID").build();
        when(userRepository.findByEmail("gordon@cooksync.com")).thenReturn(Optional.of(user));
        when(deviceTokenRepository.findByPushToken("token-abc")).thenReturn(Optional.of(existing));

        deviceTokenService.register("gordon@cooksync.com", "token-abc", "ANDROID");

        ArgumentCaptor<DeviceToken> captor = ArgumentCaptor.forClass(DeviceToken.class);
        verify(deviceTokenRepository).save(captor.capture());
        DeviceToken saved = captor.getValue();
        assertEquals("dt-1", saved.getId());
        assertEquals(user, saved.getUser());
    }

    @Test
    void unregister_ShouldDeleteByPushToken() {
        deviceTokenService.unregister("token-abc");

        verify(deviceTokenRepository).deleteByPushToken("token-abc");
    }
}
