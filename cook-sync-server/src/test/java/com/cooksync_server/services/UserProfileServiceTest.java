package com.cooksync_server.services;

import com.cooksync_server.entities.EmailChangeToken;
import com.cooksync_server.entities.User;
import com.cooksync_server.exceptions.ResourceNotFoundException;
import com.cooksync_server.exceptions.auth.InvalidCredentialsException;
import com.cooksync_server.exceptions.auth.InvalidOtpException;
import com.cooksync_server.exceptions.auth.OtpExpiredException;
import com.cooksync_server.exceptions.auth.TooManyOtpAttemptsException;
import com.cooksync_server.exceptions.auth.UserAlreadyExistsException;
import com.cooksync_server.repositories.EmailChangeTokenRepository;
import com.cooksync_server.repositories.UserRepository;
import com.dtos.request.auth.DeleteAccountRequestDTO;
import com.dtos.request.auth.EmailUpdateRequestDTO;
import com.dtos.request.auth.PrivacySettingsUpdateRequestDTO;
import com.dtos.request.auth.ProfileUpdateRequestDTO;
import com.dtos.request.auth.VerifyEmailChangeOtpRequestDTO;
import com.dtos.response.auth.AuthResponse;
import com.dtos.response.user.PublicUserProfileResponse;
import com.dtos.response.user.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test suite verifying public-profile lookup and the self-service email-change OTP flow in
 * {@link UserProfileServiceImp}.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 23/08/2026
 */
@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private CloudinaryService cloudinaryService;
    @Mock
    private AccountDeletionService accountDeletionService;
    @Mock
    private EmailChangeTokenRepository emailChangeTokenRepository;
    @Mock
    private EmailServiceImp emailService;
    @Mock
    private SessionIssuer sessionIssuer;
    @Mock
    private CredentialVerifier credentialVerifier;

    @InjectMocks
    private UserProfileServiceImp userProfileService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id("user-2")
                .firstName("Jane")
                .lastName("Smith")
                .email("jane@cooksync.com")
                .isAdmin(true)
                .enabled(true)
                .status(User.AccountStatus.ACTIVE)
                .city("Tel Aviv")
                .bio("Home cook.")
                .showRecipesPublicly(true)
                .showFavoritesPublicly(false)
                .build();
    }

    @Test
    void getUserProfileById_ShouldReturnPublicProfile_ExcludingSensitiveFields() {
        when(userRepository.findById("user-2")).thenReturn(Optional.of(sampleUser));

        PublicUserProfileResponse response = userProfileService.getUserProfileById("user-2");

        assertNotNull(response);
        assertEquals("user-2", response.id());
        assertEquals("Jane", response.firstName());
        assertEquals("Tel Aviv", response.city());
        assertEquals(true, response.showRecipesPublicly());
        assertFalse(response.showFavoritesPublicly());
    }

    @Test
    void getUserProfileById_ShouldThrowResourceNotFoundException_WhenUserDoesNotExist() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userProfileService.getUserProfileById("missing"));
    }

    @Test
    void getCurrentUserProfile_ShouldReturnFullProfile_WhenUserExists() {
        when(userRepository.findByEmail("jane@cooksync.com")).thenReturn(Optional.of(sampleUser));

        UserResponse response = userProfileService.getCurrentUserProfile("jane@cooksync.com");

        assertNotNull(response);
        assertEquals("user-2", response.id());
        assertEquals("jane@cooksync.com", response.email());
        assertEquals("Tel Aviv", response.city());
        assertEquals("Home cook.", response.bio());
        assertTrue(response.showRecipesPublicly());
        assertFalse(response.showFavoritesPublicly());
    }

    @Test
    void getCurrentUserProfile_ShouldThrowResourceNotFoundException_WhenUserDoesNotExist() {
        when(userRepository.findByEmail("missing@cooksync.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userProfileService.getCurrentUserProfile("missing@cooksync.com"));
    }

    @Test
    void updateAvatar_ShouldDeleteOldCloudinaryAsset_WhenOldAvatarExistsAndDiffersFromNewUrl() {
        sampleUser.setAvatarUrl("https://res.cloudinary.com/old.jpg");
        when(userRepository.findByEmail("jane@cooksync.com")).thenReturn(Optional.of(sampleUser));

        userProfileService.updateAvatar("jane@cooksync.com", "https://res.cloudinary.com/new.jpg");

        verify(cloudinaryService, times(1)).deleteImage("https://res.cloudinary.com/old.jpg");
        assertEquals("https://res.cloudinary.com/new.jpg", sampleUser.getAvatarUrl());
        verify(userRepository, times(1)).save(sampleUser);
    }

    @Test
    void updateAvatar_ShouldSkipDeletion_WhenOldAvatarIsNull() {
        sampleUser.setAvatarUrl(null);
        when(userRepository.findByEmail("jane@cooksync.com")).thenReturn(Optional.of(sampleUser));

        userProfileService.updateAvatar("jane@cooksync.com", "https://res.cloudinary.com/new.jpg");

        verify(cloudinaryService, never()).deleteImage(anyString());
        assertEquals("https://res.cloudinary.com/new.jpg", sampleUser.getAvatarUrl());
        verify(userRepository, times(1)).save(sampleUser);
    }

    @Test
    void updateAvatar_ShouldSkipDeletion_WhenOldAvatarIsBlank() {
        sampleUser.setAvatarUrl("   ");
        when(userRepository.findByEmail("jane@cooksync.com")).thenReturn(Optional.of(sampleUser));

        userProfileService.updateAvatar("jane@cooksync.com", "https://res.cloudinary.com/new.jpg");

        verify(cloudinaryService, never()).deleteImage(anyString());
        assertEquals("https://res.cloudinary.com/new.jpg", sampleUser.getAvatarUrl());
    }

    @Test
    void updateAvatar_ShouldSkipDeletion_WhenNewUrlEqualsOldUrl() {
        sampleUser.setAvatarUrl("https://res.cloudinary.com/same.jpg");
        when(userRepository.findByEmail("jane@cooksync.com")).thenReturn(Optional.of(sampleUser));

        userProfileService.updateAvatar("jane@cooksync.com", "https://res.cloudinary.com/same.jpg");

        verify(cloudinaryService, never()).deleteImage(anyString());
        assertEquals("https://res.cloudinary.com/same.jpg", sampleUser.getAvatarUrl());
        verify(userRepository, times(1)).save(sampleUser);
    }

    @Test
    void updateAvatar_ShouldThrowResourceNotFoundException_WhenUserDoesNotExist() {
        when(userRepository.findByEmail("missing@cooksync.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userProfileService.updateAvatar("missing@cooksync.com", "https://res.cloudinary.com/new.jpg"));
        verify(cloudinaryService, never()).deleteImage(anyString());
    }

    @Test
    void updateProfile_ShouldUpdateNameCityAndBio_WhenUserExists() {
        ProfileUpdateRequestDTO request = new ProfileUpdateRequestDTO("John", "Doe", "Haifa", "Updated bio.");
        when(userRepository.findByEmail("jane@cooksync.com")).thenReturn(Optional.of(sampleUser));

        userProfileService.updateProfile("jane@cooksync.com", request);

        assertEquals("John", sampleUser.getFirstName());
        assertEquals("Doe", sampleUser.getLastName());
        assertEquals("Haifa", sampleUser.getCity());
        assertEquals("Updated bio.", sampleUser.getBio());
        verify(userRepository, times(1)).save(sampleUser);
    }

    @Test
    void updateProfile_ShouldThrowResourceNotFoundException_WhenUserDoesNotExist() {
        ProfileUpdateRequestDTO request = new ProfileUpdateRequestDTO("John", "Doe", "Haifa", "Updated bio.");
        when(userRepository.findByEmail("missing@cooksync.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userProfileService.updateProfile("missing@cooksync.com", request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updatePrivacySettings_ShouldUpdateVisibilityPreferences_WhenUserExists() {
        PrivacySettingsUpdateRequestDTO request = new PrivacySettingsUpdateRequestDTO(false, true);
        when(userRepository.findByEmail("jane@cooksync.com")).thenReturn(Optional.of(sampleUser));

        userProfileService.updatePrivacySettings("jane@cooksync.com", request);

        assertFalse(sampleUser.isShowRecipesPublicly());
        assertTrue(sampleUser.isShowFavoritesPublicly());
        verify(userRepository, times(1)).save(sampleUser);
    }

    @Test
    void updatePrivacySettings_ShouldThrowResourceNotFoundException_WhenUserDoesNotExist() {
        PrivacySettingsUpdateRequestDTO request = new PrivacySettingsUpdateRequestDTO(false, true);
        when(userRepository.findByEmail("missing@cooksync.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userProfileService.updatePrivacySettings("missing@cooksync.com", request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void deactivateAccount_ShouldDisableAccountAndRevokeRefreshTokens_WhenUserExists() {
        when(userRepository.findByEmail("jane@cooksync.com")).thenReturn(Optional.of(sampleUser));

        userProfileService.deactivateAccount("jane@cooksync.com");

        assertFalse(sampleUser.isEnabled());
        assertEquals(User.AccountStatus.DEACTIVATED, sampleUser.getStatus());
        verify(userRepository, times(1)).save(sampleUser);
        verify(refreshTokenService, times(1)).deleteByUserId("user-2");
    }

    @Test
    void deactivateAccount_ShouldThrowResourceNotFoundException_WhenUserDoesNotExist() {
        when(userRepository.findByEmail("missing@cooksync.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userProfileService.deactivateAccount("missing@cooksync.com"));
        verify(refreshTokenService, never()).deleteByUserId(anyString());
    }

    @Test
    void requestAccountDeletion_ShouldDelegateToAccountDeletionService_WhenPasswordCorrect() {
        DeleteAccountRequestDTO request = new DeleteAccountRequestDTO("correct-password");
        when(credentialVerifier.verifyCurrentPassword("jane@cooksync.com", "correct-password")).thenReturn(sampleUser);

        userProfileService.requestAccountDeletion("jane@cooksync.com", request);

        verify(accountDeletionService, times(1)).requestDeletion(sampleUser);
    }

    @Test
    void requestAccountDeletion_ShouldThrowInvalidCredentialsException_WhenPasswordIncorrect() {
        DeleteAccountRequestDTO request = new DeleteAccountRequestDTO("wrong-password");
        when(credentialVerifier.verifyCurrentPassword("jane@cooksync.com", "wrong-password"))
                .thenThrow(new InvalidCredentialsException("Current password is incorrect"));

        assertThrows(InvalidCredentialsException.class,
                () -> userProfileService.requestAccountDeletion("jane@cooksync.com", request));
        verify(accountDeletionService, never()).requestDeletion(any(User.class));
    }

    @Test
    void requestEmailChange_ShouldSaveHashedCodeAndSendEmailToNewAddress_WhenPasswordCorrectAndEmailFree() {
        EmailUpdateRequestDTO request = new EmailUpdateRequestDTO("new@example.com", "correct-password");
        when(credentialVerifier.verifyCurrentPassword("jane@cooksync.com", "correct-password")).thenReturn(sampleUser);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        lenient().when(passwordEncoder.encode(anyString())).thenReturn("hashed-code");

        userProfileService.requestEmailChange("jane@cooksync.com", request);

        verify(emailChangeTokenRepository, times(1)).deleteByUserId("user-2");
        verify(emailChangeTokenRepository, times(1)).save(any(EmailChangeToken.class));
        verify(emailService, times(1)).sendOtpEmail(eq("new@example.com"), anyString(), anyInt());
    }

    @Test
    void requestEmailChange_ShouldThrowInvalidCredentialsException_WhenPasswordIncorrect() {
        EmailUpdateRequestDTO request = new EmailUpdateRequestDTO("new@example.com", "wrong-password");
        when(credentialVerifier.verifyCurrentPassword("jane@cooksync.com", "wrong-password"))
                .thenThrow(new InvalidCredentialsException("Current password is incorrect"));

        assertThrows(InvalidCredentialsException.class,
                () -> userProfileService.requestEmailChange("jane@cooksync.com", request));
        verify(emailChangeTokenRepository, never()).save(any(EmailChangeToken.class));
    }

    @Test
    void requestEmailChange_ShouldThrowUserAlreadyExistsException_WhenNewEmailAlreadyRegistered() {
        EmailUpdateRequestDTO request = new EmailUpdateRequestDTO("taken@example.com", "correct-password");
        when(credentialVerifier.verifyCurrentPassword("jane@cooksync.com", "correct-password")).thenReturn(sampleUser);
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class,
                () -> userProfileService.requestEmailChange("jane@cooksync.com", request));
        verify(emailChangeTokenRepository, never()).save(any(EmailChangeToken.class));
    }

    @Test
    void confirmEmailChange_ShouldApplyNewEmailAndDeleteToken_WhenCodeCorrect() {
        VerifyEmailChangeOtpRequestDTO request = new VerifyEmailChangeOtpRequestDTO("123456");
        EmailChangeToken changeToken = EmailChangeToken.builder()
                .id("token-id")
                .user(sampleUser)
                .newEmail("new@example.com")
                .codeHash("hashed-code")
                .expiryDate(Instant.now().plusSeconds(300))
                .attemptCount(0)
                .build();

        when(userRepository.findByEmail("jane@cooksync.com")).thenReturn(Optional.of(sampleUser));
        when(emailChangeTokenRepository.findByUserId("user-2")).thenReturn(Optional.of(changeToken));
        when(passwordEncoder.matches("123456", "hashed-code")).thenReturn(true);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(sessionIssuer.issue(sampleUser))
                .thenReturn(new AuthResponse("jwt-token", "refresh-token", "user-2", "Jane", "Smith", true, null));

        AuthResponse response = userProfileService.confirmEmailChange("jane@cooksync.com", request);

        assertEquals("new@example.com", sampleUser.getEmail());
        assertEquals("jwt-token", response.token());
        verify(userRepository, times(1)).save(sampleUser);
        verify(emailChangeTokenRepository, times(1)).delete(changeToken);
    }

    @Test
    void confirmEmailChange_ShouldThrowInvalidOtpException_WhenNoActiveTokenFound() {
        VerifyEmailChangeOtpRequestDTO request = new VerifyEmailChangeOtpRequestDTO("123456");
        when(userRepository.findByEmail("jane@cooksync.com")).thenReturn(Optional.of(sampleUser));
        when(emailChangeTokenRepository.findByUserId("user-2")).thenReturn(Optional.empty());

        assertThrows(InvalidOtpException.class,
                () -> userProfileService.confirmEmailChange("jane@cooksync.com", request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void confirmEmailChange_ShouldThrowOtpExpiredException_WhenTokenExpired() {
        VerifyEmailChangeOtpRequestDTO request = new VerifyEmailChangeOtpRequestDTO("123456");
        EmailChangeToken changeToken = EmailChangeToken.builder()
                .user(sampleUser)
                .newEmail("new@example.com")
                .codeHash("hashed-code")
                .expiryDate(Instant.now().minusSeconds(60))
                .attemptCount(0)
                .build();

        when(userRepository.findByEmail("jane@cooksync.com")).thenReturn(Optional.of(sampleUser));
        when(emailChangeTokenRepository.findByUserId("user-2")).thenReturn(Optional.of(changeToken));

        assertThrows(OtpExpiredException.class,
                () -> userProfileService.confirmEmailChange("jane@cooksync.com", request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void confirmEmailChange_ShouldThrowInvalidOtpException_AndIncrementAttempts_WhenCodeIncorrect() {
        VerifyEmailChangeOtpRequestDTO request = new VerifyEmailChangeOtpRequestDTO("000000");
        EmailChangeToken changeToken = EmailChangeToken.builder()
                .user(sampleUser)
                .newEmail("new@example.com")
                .codeHash("hashed-code")
                .expiryDate(Instant.now().plusSeconds(300))
                .attemptCount(0)
                .build();

        when(userRepository.findByEmail("jane@cooksync.com")).thenReturn(Optional.of(sampleUser));
        when(emailChangeTokenRepository.findByUserId("user-2")).thenReturn(Optional.of(changeToken));
        when(passwordEncoder.matches("000000", "hashed-code")).thenReturn(false);

        assertThrows(InvalidOtpException.class,
                () -> userProfileService.confirmEmailChange("jane@cooksync.com", request));
        assertEquals(1, changeToken.getAttemptCount());
        verify(emailChangeTokenRepository, times(1)).save(changeToken);
        verify(emailChangeTokenRepository, never()).delete(any(EmailChangeToken.class));
    }

    @Test
    void confirmEmailChange_ShouldThrowTooManyOtpAttemptsException_AndDeleteToken_WhenAttemptsExceeded() {
        VerifyEmailChangeOtpRequestDTO request = new VerifyEmailChangeOtpRequestDTO("000000");
        EmailChangeToken changeToken = EmailChangeToken.builder()
                .user(sampleUser)
                .newEmail("new@example.com")
                .codeHash("hashed-code")
                .expiryDate(Instant.now().plusSeconds(300))
                .attemptCount(4)
                .build();

        when(userRepository.findByEmail("jane@cooksync.com")).thenReturn(Optional.of(sampleUser));
        when(emailChangeTokenRepository.findByUserId("user-2")).thenReturn(Optional.of(changeToken));
        when(passwordEncoder.matches("000000", "hashed-code")).thenReturn(false);

        assertThrows(TooManyOtpAttemptsException.class,
                () -> userProfileService.confirmEmailChange("jane@cooksync.com", request));
        verify(emailChangeTokenRepository, times(1)).delete(changeToken);
        verify(emailChangeTokenRepository, never()).save(any(EmailChangeToken.class));
    }

    @Test
    void confirmEmailChange_ShouldThrowUserAlreadyExistsException_WhenPendingEmailTakenSinceRequest() {
        VerifyEmailChangeOtpRequestDTO request = new VerifyEmailChangeOtpRequestDTO("123456");
        EmailChangeToken changeToken = EmailChangeToken.builder()
                .user(sampleUser)
                .newEmail("new@example.com")
                .codeHash("hashed-code")
                .expiryDate(Instant.now().plusSeconds(300))
                .attemptCount(0)
                .build();

        when(userRepository.findByEmail("jane@cooksync.com")).thenReturn(Optional.of(sampleUser));
        when(emailChangeTokenRepository.findByUserId("user-2")).thenReturn(Optional.of(changeToken));
        when(passwordEncoder.matches("123456", "hashed-code")).thenReturn(true);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class,
                () -> userProfileService.confirmEmailChange("jane@cooksync.com", request));
        verify(userRepository, never()).save(any(User.class));
        verify(emailChangeTokenRepository, never()).delete(any(EmailChangeToken.class));
    }
}
