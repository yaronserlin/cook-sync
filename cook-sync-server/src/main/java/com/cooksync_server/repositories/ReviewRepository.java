/**
 * Server-side persistence-layer component of the Reviews feature. Defines the Spring Data JPA
 * query surface over {@code Review} entities that {@code ReviewServiceImp} builds its business
 * logic on, and that the account-deletion subsystem uses to hide, restore, or permanently purge a
 * departing user's reviews.
 */
package com.cooksync_server.repositories;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cooksync_server.entities.Review;

/**
 * Spring Data JPA Repository interface for Review entity persistence and moderation operations.
 *
 * @author Yaron Serlin
 * @version 1.1
 * @since 02/08/2026
 */
@Repository
public interface ReviewRepository extends JpaRepository<Review, String> {

    /**
     * Retrieves non-hidden review entries for a given recipe ordered by creation timestamp
     * descending, excluding reviews whose author has a pending account-deletion request.
     *
     * @param recipeId target recipe unique identifier
     * @param pageable pagination parameters
     * @return page of matching review entities
     */
    Page<Review> findByRecipeIdAndHiddenFalseOrderByCreatedAtDesc(String recipeId, Pageable pageable);

    /**
     * Retrieves reports still awaiting administrative moderation: flagged as reported and not
     * already hidden. Excluding hidden reviews means a report resolves itself the moment its
     * review is hidden by any means — most notably {@link #setHiddenByUserId}, called when an
     * admin suspends the reviewer — without a separate step to explicitly dismiss the report.
     * If the reviewer is later un-suspended (unhiding their reviews), the report reappears here
     * rather than being silently lost, since {@code reported}/{@code reportReason} are untouched.
     *
     * @param pageable pagination parameters
     * @return page of reported, currently-visible review entities
     */
    Page<Review> findByReportedTrueAndHiddenFalse(Pageable pageable);

    /**
     * Counts reports still awaiting administrative moderation, matching
     * {@link #findByReportedTrueAndHiddenFalse} exactly so the admin dashboard's "N open" badge
     * always agrees with what the moderation queue itself shows.
     *
     * @return aggregate count of reported, currently-visible reviews
     */
    long countByReportedTrueAndHiddenFalse();

    /**
     * Retrieves the IDs of every review either authored by the given user or attached to one of
     * the given recipes, i.e. every review that account-deletion processing is about to remove
     * (directly, or via recipe cascade). Used to clean up dependent {@code ReviewReport} rows
     * before those reviews are deleted.
     *
     * @param userId author user ID whose reviews are included
     * @param recipeIds recipe IDs whose reviews are included
     * @return list of matching review IDs
     */
    @Query("SELECT r.id FROM Review r WHERE r.user.id = :userId OR r.recipe.id IN :recipeIds")
    List<String> findIdsByUserIdOrRecipeIdIn(@Param("userId") String userId, @Param("recipeIds") List<String> recipeIds);

    /**
     * Bulk-flips the hidden flag for every review authored by a user, used to hide their reviews
     * when an account-deletion request starts and to restore them if the user logs back in
     * within the grace period. Deliberately does NOT clear the persistence context (unlike the
     * other bulk-cleanup queries in this codebase): both callers in {@code AccountDeletionServiceImp}
     * mutate the {@code User} entity in the same transaction around this call, and an automatic
     * clear would detach that entity before its pending field changes (enabled/status/
     * deletionRequestedAt) are flushed — silently discarding them instead of persisting them.
     *
     * @param hidden new hidden flag value
     * @param userId author user ID
     */
    @Modifying
    @Query("UPDATE Review r SET r.hidden = :hidden WHERE r.user.id = :userId")
    void setHiddenByUserId(@Param("hidden") boolean hidden, @Param("userId") String userId);

    /**
     * Bulk-deletes every review authored by a user, as the final permanent-purge step for an
     * expired account-deletion request. Reviews the user authored on their own (already-deleted)
     * recipes are a no-op here since the recipe cascade removes them first.
     *
     * @param userId author user ID
     */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Review r WHERE r.user.id = :userId")
    void deleteByUserId(@Param("userId") String userId);
}
