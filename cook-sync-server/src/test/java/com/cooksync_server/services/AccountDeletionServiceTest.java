package com.cooksync_server.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.cooksync_server.entities.Recipe;
import com.cooksync_server.entities.RecipeImage;
import com.cooksync_server.entities.User;
import com.cooksync_server.repositories.FavoriteRecipeRepository;
import com.cooksync_server.repositories.PasswordResetTokenRepository;
import com.cooksync_server.repositories.PersonalInstructionNoteRepository;
import com.cooksync_server.repositories.RecipeRepository;
import com.cooksync_server.repositories.ReviewReportRepository;
import com.cooksync_server.repositories.ReviewRepository;
import com.cooksync_server.repositories.UserRepository;

/**
 * Unit test suite verifying account deletion request, restore, and permanent account purge in AccountDeletionServiceImp.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 10/08/2026
 */
@ExtendWith(MockitoExtension.class)
class AccountDeletionServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RecipeRepository recipeRepository;
    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private ReviewReportRepository reviewReportRepository;
    @Mock
    private PersonalInstructionNoteRepository personalInstructionNoteRepository;
    @Mock
    private FavoriteRecipeRepository favoriteRecipeRepository;
    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock
    private RefreshTokenServiceImp refreshTokenService;
    @Mock
    private CloudinaryService cloudinaryService;

    private AccountDeletionServiceImp accountDeletionService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        accountDeletionService = new AccountDeletionServiceImp(
                userRepository,
                recipeRepository,
                reviewRepository,
                reviewReportRepository,
                personalInstructionNoteRepository,
                favoriteRecipeRepository,
                passwordResetTokenRepository,
                refreshTokenService,
                cloudinaryService
        );

        sampleUser = User.builder()
                .id("user-999")
                .firstName("Test")
                .lastName("User")
                .email("test@example.com")
                .enabled(true)
                .status(User.AccountStatus.ACTIVE)
                .avatarUrl("https://res.cloudinary.com/demo/image/upload/v12345/CookSyncApp/user_avatar.jpg")
                .build();
    }

    @Test
    void requestDeletion_ShouldDeactivateAccountAndHideReviews() {
        accountDeletionService.requestDeletion(sampleUser);

        assertFalse(sampleUser.isEnabled());
        assertEquals(User.AccountStatus.DEACTIVATED, sampleUser.getStatus());
        verify(userRepository).save(sampleUser);
        verify(reviewRepository).setHiddenByUserId(true, "user-999");
        verify(refreshTokenService).deleteByUserId("user-999");
    }

    @Test
    void restoreFromPendingDeletion_ShouldReenableAccountAndUnhideReviews() {
        sampleUser.setEnabled(false);
        sampleUser.setStatus(User.AccountStatus.DEACTIVATED);

        accountDeletionService.restoreFromPendingDeletion(sampleUser);

        assertTrue(sampleUser.isEnabled());
        assertEquals(User.AccountStatus.ACTIVE, sampleUser.getStatus());
        assertNull(sampleUser.getDeletionRequestedAt());
        verify(userRepository).save(sampleUser);
        verify(reviewRepository).setHiddenByUserId(false, "user-999");
    }

    @Test
    void purgeExpiredAccounts_ShouldPurgeUserAndCleanCloudinaryImages() {
        when(userRepository.findByStatusAndDeletionRequestedAtBefore(eq(User.AccountStatus.DEACTIVATED), any()))
                .thenReturn(List.of(sampleUser));

        Recipe sampleRecipe = Recipe.builder()
                .id("recipe-1")
                .title("Sample Recipe")
                .createdBy(sampleUser)
                .images(Set.of(RecipeImage.builder()
                        .imageUrl("https://res.cloudinary.com/demo/image/upload/v12345/CookSyncApp/recipe_1.jpg")
                        .build()))
                .build();

        when(recipeRepository.findByCreatedById(eq("user-999"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleRecipe)));
        when(reviewRepository.findIdsByUserIdOrRecipeIdIn(eq("user-999"), anyList()))
                .thenReturn(List.of("review-1", "review-2"));

        accountDeletionService.purgeExpiredAccounts();

        verify(reviewReportRepository).deleteByReviewIdIn(List.of("review-1", "review-2"));
        verify(reviewReportRepository).deleteByReporterId("user-999");
        verify(personalInstructionNoteRepository).deleteByUserIdOrRecipeIdIn(eq("user-999"), anyList());
        verify(favoriteRecipeRepository).deleteByUserIdOrRecipeIdIn(eq("user-999"), anyList());
        verify(reviewRepository).deleteByUserId("user-999");
        verify(cloudinaryService).deleteImages(List.of(
                "https://res.cloudinary.com/demo/image/upload/v12345/CookSyncApp/user_avatar.jpg",
                "https://res.cloudinary.com/demo/image/upload/v12345/CookSyncApp/recipe_1.jpg"
        ));
        verify(recipeRepository).deleteAll(List.of(sampleRecipe));
        verify(userRepository).delete(sampleUser);
    }
}
