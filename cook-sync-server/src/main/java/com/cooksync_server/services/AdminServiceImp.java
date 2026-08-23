package com.cooksync_server.services;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cooksync_server.entities.Review;
import com.cooksync_server.entities.ReviewReport;
import com.cooksync_server.entities.Tag;
import com.cooksync_server.entities.User;
import com.cooksync_server.exceptions.ResourceNotFoundException;
import com.cooksync_server.exceptions.auth.UnauthorizedActionException;
import com.cooksync_server.mappers.AdminMapper;
import com.cooksync_server.mappers.UserMapper;
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
import com.dtos.response.admin.TagVariantResponse;
import com.dtos.response.user.UserResponse;

import lombok.RequiredArgsConstructor;

/**
 * Service class implementing business logic for administrative moderation, user
 * management, and tag deduplication.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
@Service
@RequiredArgsConstructor
public class AdminServiceImp implements AdminService {

    private final ReviewRepository reviewRepository;
    private final ReviewReportRepository reviewReportRepository;
    private final RecipeRepository recipeRepository;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;
    private final AccountDeletionService accountDeletionService;

    /**
     * Calculates system-wide aggregate stats for admin dashboard monitoring.
     *
     * Complexity: Time: O(1) Space: O(1)
     *
     * @return AdminStatsResponse containing counts of reported reviews,
     * recipes, reviews, tags, and users
     */
    @Override
    public AdminStatsResponse getStats() {
        return new AdminStatsResponse(
                reviewRepository.countByReportedTrueAndHiddenFalse(),
                recipeRepository.count(),
                reviewRepository.count(),
                tagRepository.count(),
                userRepository.count()
        );
    }

    /** Fields the admin user directory may be sorted by; any other value falls back to {@code createdAt}. */
    private static final Set<String> SORTABLE_USER_FIELDS = Set.of("firstName", "lastName", "email", "createdAt");

    /**
     * Retrieves paginated, optionally search-filtered and sorted list of
     * registered users.
     *
     * Complexity: Time: O(S) where S is page size limit Space: O(S)
     *
     * @param page page number index
     * @param size page size limit
     * @param q optional search fragment matched against first name, last name,
     * or email
     * @param enabled optional account status filter (true = active, false =
     * disabled, null = both)
     * @param sortBy field to sort by; must be one of firstName, lastName,
     * email, createdAt
     * @param direction sort direction, "asc" or "desc" (default desc)
     * @return PagedResponse containing UserResponse DTO list
     */
    @Override
    public PagedResponse<UserResponse> getAllUsers(int page, int size, String q, Boolean enabled, String sortBy, String direction) {
        String sortField = SORTABLE_USER_FIELDS.contains(sortBy) ? sortBy : "createdAt";
        Sort sort = "asc".equalsIgnoreCase(direction) ? Sort.by(sortField).ascending() : Sort.by(sortField).descending();
        String normalizedQ = (q == null || q.isBlank()) ? null : q.trim().toLowerCase();

        Page<User> result = userRepository.search(normalizedQ, enabled, PageRequest.of(page, size, sort));
        return PagedResponseMapper.toPagedResponse(result, UserMapper::toResponse);
    }

    /**
     * Retrieves all review entries currently flagged as reported.
     *
     * Complexity: Time: O(R) where R is reported review count Space: O(R)
     *
     * @return list of ReportedReviewResponse DTOs
     */
    @Override
    public PagedResponse<ReportedReviewResponse> getReportedReviews(int page, int size) {
        Page<Review> result = reviewRepository.findByReportedTrueAndHiddenFalse(PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return PagedResponseMapper.toPagedResponse(result, review -> {
            ReviewReport latestReport = reviewReportRepository
                    .findTopByReviewIdOrderByCreatedAtDesc(review.getId())
                    .orElse(null);
            return AdminMapper.toReportedReviewResponse(review, latestReport);
        });
    }

    /**
     * Dismisses moderation report flag on a specific review ID.
     *
     * Complexity: Time: O(1) Space: O(1)
     *
     * @param reviewId target review ID
     */
    @Transactional
    @Override
    public void dismissReport(String reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", reviewId));
        review.setReported(false);
        review.setReportReason(null);
        review.setReportedAt(null);
        reviewRepository.save(review);
    }

    /**
     * Suspends a user account, preventing login and hiding both authored
     * recipes and authored reviews from public listings. Recipes are hidden
     * implicitly, the same way as a self-deactivation or self-deletion request:
     * public recipe listings already filter on {@code createdBy.enabled}.
     * Reviews need an explicit bulk flip since there's no equivalent join-based
     * filter for review authorship.
     * <p>
     * A suspended account is never picked up by the scheduled purge job — that
     * job only ever matches {@code status == DEACTIVATED} rows, never
     * {@code SUSPENDED} ones — so suspension is indefinite and reversible
     * solely by an admin, regardless of how long it lasts. Also clears any
     * leftover {@code deletionRequestedAt} timestamp: if a user had
     * self-requested deletion and an admin suspended them mid-grace-period,
     * that timestamp is now meaningless (the account is admin-suspended, not
     * self-deletion-pending) and left set would misleadingly suggest an active
     * countdown that in fact no longer applies to this account.
     *
     * Complexity: Time: O(R) where R is the user's review count Space: O(1)
     *
     * @param userId target user ID
     */
    @Transactional
    @Override
    public void suspendUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        user.setEnabled(false);
        user.setStatus(User.AccountStatus.SUSPENDED);
        user.setDeletionRequestedAt(null);
        userRepository.save(user);
        reviewRepository.setHiddenByUserId(true, userId);
    }

    /**
     * Reactivates a previously suspended or deactivated user account, restoring
     * both authored recipes and authored reviews to public visibility.
     * Delegates to
     * {@link AccountDeletionServiceImp#restoreFromPendingDeletion(User)}, the
     * same restoration logic the self-service login-restore path uses: it
     * re-enables the account, resets its status to {@code ACTIVE}, clears any
     * pending deletion timestamp (harmless no-op if the account was only
     * suspended, never mid-deletion), and un-hides its reviews.
     *
     * Complexity: Time: O(R) where R is the user's review count Space: O(1)
     *
     * @param userId target user ID
     */
    @Transactional
    @Override
    public void enableUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        accountDeletionService.restoreFromPendingDeletion(user);
    }

    /**
     * Permanently deletes a user account and everything it owns, bypassing the
     * normal 30-day self-service deletion grace period entirely by delegating
     * straight to {@link AccountDeletionService#purgeAccountImmediately(User)}.
     * Refuses to touch the acting admin's own account (self-lockout guard) or
     * any other admin account (prevents admins from removing one another
     * through this console).
     *
     * Complexity: Time: O(P) where P is the account's combined
     * recipe/review/note/favorite graph size Space: O(P)
     *
     * @param userId target user ID
     * @param actingAdminEmail email of the admin performing the deletion
     */
    @Transactional
    @Override
    public void deleteUserPermanently(String userId, String actingAdminEmail) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (user.getEmail() != null && user.getEmail().equalsIgnoreCase(actingAdminEmail)) {
            throw new UnauthorizedActionException("You cannot delete your own account from the admin console.");
        }
        if (user.isAdmin()) {
            throw new UnauthorizedActionException("Admin accounts cannot be deleted from the admin console.");
        }

        accountDeletionService.purgeAccountImmediately(user);
    }

    /**
     * Scans catalog tags to detect duplicate tag groups based on normalized
     * name formatting.
     *
     * Complexity: Time: O(T) where T is total tag count Space: O(T)
     *
     * @return list of DuplicateTagGroupResponse DTOs
     */
    @Override
    public PagedResponse<DuplicateTagGroupResponse> getDuplicateTagGroups(int page, int size) {
        Page<Tag> tagPage = tagRepository.findAll(PageRequest.of(page, size));
        Map<String, List<Tag>> byNormalizedName = new LinkedHashMap<>();
        for (Tag tag : tagPage.getContent()) {
            String normalized = normalize(tag.getName());
            byNormalizedName.computeIfAbsent(normalized, k -> new ArrayList<>()).add(tag);
        }

        List<DuplicateTagGroupResponse> groups = new ArrayList<>();
        for (Map.Entry<String, List<Tag>> entry : byNormalizedName.entrySet()) {
            if (entry.getValue().size() < 2) {
                continue;
            }
            List<TagVariantResponse> variants = entry.getValue().stream()
                    .map(tag -> new TagVariantResponse(tag.getId(), tag.getName(), recipeRepository.countByTagId(tag.getId())))
                    .collect(Collectors.toList());
            groups.add(new DuplicateTagGroupResponse(entry.getKey(), variants));
        }
        return new PagedResponse<>(groups, tagPage.getNumber(), tagPage.getSize(),
                tagPage.getTotalElements(), tagPage.getTotalPages(), tagPage.isLast());
    }

    /**
     * Merges source duplicate tag into canonical target tag using direct SQL
     * and deletes source tag.
     *
     * Complexity: Time: O(R) where R is count of recipes tagged with source tag
     * Space: O(1)
     *
     * @param request tag merge request DTO containing source and target tag IDs
     */
    @Transactional
    @Override
    public void mergeTags(TagMergeRequestDTO request) {
        if (request.sourceTagId().equals(request.targetTagId())) {
            throw new IllegalArgumentException("Source and target tags must be different.");
        }
        if (!tagRepository.existsById(request.sourceTagId())) {
            throw new ResourceNotFoundException("Tag", request.sourceTagId());
        }
        if (!tagRepository.existsById(request.targetTagId())) {
            throw new ResourceNotFoundException("Tag", request.targetTagId());
        }

        jdbcTemplate.update(
                "DELETE rt FROM recipe_tags rt JOIN recipe_tags rt2 ON rt.recipe_id = rt2.recipe_id "
                + "WHERE rt.tag_id = ? AND rt2.tag_id = ?",
                request.sourceTagId(), request.targetTagId());

        jdbcTemplate.update(
                "UPDATE recipe_tags SET tag_id = ? WHERE tag_id = ?",
                request.targetTagId(), request.sourceTagId());

        jdbcTemplate.update("DELETE FROM tags WHERE id = ?", request.sourceTagId());
    }

    private String normalize(String name) {
        if (name == null) {
            return "";
        }
        return name.toLowerCase().trim().replaceAll("[-_\\s]+", " ");
    }
}
