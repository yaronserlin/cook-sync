package com.cooksync.app.ui.recipe.detail;

import com.dtos.response.review.ReviewResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * Owns {@link RecipeDetailActivity}'s reviews-section state: the recipe's full review list, the
 * active star-value filter, the current sort order, and the optimistic-delete-with-undo
 * bookkeeping — deliberately free of any Android/View dependency so it can be unit tested on the
 * plain JVM. {@link RecipeDetailActivity} owns one instance and delegates every reviews-section
 * state transition to it, keeping view-binding code and state logic separate.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 09/09/2026
 */
class ReviewsSectionController {

    private final List<ReviewResponse> reviews = new ArrayList<>();
    private ReviewResponse pendingDeletedReview;
    private Integer activeStarFilter;
    private RecipeDetailViewModel.ReviewSort currentSort = RecipeDetailViewModel.ReviewSort.NEWEST;

    /**
     * Replaces the full review list for a freshly (re)loaded recipe, resetting the star filter
     * and sort order back to their defaults.
     *
     * @param newReviews the recipe's reviews, or {@code null}
     */
    void setReviews(List<ReviewResponse> newReviews) {
        reviews.clear();
        if (newReviews != null) {
            reviews.addAll(newReviews);
        }
        activeStarFilter = null;
        currentSort = RecipeDetailViewModel.ReviewSort.NEWEST;
    }

    /**
     * @return the full, unfiltered review list backing this section
     */
    List<ReviewResponse> getReviews() {
        return reviews;
    }

    /**
     * Optimistically removes a review pending a deferred server-side delete, recording it so it
     * can be put back by {@link #restorePendingDelete()} if the delete is undone or fails.
     *
     * @param review the review being deleted
     */
    void markPendingDelete(ReviewResponse review) {
        reviews.remove(review);
        pendingDeletedReview = review;
    }

    /**
     * Restores the most recently {@link #markPendingDelete(ReviewResponse) marked} review, if
     * one is still pending (i.e. its delete was undone or failed server-side before a newer
     * delete overwrote it).
     *
     * @return {@code true} if a review was pending and got restored; {@code false} if none was
     */
    boolean restorePendingDelete() {
        if (pendingDeletedReview == null) {
            return false;
        }
        reviews.add(pendingDeletedReview);
        pendingDeletedReview = null;
        return true;
    }

    /**
     * @return the currently active star-value filter (1-5), or {@code null} if unfiltered
     */
    Integer getActiveStarFilter() {
        return activeStarFilter;
    }

    /**
     * Toggles the given star value as the active filter: activates it if it wasn't already
     * active, or clears the filter entirely if it was.
     *
     * @param star the tapped star-filter chip's value
     */
    void toggleStarFilter(int star) {
        activeStarFilter = (activeStarFilter != null && activeStarFilter == star) ? null : star;
    }

    /**
     * Clears the active star filter, unconditionally.
     */
    void clearStarFilter() {
        activeStarFilter = null;
    }

    /**
     * @return the review list's current sort order
     */
    RecipeDetailViewModel.ReviewSort getCurrentSort() {
        return currentSort;
    }

    /**
     * Advances to the next sort order in {@link RecipeDetailViewModel.ReviewSort}'s cycle.
     */
    void advanceSort() {
        currentSort = currentSort.next();
    }
}
