package com.cooksync_server.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.cooksync_server.entities.RefreshToken;
import com.cooksync_server.entities.User;
import com.cooksync_server.exceptions.ResourceNotFoundException;
import com.cooksync_server.exceptions.auth.UnauthorizedActionException;
import com.cooksync_server.repositories.RefreshTokenRepository;
import com.cooksync_server.repositories.UserRepository;

/**
 * Unit test suite verifying refresh token generation, expiry validation, and cleanup in RefreshTokenServiceImp.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 12/08/2026
 */
@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RefreshTokenServiceImp refreshTokenService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(refreshTokenService, "refreshTokenDurationMs", 604800000L);
        sampleUser = User.builder().id("user-1").email("gordon@cooksync.com").build();
    }

    @Test
    void createRefreshToken_ShouldThrowResourceNotFoundException_WhenUserMissing() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> refreshTokenService.createRefreshToken("missing"));
    }

    @Test
    void createRefreshToken_ShouldRevokeExistingTokenAndCreateNewOne() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(sampleUser));
        when(refreshTokenRepository.save(org.mockito.ArgumentMatchers.any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RefreshToken token = refreshTokenService.createRefreshToken("user-1");

        verify(refreshTokenRepository).deleteByUserId("user-1");
        assertNotNull(token.getToken());
        assertEquals(sampleUser, token.getUser());
    }

    @Test
    void verifyExpiration_ShouldReturnToken_WhenNotExpired() {
        RefreshToken token = RefreshToken.builder().id("token-1").token("abc")
                .expiryDate(Instant.now().plusSeconds(3600)).build();

        RefreshToken result = refreshTokenService.verifyExpiration(token);

        assertEquals(token, result);
    }

    @Test
    void verifyExpiration_ShouldThrowAndDeleteToken_WhenExpired() {
        RefreshToken token = RefreshToken.builder().id("token-1").token("abc")
                .expiryDate(Instant.now().minusSeconds(3600)).build();

        assertThrows(UnauthorizedActionException.class, () -> refreshTokenService.verifyExpiration(token));
        verify(refreshTokenRepository).delete(token);
    }

    @Test
    void deleteByUserId_ShouldDelegateToRepository() {
        refreshTokenService.deleteByUserId("user-1");

        verify(refreshTokenRepository).deleteByUserId("user-1");
    }
}
