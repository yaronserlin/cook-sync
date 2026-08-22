package com.cooksync_server.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

import com.cooksync_server.entities.User;
import com.cooksync_server.exceptions.ResourceNotFoundException;
import com.cooksync_server.repositories.RecipeRepository;
import com.cooksync_server.repositories.ReviewReportRepository;
import com.cooksync_server.repositories.ReviewRepository;
import com.cooksync_server.repositories.TagRepository;
import com.cooksync_server.repositories.UserRepository;
import com.dtos.request.tags.TagMergeRequestDTO;
import com.dtos.response.PagedResponse;
import com.dtos.response.admin.AdminStatsResponse;
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
    void disableUser_ShouldThrowResourceNotFoundException_WhenUserDoesNotExist() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> adminService.disableUser("missing"));
    }

    @Test
    void disableUser_ShouldSuspendAccountAndHideReviews() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(sampleUser));

        adminService.disableUser("user-1");

        assertFalse(sampleUser.isEnabled());
        assertEquals(User.AccountStatus.SUSPENDED, sampleUser.getStatus());
        verify(userRepository).save(sampleUser);
        verify(reviewRepository).setHiddenByUserId(true, "user-1");
    }

    @Test
    void enableUser_ShouldDelegateToAccountDeletionServiceRestore() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(sampleUser));

        adminService.enableUser("user-1");

        verify(accountDeletionService).restoreFromPendingDeletion(sampleUser);
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
}
