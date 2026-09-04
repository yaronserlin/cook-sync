package com.cooksync_server.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cooksync_server.services.DeviceTokenService;
import com.dtos.request.device.DeviceTokenRegisterRequestDTO;
import com.dtos.response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST Controller managing per-device push-notification token registration and removal.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/09/2026
 */
@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceTokenService deviceTokenService;

    /**
     * Registers (or refreshes) the calling device's push-notification token for the
     * authenticated user. Called after login and on every app launch.
     *
     * @param request the device's current push token and platform
     * @param authentication active user authentication token
     * @return response entity acknowledging registration
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> registerDevice(
            @Valid @RequestBody DeviceTokenRegisterRequestDTO request,
            Authentication authentication) {
        deviceTokenService.register(authentication.getName(), request.pushToken(), request.platform());
        return ResponseEntity.ok(ApiResponse.success(null, "Device registered successfully"));
    }

    /**
     * Removes a device's push-notification token registration, e.g. on logout.
     *
     * @param pushToken the device's FCM registration token
     * @return response entity acknowledging removal
     */
    @DeleteMapping("/{pushToken}")
    public ResponseEntity<ApiResponse<Void>> unregisterDevice(@PathVariable String pushToken) {
        deviceTokenService.unregister(pushToken);
        return ResponseEntity.ok(ApiResponse.success(null, "Device unregistered successfully"));
    }
}
