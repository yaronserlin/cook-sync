package com.cooksync_server.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;

import com.cooksync_server.entities.Review;
import com.cooksync_server.entities.Tag;
import com.cooksync_server.entities.User;
import com.cooksync_server.exceptions.ResourceNotFoundException;
import com.cooksync_server.exceptions.auth.UnauthorizedActionException;
import com.cooksync_server.repositories.RecipeRepository;
import com.cooksync_server.repositories.ReviewReportRepository;
import com.cooksync_server.repositories.ReviewRepository;
import com.cooksync_server.repositories.TagRepository;
import com.cooksync_server.repositories.UserRepository;
import com.dtos.request.tags.TagMergeRequestDTO;
import com.dtos.response.PagedResponse;
import com.dtos.response.admin.AdminStatsResponse;
import com.dtos.response.admin.DuplicateTagGroupResponse;
import com.dtos.response.admin.ReportedReviewResponse;
import com.dtos.response.user.UserResponse;

/**
 * Unit test suite verifying admin dashboard stats, user moderation, and tag deduplication in AdminServiceImp.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 12/08/2026
 */
@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private ReviewReportRepository reviewReportRepository;
    @Mock
    private RecipeRepository recipeRepository;
    @Mock
    private TagRepository tagRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private AccountDeletionService accountDeletionService;
    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AdminServiceImp adminService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id("user-1")
                .firstName("Gordon")
                .lastName("Ramsay")
                .email("gordon@cooksync.com")
                .enabled(true)
                .status(User.AccountStatus.ACTIVE)
                .build();
    }

    @Test
    void getStats_ShouldReturnAggregateCounts() {
        when(reviewRepository.countByReportedTrueAndHiddenFalse()).thenReturn(2L);
        when(recipeRepository.count()).thenReturn(30L);
        when(reviewRepository.count()).thenReturn(50L);
        when(tagRepository.count()).thenReturn(12L);
        when(userRepository.count()).thenReturn(8L);

        AdminStatsResponse stats = adminService.getStats();

        assertEquals(2L, stats.reportedReviews());
        assertEquals(30L, stats.recipes());
        assertEquals(50L, stats.reviews());
        assertEquals(12L, stats.tags());
        assertEquals(8L, stats.users());
    }

    @Test
    void getAllUsers_ShouldReturnPagedResponse() {
        Page<User> userPage = new PageImpl<>(java.util.List.of(sampleUser), PageRequest.of(0, 10), 1);
        when(userRepository.search(any(), any(), any(Pageable.class))).thenReturn(userPage);

        PagedResponse<UserResponse> response = adminService.getAllUsers(0, 10, null, null, "createdAt", "desc");

        assertEquals(1, response.content().size());
        assertEquals("Gordon", response.content().get(0).firstName());
    }

    @Test
    void getReportedReviews_ShouldReturnPagedReportedReviews() {
        Review review = Review.builder()
                .id("review-1")
                .comment("Undercooked and salty.")
                .rating(BigDecimal.valueOf(1))
                .reported(true)
                .reportReason(Review.ReportReason.ABUSE)
                .reportedAt(LocalDateTime.now())
                .build();
        Page<Review> reviewPage = new PageImpl<>(List.of(review), PageRequest.of(0, 10), 1);
        when(reviewRepository.findByReportedTrueAndHiddenFalse(any(Pageable.class))).thenReturn(reviewPage);
        when(reviewReportRepository.findTopByReviewIdOrderByCreatedAtDesc("review-1")).thenReturn(Optional.empty());

        PagedResponse<ReportedReviewResponse> response = adminService.getReportedReviews(0, 10);

        assertEquals(1, response.content().size());
        assertEquals("review-1", response.content().get(0).id());
        assertEquals("Undercooked and salty.", response.content().get(0).comment());
    }

    @Test
    void dismissReport_ShouldClearReportFlagsAndSave_WhenReviewExists() {
        Review review = Review.builder()
                .id("review-1")
                .reported(true)
                .reportReason(Review.ReportReason.SPAM)
                .reportedAt(LocalDateTime.now())
                .build();
        when(reviewRepository.findById("review-1")).thenReturn(Optional.of(review));

        adminService.dismissReport("review-1");

        assertFalse(review.isReported());
        assertNull(review.getReportReason());
        assertNull(review.getReportedAt());
        verify(reviewRepository).save(review);
    }

    @Test
    void dismissReport_ShouldThrowResourceNotFoundException_WhenReviewDoesNotExist() {
        when(reviewRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> adminService.dismissReport("missing"));
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void suspendUser_ShouldThrowResourceNotFoundException_WhenUserDoesNotExist() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> adminService.suspendUser("missing", "admin@cooksync.com"));
    }

    @Test
    void suspendUser_ShouldSuspendAccountHideReviewsAndRevokeSession() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(sampleUser));

        adminService.suspendUser("user-1", "admin@cooksync.com");

        assertFalse(sampleUser.isEnabled());
        assertEquals(User.AccountStatus.SUSPENDED, sampleUser.getStatus());
        verify(userRepository).save(sampleUser);
        verify(reviewRepository).setHiddenByUserId(true, "user-1");
        verify(refreshTokenService).deleteByUserId("user-1");
    }

    @Test
    void suspendUser_ShouldThrowUnauthorizedActionException_WhenTargetIsActingAdmin() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(sampleUser));

        assertThrows(UnauthorizedActionException.class,
                () -> adminService.suspendUser("user-1", sampleUser.getEmail()));

        verify(userRepository, never()).save(any());
        verify(refreshTokenService, never()).deleteByUserId(anyString());
    }

    @Test
    void suspendUser_ShouldThrowUnauthorizedActionException_WhenTargetIsAnotherAdmin() {
        sampleUser.setAdmin(true);
        when(userRepository.findById("user-1")).thenReturn(Optional.of(sampleUser));

        assertThrows(UnauthorizedActionException.class,
                () -> adminService.suspendUser("user-1", "other-admin@cooksync.com"));

        verify(userRepository, never()).save(any());
        verify(refreshTokenService, never()).deleteByUserId(anyString());
    }

    @Test
    void enableUser_ShouldDelegateToAccountDeletionServiceRestore() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(sampleUser));

        adminService.enableUser("user-1");

        verify(accountDeletionService).restoreFromPendingDeletion(sampleUser);
    }

    @Test
    void deleteUserPermanently_ShouldThrowResourceNotFoundException_WhenUserDoesNotExist() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> adminService.deleteUserPermanently("missing", "admin@cooksync.com"));
    }

    @Test
    void deleteUserPermanently_ShouldDelegateToAccountDeletionService_WhenUserExists() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(sampleUser));

        adminService.deleteUserPermanently("user-1", "admin@cooksync.com");

        verify(accountDeletionService).purgeAccountImmediately(sampleUser);
    }

    @Test
    void deleteUserPermanently_ShouldThrowUnauthorizedActionException_WhenTargetIsActingAdmin() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(sampleUser));

        assertThrows(UnauthorizedActionException.class,
                () -> adminService.deleteUserPermanently("user-1", sampleUser.getEmail()));

        verify(accountDeletionService, never()).purgeAccountImmediately(any());
    }

    @Test
    void deleteUserPermanently_ShouldThrowUnauthorizedActionException_WhenTargetIsAnotherAdmin() {
        sampleUser.setAdmin(true);
        when(userRepository.findById("user-1")).thenReturn(Optional.of(sampleUser));

        assertThrows(UnauthorizedActionException.class,
                () -> adminService.deleteUserPermanently("user-1", "other-admin@cooksync.com"));

        verify(accountDeletionService, never()).purgeAccountImmediately(any());
    }

    @Test
    void mergeTags_ShouldThrowIllegalArgumentException_WhenSourceEqualsTarget() {
        TagMergeRequestDTO request = new TagMergeRequestDTO("tag-1", "tag-1");

        assertThrows(IllegalArgumentException.class, () -> adminService.mergeTags(request));
        verify(jdbcTemplate, never()).update(anyString(), any(), any());
    }

    @Test
    void mergeTags_ShouldThrowResourceNotFoundException_WhenSourceTagMissing() {
        TagMergeRequestDTO request = new TagMergeRequestDTO("missing-source", "tag-2");
        when(tagRepository.existsById("missing-source")).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> adminService.mergeTags(request));
    }

    @Test
    void mergeTags_ShouldThrowResourceNotFoundException_WhenTargetTagMissing() {
        TagMergeRequestDTO request = new TagMergeRequestDTO("tag-1", "missing-target");
        when(tagRepository.existsById("tag-1")).thenReturn(true);
        when(tagRepository.existsById("missing-target")).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> adminService.mergeTags(request));
        verify(jdbcTemplate, never()).update(anyString(), any(), any());
    }

    @Test
    void mergeTags_ShouldRunReassignmentAndDeletion_WhenBothTagsExist() {
        TagMergeRequestDTO request = new TagMergeRequestDTO("tag-1", "tag-2");
        when(tagRepository.existsById("tag-1")).thenReturn(true);
        when(tagRepository.existsById("tag-2")).thenReturn(true);

        adminService.mergeTags(request);

        verify(jdbcTemplate).update(
                "DELETE rt FROM recipe_tags rt JOIN recipe_tags rt2 ON rt.recipe_id = rt2.recipe_id " +
                        "WHERE rt.tag_id = ? AND rt2.tag_id = ?",
                "tag-1", "tag-2");
        verify(jdbcTemplate).update("UPDATE recipe_tags SET tag_id = ? WHERE tag_id = ?", "tag-2", "tag-1");
        verify(jdbcTemplate).update("DELETE FROM tags WHERE id = ?", "tag-1");
    }

    @Test
    void getDuplicateTagGroups_ShouldFindDuplicates_AcrossEntireDataset() {
        Tag vegan = Tag.builder().id("tag-1").name("Vegan").build();
        Tag vegetarian = Tag.builder().id("tag-2").name("Vegetarian").build();
        Tag veganDuplicate = Tag.builder().id("tag-3").name("vegan").build();
        when(tagRepository.findAll()).thenReturn(List.of(vegan, vegetarian, veganDuplicate));
        when(recipeRepository.countByTagId(anyString())).thenReturn(0L);

        PagedResponse<DuplicateTagGroupResponse> response = adminService.getDuplicateTagGroups(0, 1);

        assertEquals(1, response.content().size());
        DuplicateTagGroupResponse group = response.content().get(0);
        assertEquals("vegan", group.normalizedName());
        assertEquals(2, group.variants().size());
    }

    @Test
    void getDuplicateTagGroups_ShouldGroupAllPunctuationVariants_OfSameTag() {
        Tag hyphenated = Tag.builder().id("tag-1").name("high-protein").build();
        Tag underscored = Tag.builder().id("tag-2").name("high_protein").build();
        Tag slashed = Tag.builder().id("tag-3").name("high/protein").build();
        Tag spaced = Tag.builder().id("tag-4").name("high protein").build();
        when(tagRepository.findAll()).thenReturn(List.of(hyphenated, underscored, slashed, spaced));
        when(recipeRepository.countByTagId(anyString())).thenReturn(0L);

        PagedResponse<DuplicateTagGroupResponse> response = adminService.getDuplicateTagGroups(0, 10);

        assertEquals(1, response.content().size());
        DuplicateTagGroupResponse group = response.content().get(0);
        assertEquals("high protein", group.normalizedName());
        assertEquals(4, group.variants().size());
    }
}
