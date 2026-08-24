package com.cooksync_server.services;

import org.springframework.stereotype.Component;

import com.cooksync_server.config.JwtUtil;
import com.cooksync_server.entities.RefreshToken;
import com.cooksync_server.entities.User;
import com.dtos.response.auth.AuthResponse;

import lombok.RequiredArgsConstructor;

/**
 * Issues a fresh authenticated session — a signed JWT access token plus a newly created refresh
 * token — for a given {@link User}, and packages both into the {@link AuthResponse} payload
 * every session-issuing endpoint returns. Shared by every flow that hands the caller a live
 * session: initial registration, login, refresh-token rotation, and a confirmed email change.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 24/08/2026
 */
@Component
@RequiredArgsConstructor
public class SessionIssuer {

    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    /**
     * Issues a new access token and refresh token for the given user, revoking any refresh token
     * previously issued to them.
     *
     * @param user the account to issue a session for
     * @return authentication payload carrying the new tokens and the user's profile summary
     */
    public AuthResponse issue(User user) {
        String token = jwtUtil.generateToken(user.getEmail(), user.getId(), user.isAdmin());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());
        return new AuthResponse(token, refreshToken.getToken(), user.getId(), user.getFirstName(), user.getLastName(), user.isAdmin(), user.getAvatarUrl());
    }
}
