package com.cooksync_server.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cooksync_server.entities.Recipe;
import com.cooksync_server.entities.User;
import com.cooksync_server.repositories.FavoriteRecipeRepository;
import com.cooksync_server.repositories.PasswordResetTokenRepository;
import com.cooksync_server.repositories.PersonalInstructionNoteRepository;
import com.cooksync_server.repositories.RecipeRepository;
import com.cooksync_server.repositories.ReviewReportRepository;
import com.cooksync_server.repositories.ReviewRepository;
import com.cooksync_server.repositories.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service class managing the self-service account-deletion lifecycle: starting the 30-day
 * grace period, restoring an account if the user logs back in during that window, and
 * permanently purging accounts whose grace period has lapsed.
 * <p>
 * A deletion request reuses {@link User.AccountStatus#DEACTIVATED} rather than introducing a
 * dedicated status value, so the only difference between a plain self-deactivation and a
 * deletion request is whether {@link User#getDeletionRequestedAt()} is set. Recipe visibility
 * needs no extra handling here: {@code RecipeSpecifications.isPublicAndEnabled()} already
 * filters public listings on {@code createdBy.enabled}, so disabling the account alone already
 * hides the user's recipes.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 08/08/2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountDeletionServiceImp implements AccountDeletionService {

    /** Grace period between a deletion request and permanent purge. */
    private static final long GRACE_PERIOD_DAYS = 30;

    /** Placeholder ID substituted for an empty recipe-ID list so IN-clause queries stay well-formed. */
    private static final List<String> NO_RECIPES = List.of("");

    private final UserRepository userRepository;
    private final RecipeRepository recipeRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewReportRepository reviewReportRepository;
    private final PersonalInstructionNoteRepository personalInstructionNoteRepository;
    private final FavoriteRecipeRepository favoriteRecipeRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RefreshTokenService refreshTokenService;
    private final CloudinaryService cloudinaryService;

    /**
     * Starts the 30-day account-deletion grace period: disables the account, hides every review
     * the user authored from public view, and revokes active sessions. The user's recipes are
     * hidden implicitly since public listings already filter on {@code createdBy.enabled}.
     *
     * Complexity:
     * Time: O(R) where R is the user's review count
     * Space: O(1)
     *
     * @param user the account requesting deletion, already password-verified by the caller
     */
    @Override
    @Transactional
    public void requestDeletion(User user) {
        user.setEnabled(false);
        user.setStatus(User.AccountStatus.DEACTIVATED);
        user.setDeletionRequestedAt(LocalDateTime.now());
        userRepository.save(user);

        reviewRepository.setHiddenByUserId(true, user.getId());
        refreshTokenService.deleteByUserId(user.getId());

        log.info("Account deletion requested for user ID: {}", user.getId());
    }

    /**
     * Restores an account to normal: re-enables it, resets its status to {@code ACTIVE}, clears
     * any pending deletion timestamp, and un-hides its reviews. Their recipes reappear
     * automatically once {@code enabled} flips back to true. Used both when a user logs back in
     * within their own 30-day deletion grace period ({@link AuthServiceImp#login}), and when an
     * admin reactivates a suspended or deactivated account ({@code AdminServiceImp#enableUser}) —
     * the same "bring this account back to normal" operation either way.
     *
     * Complexity:
     * Time: O(R) where R is the user's review count
     * Space: O(1)
     *
     * @param user the account being restored
     */
    @Override
    @Transactional
    public void restoreFromPendingDeletion(User user) {
        user.setEnabled(true);
        user.setStatus(User.AccountStatus.ACTIVE);
        user.setDeletionRequestedAt(null);
        userRepository.save(user);

        reviewRepository.setHiddenByUserId(false, user.getId());

        log.info("Account deletion cancelled by login for user ID: {}", user.getId());
    }

    /**
     * Scheduled entry point that permanently purges every account whose 30-day deletion grace
     * period has lapsed without the user logging back in. Intended to be invoked by a daily
     * cron trigger.
     *
     * Complexity:
     * Time: O(U * (P + N)) where U is expired-account count, P is each user's recipe/review
     * graph size, and N is the personal-note/favorite cleanup cost per account
     * Space: O(P) per account processed
     */
    @Override
    @Transactional
    public void purgeExpiredAccounts() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(GRACE_PERIOD_DAYS);
        List<User> expiredAccounts = userRepository.findByStatusAndDeletionRequestedAtBefore(
                User.AccountStatus.DEACTIVATED, cutoff);

        if (expiredAccounts.isEmpty()) {
            return;
        }

        log.info("Purging {} account(s) past the 30-day deletion grace period", expiredAccounts.size());
        for (User user : expiredAccounts) {
            purgeAccount(user);
        }
    }

    /**
     * Permanently purges a single account right now, bypassing the 30-day grace period —
     * delegates straight to {@link #purgeAccount(User)}, the same erasure logic the scheduled
     * job uses, just without the status/cutoff filtering. Used for admin-initiated hard deletes.
     *
     * Complexity:
     * Time: O(P) where P is the account's combined recipe/review/note/favorite graph size
     * Space: O(P)
     *
     * @param user the account to purge immediately
     */
    @Override
    @Transactional
    public void purgeAccountImmediately(User user) {
        log.info("Admin-initiated immediate purge overriding the 30-day grace period for user ID: {}", user.getId());
        purgeAccount(user);
    }

    /**
     * Permanently erases a single account and everything it owns, in FK-safe order: dependent
     * moderation reports, personal notes, and favorites are cleaned up first (none of them
     * cascade-delete from {@code User} or {@code Recipe}), then Cloudinary media assets (avatar
     * and owned recipe images) are deleted, then the user's recipes are removed as managed
     * entities so their own {@code cascade = ALL, orphanRemoval = true} relations (ingredients,
     * instructions, images, description blocks, reviews) are cleaned up by Hibernate, then the
     * user's remaining reviews, sessions, and reset tokens, and finally the user row itself.
     *
     * Complexity:
     * Time: O(P) where P is the user's combined recipe/review/note/favorite graph size
     * Space: O(P)
     *
     * @param user the expired account to purge
     */
    private void purgeAccount(User user) {
        String userId = user.getId();
        List<Recipe> ownedRecipes = recipeRepository.findByCreatedById(userId, Pageable.unpaged()).getContent();
        List<String> recipeIds = ownedRecipes.isEmpty()
                ? NO_RECIPES
                : ownedRecipes.stream().map(Recipe::getId).toList();

        // Collected before the bulk deletes below: several of them are @Modifying(clearAutomatically
        // = true), which detaches ownedRecipes from the persistence context — reading their lazy
        // instructions/images/descriptionBlocks collections afterward would throw
        // LazyInitializationException, so this has to run while they're still managed.
        List<String> imagesToDelete = new ArrayList<>();
        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isBlank()) {
            imagesToDelete.add(user.getAvatarUrl());
        }
        for (Recipe recipe : ownedRecipes) {
            imagesToDelete.addAll(RecipeImageUtils.extractAllImageUrls(recipe));
        }

        List<String> reviewIdsToClean = reviewRepository.findIdsByUserIdOrRecipeIdIn(userId, recipeIds);
        if (!reviewIdsToClean.isEmpty()) {
            reviewReportRepository.deleteByReviewIdIn(reviewIdsToClean);
        }
        reviewReportRepository.deleteByReporterId(userId);

        personalInstructionNoteRepository.deleteByUserIdOrRecipeIdIn(userId, recipeIds);
        favoriteRecipeRepository.deleteByUserIdOrRecipeIdIn(userId, recipeIds);
        reviewRepository.deleteByUserId(userId);

        if (!imagesToDelete.isEmpty()) {
            cloudinaryService.deleteImages(imagesToDelete);
        }
        cloudinaryService.deleteFolder(cloudinaryService.buildUserFolder(user.getEmail(), null));

        if (!ownedRecipes.isEmpty()) {
            recipeRepository.deleteAll(ownedRecipes);
        }

        refreshTokenService.deleteByUserId(userId);
        passwordResetTokenRepository.deleteByUserId(userId);
        userRepository.delete(user);

        log.info("Permanently purged account ID: {}", userId);
    }
}
