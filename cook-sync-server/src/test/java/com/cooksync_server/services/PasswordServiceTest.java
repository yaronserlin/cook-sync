package com.cooksync_server.services;

import com.dtos.request.auth.ChangePasswordRequestDTO;
import com.dtos.request.auth.ForgotPasswordRequestDTO;
import com.dtos.request.auth.ResetPasswordRequestDTO;
import com.cooksync_server.entities.PasswordResetToken;
import com.cooksync_server.entities.User;
import com.cooksync_server.exceptions.auth.InvalidCredentialsException;
import com.cooksync_server.exceptions.auth.InvalidOtpException;
import com.cooksync_server.exceptions.auth.OtpExpiredException;
import com.cooksync_server.exceptions.auth.TooManyOtpAttemptsException;
import com.cooksync_server.repositories.PasswordResetTokenRepository;
import com.cooksync_server.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test suite verifying the forgot/reset-password OTP flow in PasswordServiceImp. Split out of
 * the former combined AuthServiceTest when {@code AuthServiceImp} was divided by responsibility
 * (registration/login stayed in {@link AuthServiceTest}, profile settings moved to
 * {@code UserProfileServiceTest}).
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 13/08/2026
 */
@ExtendWith(MockitoExtension.class)
class PasswordServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock
    private RefreshTokenServiceImp refreshTokenService;
    @Mock
    private EmailServiceImp emailService;
    @Mock
    private CredentialVerifier credentialVerifier;

    @InjectMocks
    private PasswordServiceImp passwordService;

    @BeforeEach
    void setUp() {
        lenient().when(passwordEncoder.encode(anyString())).thenReturn("hashed-password");
    }

    @Test
    void changePassword_ShouldUpdatePasswordAndRevokeSessions_WhenCurrentPasswordIsCorrect() {
        ChangePasswordRequestDTO request = new ChangePasswordRequestDTO("OldPassword123!", "NewPassword123!");
        User existingUser = User.builder().id("user-123").email("john@example.com").passwordHash("old-hash").build();

        when(credentialVerifier.verifyCurrentPassword("john@example.com", "OldPassword123!")).thenReturn(existingUser);

        passwordService.changePassword("john@example.com", request);

        assertEquals("hashed-password", existingUser.getPasswordHash());
        verify(userRepository, times(1)).save(existingUser);
        verify(refreshTokenService, times(1)).deleteByUserId("user-123");
    }

    @Test
    void changePassword_ShouldThrowInvalidCredentialsException_WhenCurrentPasswordIsIncorrect() {
        ChangePasswordRequestDTO request = new ChangePasswordRequestDTO("WrongPassword", "NewPassword123!");

        when(credentialVerifier.verifyCurrentPassword("john@example.com", "WrongPassword"))
                .thenThrow(new InvalidCredentialsException("Current password is incorrect"));

        assertThrows(InvalidCredentialsException.class, () -> passwordService.changePassword("john@example.com", request));
        verify(userRepository, never()).save(any(User.class));
        verify(refreshTokenService, never()).deleteByUserId(anyString());
    }

    @Test
    void forgotPassword_ShouldSaveHashedCodeAndSendEmail_WhenEmailIsRegistered() {
        ForgotPasswordRequestDTO request = new ForgotPasswordRequestDTO("john@example.com");
        User existingUser = User.builder().id("user-123").email("john@example.com").build();

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(existingUser));

        passwordService.forgotPassword(request);

        verify(passwordResetTokenRepository, times(1)).deleteByUserId("user-123");
        verify(passwordResetTokenRepository, times(1)).save(any(PasswordResetToken.class));
        verify(emailService, times(1)).sendPasswordResetEmail(eq("john@example.com"), anyString(), anyInt());
    }

    @Test
    void forgotPassword_ShouldDoNothing_WhenEmailIsNotRegistered() {
        ForgotPasswordRequestDTO request = new ForgotPasswordRequestDTO("unknown@example.com");
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        passwordService.forgotPassword(request);

        verify(passwordResetTokenRepository, never()).save(any(PasswordResetToken.class));
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString(), anyInt());
    }

    @Test
    void resetPassword_ShouldUpdatePasswordAndDeleteToken_WhenCodeIsCorrect() {
        ResetPasswordRequestDTO request = new ResetPasswordRequestDTO("john@example.com", "123456", "NewPassword123!");
        User existingUser = User.builder().id("user-123").email("john@example.com").passwordHash("old-hash").build();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .id("reset-id")
                .user(existingUser)
                .codeHash("hashed-code")
                .expiryDate(Instant.now().plusSeconds(300))
                .attemptCount(0)
                .build();

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(existingUser));
        when(passwordResetTokenRepository.findByUserId("user-123")).thenReturn(Optional.of(resetToken));
        when(passwordEncoder.matches("123456", "hashed-code")).thenReturn(true);

        passwordService.resetPassword(request);

        assertEquals("hashed-password", existingUser.getPasswordHash());
        verify(userRepository, times(1)).save(existingUser);
        verify(passwordResetTokenRepository, times(1)).delete(resetToken);
        verify(refreshTokenService, times(1)).deleteByUserId("user-123");
    }

    @Test
    void resetPassword_ShouldThrowInvalidOtpException_WhenEmailIsNotRegistered() {
        ResetPasswordRequestDTO request = new ResetPasswordRequestDTO("unknown@example.com", "123456", "NewPassword123!");
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(InvalidOtpException.class, () -> passwordService.resetPassword(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void resetPassword_ShouldThrowInvalidOtpException_WhenNoActiveResetTokenFound() {
        ResetPasswordRequestDTO request = new ResetPasswordRequestDTO("john@example.com", "123456", "NewPassword123!");
        User existingUser = User.builder().id("user-123").email("john@example.com").build();

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(existingUser));
        when(passwordResetTokenRepository.findByUserId("user-123")).thenReturn(Optional.empty());

        assertThrows(InvalidOtpException.class, () -> passwordService.resetPassword(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void resetPassword_ShouldThrowOtpExpiredException_WhenCodeExpired() {
        ResetPasswordRequestDTO request = new ResetPasswordRequestDTO("john@example.com", "123456", "NewPassword123!");
        User existingUser = User.builder().id("user-123").email("john@example.com").build();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(existingUser)
                .codeHash("hashed-code")
                .expiryDate(Instant.now().minusSeconds(60))
                .attemptCount(0)
                .build();

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(existingUser));
        when(passwordResetTokenRepository.findByUserId("user-123")).thenReturn(Optional.of(resetToken));

        assertThrows(OtpExpiredException.class, () -> passwordService.resetPassword(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void resetPassword_ShouldThrowInvalidOtpException_AndIncrementAttempts_WhenCodeIncorrect() {
        ResetPasswordRequestDTO request = new ResetPasswordRequestDTO("john@example.com", "000000", "NewPassword123!");
        User existingUser = User.builder().id("user-123").email("john@example.com").build();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(existingUser)
                .codeHash("hashed-code")
                .expiryDate(Instant.now().plusSeconds(300))
                .attemptCount(0)
                .build();

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(existingUser));
        when(passwordResetTokenRepository.findByUserId("user-123")).thenReturn(Optional.of(resetToken));
        when(passwordEncoder.matches("000000", "hashed-code")).thenReturn(false);

        assertThrows(InvalidOtpException.class, () -> passwordService.resetPassword(request));
        assertEquals(1, resetToken.getAttemptCount());
        verify(passwordResetTokenRepository, times(1)).save(resetToken);
        verify(passwordResetTokenRepository, never()).delete(any(PasswordResetToken.class));
    }

    @Test
    void resetPassword_ShouldThrowTooManyOtpAttemptsException_AndDeleteToken_WhenAttemptsExceeded() {
        ResetPasswordRequestDTO request = new ResetPasswordRequestDTO("john@example.com", "000000", "NewPassword123!");
        User existingUser = User.builder().id("user-123").email("john@example.com").build();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(existingUser)
                .codeHash("hashed-code")
                .expiryDate(Instant.now().plusSeconds(300))
                .attemptCount(4)
                .build();

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(existingUser));
        when(passwordResetTokenRepository.findByUserId("user-123")).thenReturn(Optional.of(resetToken));
        when(passwordEncoder.matches("000000", "hashed-code")).thenReturn(false);

        assertThrows(TooManyOtpAttemptsException.class, () -> passwordService.resetPassword(request));
        verify(passwordResetTokenRepository, times(1)).delete(resetToken);
        verify(passwordResetTokenRepository, never()).save(any(PasswordResetToken.class));
    }
}
