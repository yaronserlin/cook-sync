package com.cooksync_server.services;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cooksync_server.config.JwtUtil;
import com.dtos.request.auth.LoginRequestDTO;
import com.dtos.request.auth.RegisterRequestDTO;
import com.dtos.request.auth.ResendRegistrationOtpRequestDTO;
import com.dtos.request.auth.TokenRefreshRequestDTO;
import com.dtos.request.auth.VerifyRegistrationOtpRequestDTO;
import com.dtos.response.auth.AuthResponse;
import com.dtos.response.auth.PendingRegistrationResponse;
import com.cooksync_server.entities.PendingRegistration;
import com.cooksync_server.entities.RefreshToken;
import com.cooksync_server.entities.User;
import com.cooksync_server.exceptions.ResourceNotFoundException;
import com.cooksync_server.exceptions.auth.InvalidCredentialsException;
import com.cooksync_server.exceptions.auth.InvalidOtpException;
import com.cooksync_server.exceptions.auth.OtpExpiredException;
import com.cooksync_server.exceptions.auth.TooManyOtpAttemptsException;
import com.cooksync_server.exceptions.auth.UnauthorizedActionException;
import com.cooksync_server.exceptions.auth.UserAlreadyExistsException;
import com.cooksync_server.repositories.PendingRegistrationRepository;
import com.cooksync_server.repositories.UserRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Service class handling user registration (with OTP verification) and session/token
 * authentication: login, refresh-token rotation, and token validation. Profile management lives
 * in {@link UserProfileServiceImp}; password change/reset lives in {@link PasswordServiceImp}.
 * Includes SLF4J structured logging for monitoring security events.
 *
 * @author Yaron Serlin
 * @version 2.0
 * @since 02/08/2026
 */
@Slf4j
@Service
public class AuthServiceImp implements AuthService {

    /** Grace period after a deletion request during which logging back in restores the account. */
    private static final long DELETION_GRACE_PERIOD_DAYS = 30;

    /** How many minutes a registration OTP code remains valid after being issued or resent. */
    private static final int OTP_VALIDITY_MINUTES = 10;

    /** {@link #OTP_VALIDITY_MINUTES} expressed in milliseconds, for {@link Instant} arithmetic. */
    private static final long OTP_VALIDITY_MS = OTP_VALIDITY_MINUTES * 60 * 1000L;

    /** Grace period past OTP expiry before an abandoned pending registration is purged. */
    private static final long PENDING_REGISTRATION_PURGE_GRACE_DAYS = 1;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final PendingRegistrationRepository pendingRegistrationRepository;
    private final EmailService emailService;
    private final AccountDeletionService accountDeletionService;
    private final String dummyPasswordHash;

    /**
     * Constructs AuthServiceImp with required dependencies and initializes timing attack dummy hash.
     *
     * @param userRepository repository for user persistence
     * @param passwordEncoder encoder for BCrypt password hashing
     * @param jwtUtil utility for JWT generation and verification
     * @param refreshTokenService service for managing session refresh tokens
     * @param pendingRegistrationRepository repository for unverified registration attempts
     * @param emailService service used to deliver registration OTP emails
     * @param accountDeletionService service handling the self-service account-deletion lifecycle
     */
    public AuthServiceImp(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil,
            RefreshTokenService refreshTokenService, PendingRegistrationRepository pendingRegistrationRepository,
            EmailService emailService, AccountDeletionService accountDeletionService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
        this.pendingRegistrationRepository = pendingRegistrationRepository;
        this.emailService = emailService;
        this.accountDeletionService = accountDeletionService;
        this.dummyPasswordHash = passwordEncoder.encode("dummy-password-for-timing-protection");
    }

    /**
     * Initiates registration for a new account: validates the email is not already registered,
     * stores the submitted profile details and a hashed password in a pending state, and emails
     * a one-time 6-digit verification code. No account is created and no tokens are issued until
     * the code is confirmed via {@link #verifyRegistrationOtp(VerifyRegistrationOtpRequestDTO)} —
     * if the user never completes verification, nothing survives past the pending row's expiry.
     *
     * @param request registration details payload
     * @return PendingRegistrationResponse acknowledging the pending registration and OTP expiry
     */
    @Transactional
    public PendingRegistrationResponse register(RegisterRequestDTO request) {
        log.info("Processing user registration attempt for email: {}", request.email());
        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException("Email is already registered");
        }

        pendingRegistrationRepository.deleteByEmail(request.email());

        String otpCode = OtpCodeGenerator.generate();
        PendingRegistration pendingRegistration = PendingRegistration.builder()
                .email(request.email())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .passwordHash(passwordEncoder.encode(request.password()))
                .termsAccepted(request.termsAccepted())
                .marketingOptIn(request.marketingOptIn())
                .otpCodeHash(passwordEncoder.encode(otpCode))
                .otpExpiresAt(Instant.now().plusMillis(OTP_VALIDITY_MS))
                .build();

        try {
            pendingRegistrationRepository.save(pendingRegistration);
        } catch (DataIntegrityViolationException e) {
            throw new UserAlreadyExistsException("Email is already registered");
        }

        emailService.sendOtpEmail(request.email(), otpCode, OTP_VALIDITY_MINUTES);

        log.info("Registration OTP issued for email: {}", request.email());
        return new PendingRegistrationResponse(request.email(), OTP_VALIDITY_MS / 1000);
    }

    /**
     * Completes registration by validating a submitted OTP code against its pending
     * registration. On success, creates the real user account from the pending data, deletes the
     * pending row, and issues initial access and refresh tokens exactly as registration did
     * prior to the OTP step. Incorrect codes increment the pending registration's attempt count;
     * once {@link OtpCodeGenerator#MAX_ATTEMPTS} incorrect attempts accumulate, the pending
     * registration is invalidated and the user must submit the registration form again for a
     * fresh code.
     *
     * @param request OTP verification payload
     * @return AuthResponse containing access token, refresh token, and user info
     */
    @Transactional(noRollbackFor = {InvalidOtpException.class, TooManyOtpAttemptsException.class})
    public AuthResponse verifyRegistrationOtp(VerifyRegistrationOtpRequestDTO request) {
        PendingRegistration pending = pendingRegistrationRepository.findByEmail(request.email())
                .orElseThrow(() -> new InvalidOtpException("No pending registration found for this email"));

        if (pending.getOtpExpiresAt().isBefore(Instant.now())) {
            throw new OtpExpiredException("Verification code has expired");
        }

        if (!passwordEncoder.matches(request.code(), pending.getOtpCodeHash())) {
            pending.setAttemptCount(pending.getAttemptCount() + 1);
            if (pending.getAttemptCount() >= OtpCodeGenerator.MAX_ATTEMPTS) {
                pendingRegistrationRepository.delete(pending);
                throw new TooManyOtpAttemptsException("Too many incorrect attempts. Please register again.");
            }
            pendingRegistrationRepository.save(pending);
            throw new InvalidOtpException("Incorrect verification code");
        }

        User newUser = User.builder()
                .firstName(pending.getFirstName())
                .lastName(pending.getLastName())
                .email(pending.getEmail())
                .passwordHash(pending.getPasswordHash())
                .isAdmin(false)
                .termsAccepted(pending.isTermsAccepted())
                .marketingOptIn(pending.isMarketingOptIn())
                .build();

        try {
            userRepository.save(newUser);
        } catch (DataIntegrityViolationException e) {
            throw new UserAlreadyExistsException("Email is already registered");
        }
        pendingRegistrationRepository.delete(pending);

        String token = jwtUtil.generateToken(newUser.getEmail(), newUser.getId(), newUser.isAdmin());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(newUser.getId());

        log.info("User registered successfully with ID: {}", newUser.getId());
        return new AuthResponse(token, refreshToken.getToken(), newUser.getId(), newUser.getFirstName(), newUser.getLastName(), newUser.isAdmin(), newUser.getAvatarUrl());
    }

    /**
     * Regenerates and re-emails a fresh OTP code for an existing pending registration,
     * restarting its expiry window and resetting its incorrect-attempt count. Used when the
     * previous code expired or was not received.
     *
     * @param request resend request payload
     * @return PendingRegistrationResponse acknowledging the newly issued OTP and its expiry
     */
    @Transactional
    public PendingRegistrationResponse resendRegistrationOtp(ResendRegistrationOtpRequestDTO request) {
        PendingRegistration pending = pendingRegistrationRepository.findByEmail(request.email())
                .orElseThrow(() -> new InvalidOtpException("No pending registration found for this email"));

        String otpCode = OtpCodeGenerator.generate();
        pending.setOtpCodeHash(passwordEncoder.encode(otpCode));
        pending.setOtpExpiresAt(Instant.now().plusMillis(OTP_VALIDITY_MS));
        pending.setAttemptCount(0);
        pendingRegistrationRepository.save(pending);

        emailService.sendOtpEmail(request.email(), otpCode, OTP_VALIDITY_MINUTES);

        log.info("Registration OTP resent for email: {}", request.email());
        return new PendingRegistrationResponse(request.email(), OTP_VALIDITY_MS / 1000);
    }

    /**
     * Purges every pending registration whose OTP has been expired for at least
     * {@link #PENDING_REGISTRATION_PURGE_GRACE_DAYS}, cleaning up abandoned registration
     * attempts that were never verified.
     *
     * Complexity:
     * Time: O(P) where P is expired pending-registration row count
     * Space: O(1)
     */
    @Transactional
    public void purgeExpiredPendingRegistrations() {
        Instant cutoff = Instant.now().minus(PENDING_REGISTRATION_PURGE_GRACE_DAYS, ChronoUnit.DAYS);
        pendingRegistrationRepository.deleteByOtpExpiresAtBefore(cutoff);
        log.info("Purged expired pending registrations older than {}", cutoff);
    }

    /**
     * Authenticates user credentials with constant-time password comparison to prevent timing attacks.
     *
     * @param request login credentials payload
     * @return AuthResponse containing fresh tokens and user info
     */
    @Transactional
    public AuthResponse login(LoginRequestDTO request) {
        log.info("Processing user login attempt for email: {}", request.email());
        Optional<User> optionalUser = userRepository.findByEmail(request.email());

        String hashToTest = optionalUser.map(User::getPasswordHash).orElse(this.dummyPasswordHash);
        boolean isPasswordMatch = passwordEncoder.matches(request.password(), hashToTest);

        if (optionalUser.isEmpty() || !isPasswordMatch) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        User user = optionalUser.get();
        if (!user.isEnabled()) {
            if (isWithinDeletionGracePeriod(user)) {
                log.info("Login during deletion grace period - restoring account ID: {}", user.getId());
                accountDeletionService.restoreFromPendingDeletion(user);
            } else {
                throw new UnauthorizedActionException("This account has been disabled.");
            }
        }
        String token = jwtUtil.generateToken(user.getEmail(), user.getId(), user.isAdmin());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

        log.info("User logged in successfully with ID: {}", user.getId());
        return new AuthResponse(token, refreshToken.getToken(), user.getId(), user.getFirstName(), user.getLastName(), user.isAdmin(), user.getAvatarUrl());
    }

    /**
     * Renews the access token using a valid refresh token payload. The refresh token itself is
     * rotated on every use: the presented token is deleted and replaced with a newly issued one
     * (via {@link RefreshTokenServiceImp#createRefreshToken(String)}, which already deletes any
     * prior token for the user before saving the new one). Because only one refresh token per
     * user is ever valid at a time, replaying an already-rotated token fails immediately with
     * {@link UnauthorizedActionException} the next time it is presented, since it no longer
     * exists in the database.
     *
     * @param request refresh token request payload
     * @return AuthResponse containing new access token and newly rotated refresh token
     */
    @Transactional
    public AuthResponse refreshToken(TokenRefreshRequestDTO request) {
        String requestRefreshToken = request.refreshToken();

        return refreshTokenService.findByToken(requestRefreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    String token = jwtUtil.generateToken(user.getEmail(), user.getId(), user.isAdmin());
                    RefreshToken rotatedRefreshToken = refreshTokenService.createRefreshToken(user.getId());
                    return new AuthResponse(token, rotatedRefreshToken.getToken(), user.getId(), user.getFirstName(), user.getLastName(), user.isAdmin(), user.getAvatarUrl());
                })
                .orElseThrow(() -> new UnauthorizedActionException("Refresh token is not in database or is invalid!"));
    }

    /**
     * Validates active JWT token context and returns user profile details without issuing new tokens.
     *
     * @param userEmail authenticated user email
     * @return AuthResponse with profile details
     */
    @Transactional(readOnly = true)
    public AuthResponse validateToken(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", userEmail));

        return new AuthResponse(null, null, user.getId(), user.getFirstName(), user.getLastName(), user.isAdmin(), user.getAvatarUrl());
    }

    /**
     * Revokes active user refresh tokens upon logout.
     *
     * @param userEmail authenticated user email
     */
    @Transactional
    public void logout(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", userEmail));
        refreshTokenService.deleteByUserId(user.getId());
    }

    /**
     * Determines whether a disabled account is still within its 30-day account-deletion grace
     * period and therefore eligible to be restored by logging back in, as opposed to a plain
     * deactivation (never self-service restorable) or an already-lapsed deletion request (the
     * scheduled purge job should have already erased it, but login is rejected defensively
     * either way since {@link #login(LoginRequestDTO)} only reaches this check for existing rows).
     *
     * @param user the disabled account attempting to log in
     * @return true if the account has a pending deletion request within the grace period
     */
    private boolean isWithinDeletionGracePeriod(User user) {
        return user.getStatus() == User.AccountStatus.DEACTIVATED
                && user.getDeletionRequestedAt() != null
                && user.getDeletionRequestedAt().isAfter(LocalDateTime.now().minusDays(DELETION_GRACE_PERIOD_DAYS));
    }
}
