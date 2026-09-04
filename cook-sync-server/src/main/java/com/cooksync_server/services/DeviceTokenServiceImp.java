package com.cooksync_server.services;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cooksync_server.constants.EntityNames;
import com.cooksync_server.entities.DeviceToken;
import com.cooksync_server.entities.User;
import com.cooksync_server.exceptions.ResourceNotFoundException;
import com.cooksync_server.repositories.DeviceTokenRepository;
import com.cooksync_server.repositories.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service class managing per-device push-notification token registration and removal.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/09/2026
 */
@Service
@RequiredArgsConstructor
public class DeviceTokenServiceImp implements DeviceTokenService {

    private final DeviceTokenRepository deviceTokenRepository;
    private final UserRepository userRepository;

    /**
     * {@inheritDoc}
     *
     * @throws ResourceNotFoundException if no user with the given email exists
     */
    @Override
    @Transactional
    public void register(String userEmail, String pushToken, String platform) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException(EntityNames.USER, userEmail));

        Optional<DeviceToken> existing = deviceTokenRepository.findByPushToken(pushToken);
        if (existing.isPresent()) {
            DeviceToken deviceToken = existing.get();
            deviceToken.setUser(user);
            deviceToken.setPlatform(platform);
            deviceTokenRepository.save(deviceToken);
            return;
        }

        DeviceToken deviceToken = DeviceToken.builder()
                .user(user)
                .pushToken(pushToken)
                .platform(platform)
                .build();
        deviceTokenRepository.save(deviceToken);
    }

    @Override
    @Transactional
    public void unregister(String pushToken) {
        deviceTokenRepository.deleteByPushToken(pushToken);
    }
}
