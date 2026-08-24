package com.cooksync_server.services;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.cooksync_server.entities.User;
import com.cooksync_server.exceptions.ResourceNotFoundException;
import com.cooksync_server.exceptions.auth.InvalidCredentialsException;
import com.cooksync_server.repositories.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Re-verifies an authenticated caller's current password before a sensitive account change.
 * Shared by every flow that requires this re-authentication step (password change, email
 * change, account deletion) so the "look up the account, then confirm the supplied password
 * matches it" check lives in exactly one place.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 24/08/2026
 */
@Component
@RequiredArgsConstructor
public class CredentialVerifier {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Looks up the account by email and confirms the supplied password matches it.
     *
     * @param userEmail authenticated user's email address
     * @param currentPassword the password to verify against the account's stored hash
     * @return the matching user, once the password has been confirmed
     * @throws ResourceNotFoundException if no user matches {@code userEmail}
     * @throws InvalidCredentialsException if {@code currentPassword} does not match
     */
    public User verifyCurrentPassword(String userEmail, String currentPassword) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", userEmail));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }

        return user;
    }
}
