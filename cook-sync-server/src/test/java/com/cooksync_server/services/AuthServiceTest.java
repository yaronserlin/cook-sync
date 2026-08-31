package com.cooksync_server.services;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test suite verifying user registration (pending state + OTP verification/resend) and
 * login/session authentication in AuthServiceImp. Password reset flow tests live in
 * {@link PasswordServiceTest}.
 *
 * @author Yaron Serlin
 * @version 2.0
 * @since 09/08/2026
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RefreshTokenServiceImp refreshTokenService;
    @Mock
    private PendingRegistrationRepository pendingRegistrationRepository;
    @Mock
    private EmailServiceImp emailService;
    @Mock
    private AccountDeletionService accountDeletionService;
    @Mock
    private SessionIssuer sessionIssuer;

    private AuthServiceImp authService;

    @BeforeEach
    void setUp() {
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-password");

        authService = new AuthServiceImp(
                userRepository,
                passwordEncoder,
                refreshTokenService,
                pendingRegistrationRepository,
                emailService,
                accountDeletionService,
                sessionIssuer
        );
    }

    @Test
    void register_ShouldSavePendingRegistrationAndSendOtpEmail_WhenEmailNotRegistered() {
        RegisterRequestDTO request = new RegisterRequestDTO(
                "John", "Doe", "john@example.com", "Password123!", true, false
        );

        when(userRepository.existsByEmail(request.email())).thenReturn(false);

        PendingRegistrationResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("john@example.com", response.email());
        assertTrue(response.otpExpiresInSeconds() > 0);
        verify(pendingRegistrationRepository, times(1)).deleteByEmail("john@example.com");
        verify(pendingRegistrationRepository, times(1)).save(any(PendingRegistration.class));
        verify(emailService, times(1)).sendOtpEmail(eq("john@example.com"), anyString(), anyInt());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_ShouldThrowUserAlreadyExistsException_WhenEmailAlreadyExists() {
        RegisterRequestDTO request = new RegisterRequestDTO(
                "John", "Doe", "john@example.com", "Password123!", true, false
        );

        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> authService.register(request));
        verify(pendingRegistrationRepository, never()).save(any(PendingRegistration.class));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void verifyRegistrationOtp_ShouldCreateUserAndReturnAuthResponse_WhenCodeIsCorrect() {
        VerifyRegistrationOtpRequestDTO request = new VerifyRegistrationOtpRequestDTO("john@example.com", "123456");
        PendingRegistration pending = PendingRegistration.builder()
                .id("pending-id")
                .email("john@example.com")
                .firstName("John")
                .lastName("Doe")
                .passwordHash("hashed-password")
                .termsAccepted(true)
                .marketingOptIn(false)
                .otpCodeHash("hashed-otp")
                .otpExpiresAt(Instant.now().plusSeconds(300))
                .attemptCount(0)
                .build();

        when(pendingRegistrationRepository.findByEmail("john@example.com")).thenReturn(Optional.of(pending));
        when(passwordEncoder.matches("123456", "hashed-otp")).thenReturn(true);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId("generated-user-id");
            return user;
        });
        when(sessionIssuer.issue(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            return new AuthResponse("mock-jwt-token", "mock-refresh-token", user.getId(), user.getFirstName(), user.getLastName(), user.isAdmin(), user.getAvatarUrl());
        });

        AuthResponse response = authService.verifyRegistrationOtp(request);

        assertNotNull(response);
        assertEquals("mock-jwt-token", response.token());
        assertEquals("mock-refresh-token", response.refreshToken());
        assertEquals("generated-user-id", response.userId());
        assertEquals("John", response.firstName());
        verify(userRepository, times(1)).save(any(User.class));
        verify(pendingRegistrationRepository, times(1)).delete(pending);
    }

    @Test
    void verifyRegistrationOtp_ShouldThrowInvalidOtpException_WhenNoPendingRegistrationFound() {
        VerifyRegistrationOtpRequestDTO request = new VerifyRegistrationOtpRequestDTO("unknown@example.com", "123456");
        when(pendingRegistrationRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(InvalidOtpException.class, () -> authService.verifyRegistrationOtp(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void verifyRegistrationOtp_ShouldThrowOtpExpiredException_WhenCodeExpired() {
        VerifyRegistrationOtpRequestDTO request = new VerifyRegistrationOtpRequestDTO("john@example.com", "123456");
        PendingRegistration pending = PendingRegistration.builder()
                .email("john@example.com")
                .otpCodeHash("hashed-otp")
                .otpExpiresAt(Instant.now().minusSeconds(60))
                .attemptCount(0)
                .build();

        when(pendingRegistrationRepository.findByEmail("john@example.com")).thenReturn(Optional.of(pending));

        assertThrows(OtpExpiredException.class, () -> authService.verifyRegistrationOtp(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void verifyRegistrationOtp_ShouldThrowInvalidOtpException_AndIncrementAttempts_WhenCodeIncorrect() {
        VerifyRegistrationOtpRequestDTO request = new VerifyRegistrationOtpRequestDTO("john@example.com", "000000");
        PendingRegistration pending = PendingRegistration.builder()
                .email("john@example.com")
                .otpCodeHash("hashed-otp")
                .otpExpiresAt(Instant.now().plusSeconds(300))
                .attemptCount(0)
                .build();

        when(pendingRegistrationRepository.findByEmail("john@example.com")).thenReturn(Optional.of(pending));
        when(passwordEncoder.matches("000000", "hashed-otp")).thenReturn(false);

        assertThrows(InvalidOtpException.class, () -> authService.verifyRegistrationOtp(request));
        assertEquals(1, pending.getAttemptCount());
        verify(pendingRegistrationRepository, times(1)).save(pending);
        verify(pendingRegistrationRepository, never()).delete(any(PendingRegistration.class));
    }

    @Test
    void verifyRegistrationOtp_ShouldThrowTooManyOtpAttemptsException_AndDeletePending_WhenAttemptsExceeded() {
        VerifyRegistrationOtpRequestDTO request = new VerifyRegistrationOtpRequestDTO("john@example.com", "000000");
        PendingRegistration pending = PendingRegistration.builder()
                .email("john@example.com")
                .otpCodeHash("hashed-otp")
                .otpExpiresAt(Instant.now().plusSeconds(300))
                .attemptCount(4)
                .build();

        when(pendingRegistrationRepository.findByEmail("john@example.com")).thenReturn(Optional.of(pending));
        when(passwordEncoder.matches("000000", "hashed-otp")).thenReturn(false);

        assertThrows(TooManyOtpAttemptsException.class, () -> authService.verifyRegistrationOtp(request));
        verify(pendingRegistrationRepository, times(1)).delete(pending);
        verify(pendingRegistrationRepository, never()).save(any(PendingRegistration.class));
    }

    @Test
    void resendRegistrationOtp_ShouldRegenerateCodeAndSendEmail_WhenPendingRegistrationExists() {
        ResendRegistrationOtpRequestDTO request = new ResendRegistrationOtpRequestDTO("john@example.com");
        PendingRegistration pending = PendingRegistration.builder()
                .email("john@example.com")
                .otpCodeHash("old-hashed-otp")
                .otpExpiresAt(Instant.now().minusSeconds(60))
                .attemptCount(3)
                .build();

        when(pendingRegistrationRepository.findByEmail("john@example.com")).thenReturn(Optional.of(pending));

        PendingRegistrationResponse response = authService.resendRegistrationOtp(request);

        assertNotNull(response);
        assertEquals("john@example.com", response.email());
        assertEquals(0, pending.getAttemptCount());
        verify(pendingRegistrationRepository, times(1)).save(pending);
        verify(emailService, times(1)).sendOtpEmail(eq("john@example.com"), anyString(), anyInt());
    }

    @Test
    void resendRegistrationOtp_ShouldThrowInvalidOtpException_WhenNoPendingRegistrationFound() {
        ResendRegistrationOtpRequestDTO request = new ResendRegistrationOtpRequestDTO("unknown@example.com");
        when(pendingRegistrationRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(InvalidOtpException.class, () -> authService.resendRegistrationOtp(request));
    }

    @Test
    void login_ShouldReturnAuthResponse_WhenCredentialsAreValid() {
        LoginRequestDTO request = new LoginRequestDTO("john@example.com", "Password123!");
        User existingUser = User.builder()
                .id("user-123")
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .passwordHash("hashed-password")
                .enabled(true)
                .isAdmin(false)
                .build();

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("Password123!", "hashed-password")).thenReturn(true);
        when(sessionIssuer.issue(existingUser))
                .thenReturn(new AuthResponse("mock-jwt-token", "mock-refresh-token", "user-123", "John", "Doe", false, null));

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mock-jwt-token", response.token());
        assertEquals("mock-refresh-token", response.refreshToken());
        assertEquals("user-123", response.userId());
    }

    @Test
    void login_ShouldThrowInvalidCredentialsException_WhenPasswordDoesNotMatch() {
        LoginRequestDTO request = new LoginRequestDTO("john@example.com", "WrongPassword");
        User existingUser = User.builder()
                .id("user-123")
                .email("john@example.com")
                .passwordHash("hashed-password")
                .build();

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("WrongPassword", "hashed-password")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void login_ShouldRestoreAccountAndReturnAuthResponse_WhenWithinDeletionGracePeriod() {
        LoginRequestDTO request = new LoginRequestDTO("john@example.com", "Password123!");
        User existingUser = User.builder()
                .id("user-123")
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .passwordHash("hashed-password")
                .enabled(false)
                .status(User.AccountStatus.DEACTIVATED)
                .deletionRequestedAt(LocalDateTime.now().minusDays(5))
                .isAdmin(false)
                .build();

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("Password123!", "hashed-password")).thenReturn(true);
        when(sessionIssuer.issue(existingUser))
                .thenReturn(new AuthResponse("mock-jwt-token", "mock-refresh-token", "user-123", "John", "Doe", false, null));

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mock-jwt-token", response.token());
        verify(accountDeletionService, times(1)).restoreFromPendingDeletion(existingUser);
        verify(sessionIssuer, times(1)).issue(existingUser);
    }

    @Test
    void login_ShouldThrowUnauthorizedActionException_WhenDisabledAccountIsOutsideDeletionGracePeriod() {
        LoginRequestDTO request = new LoginRequestDTO("john@example.com", "Password123!");
        User existingUser = User.builder()
                .id("user-123")
                .email("john@example.com")
                .passwordHash("hashed-password")
                .enabled(false)
                .status(User.AccountStatus.DEACTIVATED)
                .deletionRequestedAt(LocalDateTime.now().minusDays(45))
                .build();

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("Password123!", "hashed-password")).thenReturn(true);

        UnauthorizedActionException exception = assertThrows(UnauthorizedActionException.class,
                () -> authService.login(request));

        assertEquals("This account has been disabled.", exception.getMessage());
        verify(accountDeletionService, never()).restoreFromPendingDeletion(any(User.class));
        verify(sessionIssuer, never()).issue(any(User.class));
    }

    @Test
    void refreshToken_ShouldReturnNewAuthResponse_WhenTokenIsValid() {
        TokenRefreshRequestDTO request = new TokenRefreshRequestDTO("valid-refresh-token");
        User user = User.builder().id("user-123").email("john@example.com").firstName("John").lastName("Doe").build();
        RefreshToken refreshToken = RefreshToken.builder()
                .id("rt-id")
                .token("valid-refresh-token")
                .user(user)
                .expiryDate(Instant.now().plusSeconds(300))
                .build();

        when(refreshTokenService.findByToken("valid-refresh-token")).thenReturn(Optional.of(refreshToken));
        when(refreshTokenService.verifyExpiration(refreshToken)).thenReturn(refreshToken);
        when(sessionIssuer.issue(user))
                .thenReturn(new AuthResponse("new-jwt-token", "new-refresh-token", "user-123", "John", "Doe", false, null));

        AuthResponse response = authService.refreshToken(request);

        assertNotNull(response);
        assertEquals("new-jwt-token", response.token());
        assertEquals("new-refresh-token", response.refreshToken());
        assertEquals("user-123", response.userId());
        verify(sessionIssuer, times(1)).issue(user);
    }

    @Test
    void refreshToken_ShouldThrowUnauthorizedActionException_WhenTokenNotFound() {
        TokenRefreshRequestDTO request = new TokenRefreshRequestDTO("unknown-token");
        when(refreshTokenService.findByToken("unknown-token")).thenReturn(Optional.empty());

        assertThrows(UnauthorizedActionException.class, () -> authService.refreshToken(request));
        verify(sessionIssuer, never()).issue(any(User.class));
    }

    @Test
    void validateToken_ShouldReturnAuthResponseWithProfileDetails_WhenUserExists() {
        User user = User.builder()
                .id("user-123")
                .email("john@example.com")
                .firstName("John")
                .lastName("Doe")
                .isAdmin(true)
                .avatarUrl("avatar.png")
                .build();

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));

        AuthResponse response = authService.validateToken("john@example.com");

        assertNotNull(response);
        assertNull(response.token());
        assertNull(response.refreshToken());
        assertEquals("user-123", response.userId());
        assertEquals("John", response.firstName());
        assertEquals("Doe", response.lastName());
        assertTrue(response.isAdmin());
        assertEquals("avatar.png", response.avatarUrl());
    }

    @Test
    void validateToken_ShouldThrowResourceNotFoundException_WhenUserDoesNotExist() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authService.validateToken("unknown@example.com"));
    }

    @Test
    void logout_ShouldDeleteRefreshTokenForUser_WhenUserExists() {
        User user = User.builder().id("user-123").email("john@example.com").build();
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));

        authService.logout("john@example.com");

        verify(refreshTokenService, times(1)).deleteByUserId("user-123");
    }

    @Test
    void logout_ShouldThrowResourceNotFoundException_WhenUserDoesNotExist() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authService.logout("unknown@example.com"));
        verify(refreshTokenService, never()).deleteByUserId(anyString());
    }

    @Test
    void purgeExpiredPendingRegistrations_ShouldDeleteRegistrationsPastOtpExpiryGracePeriod() {
        ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);

        authService.purgeExpiredPendingRegistrations();

        verify(pendingRegistrationRepository, times(1)).deleteByOtpExpiresAtBefore(cutoffCaptor.capture());
        Instant expectedCutoff = Instant.now().minus(1, ChronoUnit.DAYS);
        assertTrue(Duration.between(cutoffCaptor.getValue(), expectedCutoff).abs().getSeconds() < 5);
    }
}
