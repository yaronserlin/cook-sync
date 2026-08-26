package com.cooksync_server.services;

import com.dtos.request.auth.LoginRequestDTO;
import com.dtos.request.auth.RegisterRequestDTO;
import com.dtos.request.auth.ResendRegistrationOtpRequestDTO;
import com.dtos.request.auth.TokenRefreshRequestDTO;
import com.dtos.request.auth.VerifyRegistrationOtpRequestDTO;
import com.dtos.response.auth.AuthResponse;
import com.dtos.response.auth.PendingRegistrationResponse;

/**
 * Service interface for user registration and session/token authentication: sign-up with OTP
 * verification, login, refresh-token rotation, and token validation.
 *
 * @author Yaron Serlin
 * @version 2.0
 * @since 02/08/2026
 */
public interface AuthService {

    /**
     * Initiates registration for a new account: stores the submitted details in a pending state
     * and emails a one-time OTP code. No account is created and no tokens are issued until the
     * code is confirmed via {@link #verifyRegistrationOtp(VerifyRegistrationOtpRequestDTO)}.
     *
     * @param request registration details payload
     * @return PendingRegistrationResponse acknowledging the pending registration and OTP expiry
     * @throws com.cooksync_server.exceptions.auth.UserAlreadyExistsException if the email is already registered
     */
    PendingRegistrationResponse register(RegisterRequestDTO request);

    /**
     * Completes registration by validating a submitted OTP code against its pending
     * registration. On success, creates the real user account and issues initial access and
     * refresh tokens.
     *
     * @param request OTP verification payload
     * @return AuthResponse containing access token, refresh token, and user info
     * @throws com.cooksync_server.exceptions.auth.InvalidOtpException if no pending registration exists for the email, or the submitted code does not match
     * @throws com.cooksync_server.exceptions.auth.OtpExpiredException if the pending registration's code has expired
     * @throws com.cooksync_server.exceptions.auth.TooManyOtpAttemptsException if the incorrect-attempt limit for the pending code has just been exceeded by this call
     * @throws com.cooksync_server.exceptions.auth.UserAlreadyExistsException if the email became registered to another account in the meantime
     */
    AuthResponse verifyRegistrationOtp(VerifyRegistrationOtpRequestDTO request);

    /**
     * Regenerates and re-emails a fresh OTP code for an existing pending registration.
     *
     * @param request resend request payload
     * @return PendingRegistrationResponse acknowledging the newly issued OTP and its expiry
     * @throws com.cooksync_server.exceptions.auth.InvalidOtpException if no pending registration exists for the email
     */
    PendingRegistrationResponse resendRegistrationOtp(ResendRegistrationOtpRequestDTO request);

    /**
     * Purges every pending registration whose OTP expired at least a day ago, cleaning up
     * abandoned registration attempts that were never verified.
     */
    void purgeExpiredPendingRegistrations();

    /**
     * Authenticates user credentials and issues fresh access and refresh tokens.
     *
     * @param request login credentials payload
     * @return AuthResponse containing fresh tokens and user info
     * @throws com.cooksync_server.exceptions.auth.InvalidCredentialsException if the email or password does not match
     * @throws com.cooksync_server.exceptions.auth.UnauthorizedActionException if the account is disabled and outside its deletion grace period
     */
    AuthResponse login(LoginRequestDTO request);

    /**
     * Renews the access token using a valid refresh token payload, rotating the refresh token
     * on every use.
     *
     * @param request refresh token request payload
     * @return AuthResponse containing the new access token and rotated refresh token
     * @throws com.cooksync_server.exceptions.auth.UnauthorizedActionException if the refresh token is not in the database or is invalid
     */
    AuthResponse refreshToken(TokenRefreshRequestDTO request);

    /**
     * Validates the active JWT authentication context and returns basic user profile details
     * without issuing new tokens.
     *
     * @param userEmail authenticated user email
     * @return AuthResponse with profile details
     * @throws com.cooksync_server.exceptions.ResourceNotFoundException if no user matches {@code userEmail}
     */
    AuthResponse validateToken(String userEmail);

    /**
     * Revokes the user's active refresh token upon logout.
     *
     * @param userEmail authenticated user email
     * @throws com.cooksync_server.exceptions.ResourceNotFoundException if no user matches {@code userEmail}
     */
    void logout(String userEmail);
}
