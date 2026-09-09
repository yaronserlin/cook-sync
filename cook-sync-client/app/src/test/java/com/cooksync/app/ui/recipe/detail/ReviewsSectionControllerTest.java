package com.cooksync.app.ui.recipe.detail;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.dtos.response.review.ReviewResponse;

import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.List;

/**
 * Unit tests for {@link ReviewsSectionController}'s review-list, star-filter, sort, and
 * optimistic-delete/restore state transitions.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 09/09/2026
 */
public class ReviewsSectionControllerTest {

    private ReviewsSectionController controller;
    private ReviewResponse review1;
    private ReviewResponse review2;

    @Before
    public void setUp() {
        controller = new ReviewsSectionController();
        review1 = new ReviewResponse("r1", "u1", "Alice", null, "recipe-1",
                BigDecimal.valueOf(5), "Great", "Loved it", "2026-01-01T00:00:00Z", "2026-01-01T00:00:00Z");
        review2 = new ReviewResponse("r2", "u2", "Bob", null, "recipe-1",
                BigDecimal.valueOf(3), "Ok", "It was fine", "2026-01-02T00:00:00Z", "2026-01-02T00:00:00Z");
    }

    @Test
    public void setReviews_populatesListAndResetsFilterAndSort() {
        controller.toggleStarFilter(5);
        controller.advanceSort();

        controller.setReviews(List.of(review1, review2));

        assertEquals(List.of(review1, review2), controller.getReviews());
        assertNull(controller.getActiveStarFilter());
        assertEquals(RecipeDetailViewModel.ReviewSort.NEWEST, controller.getCurrentSort());
    }

    @Test
    public void setReviews_toleratesNullList() {
        controller.setReviews(null);

        assertTrue(controller.getReviews().isEmpty());
    }

    @Test
    public void markPendingDelete_removesReviewFromList() {
        controller.setReviews(List.of(review1, review2));

        controller.markPendingDelete(review1);

        assertEquals(List.of(review2), controller.getReviews());
    }

    @Test
    public void restorePendingDelete_addsBackAndClearsPending_whenADeleteIsPending() {
        controller.setReviews(List.of(review1, review2));
        controller.markPendingDelete(review1);

        boolean restored = controller.restorePendingDelete();

        assertTrue(restored);
        assertEquals(List.of(review2, review1), controller.getReviews());
        assertFalse(controller.restorePendingDelete());
    }

    @Test
    public void restorePendingDelete_returnsFalse_whenNothingPending() {
        assertFalse(controller.restorePendingDelete());
    }

    @Test
    public void toggleStarFilter_setsThenClearsOnSecondTapOfSameStar() {
        controller.toggleStarFilter(4);
        assertEquals(Integer.valueOf(4), controller.getActiveStarFilter());

        controller.toggleStarFilter(4);
        assertNull(controller.getActiveStarFilter());
    }

    @Test
    public void toggleStarFilter_switchesToDifferentStar() {
        controller.toggleStarFilter(4);
        controller.toggleStarFilter(2);

        assertEquals(Integer.valueOf(2), controller.getActiveStarFilter());
    }

    @Test
    public void clearStarFilter_clearsRegardlessOfCurrentValue() {
        controller.toggleStarFilter(3);

        controller.clearStarFilter();

        assertNull(controller.getActiveStarFilter());
    }

    @Test
    public void advanceSort_cyclesThroughAllThreeOptionsAndWrapsAround() {
        assertEquals(RecipeDetailViewModel.ReviewSort.NEWEST, controller.getCurrentSort());

        controller.advanceSort();
        assertEquals(RecipeDetailViewModel.ReviewSort.HIGHEST_RATED, controller.getCurrentSort());

        controller.advanceSort();
        assertEquals(RecipeDetailViewModel.ReviewSort.LOWEST_RATED, controller.getCurrentSort());

        controller.advanceSort();
        assertEquals(RecipeDetailViewModel.ReviewSort.NEWEST, controller.getCurrentSort());
    }
}
