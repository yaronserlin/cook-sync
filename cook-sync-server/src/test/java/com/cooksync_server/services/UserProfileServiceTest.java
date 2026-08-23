package com.cooksync_server.services;

import com.cooksync_server.entities.User;
import com.cooksync_server.exceptions.ResourceNotFoundException;
import com.cooksync_server.repositories.UserRepository;
import com.dtos.response.user.PublicUserProfileResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * Unit test suite verifying public-profile lookup in {@link UserProfileServiceImp}.
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
    private com.cooksync_server.config.JwtUtil jwtUtil;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private CloudinaryService cloudinaryService;
    @Mock
    private AccountDeletionService accountDeletionService;

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
}
