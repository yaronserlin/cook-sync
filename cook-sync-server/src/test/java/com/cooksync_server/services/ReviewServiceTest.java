package com.cooksync_server.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cooksync_server.entities.Recipe;
import com.cooksync_server.entities.Review;
import com.cooksync_server.entities.User;
import com.cooksync_server.exceptions.ResourceNotFoundException;
import com.cooksync_server.exceptions.auth.UnauthorizedActionException;
import com.cooksync_server.repositories.RecipeRepository;
import com.cooksync_server.repositories.ReviewReportRepository;
import com.cooksync_server.repositories.ReviewRepository;
import com.cooksync_server.repositories.UserRepository;
import com.dtos.request.review.ReportReviewRequestDTO;
import com.dtos.request.review.ReviewRequestDTO;

/**
 * Unit test suite verifying review creation, average rating recomputation, deletion authorization,
 * and moderation reporting in ReviewService.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 12/08/2026
 */
@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private RecipeRepository recipeRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ReviewReportRepository reviewReportRepository;

    @InjectMocks
    private ReviewService reviewService;

    private User author;
    private User otherUser;
    private Recipe sampleRecipe;

    @BeforeEach
    void setUp() {
        author = User.builder().id("user-1").email("gordon@cooksync.com").build();
        otherUser = User.builder().id("user-2").email("other@cooksync.com").build();
        sampleRecipe = Recipe.builder().id("recipe-1").title("Beef Wellington").createdBy(author)
                .reviewCount(0).reviews(new HashSet<>()).build();
    }

    @Test
    void getReviewsForRecipe_ShouldThrowResourceNotFoundException_WhenRecipeMissing() {
        when(recipeRepository.existsById("missing")).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> reviewService.getReviewsForRecipe("missing", 0, 10));
    }

    @Test
    void addReview_ShouldPersistReviewAndRecomputeAverageRating() {
        ReviewRequestDTO request = new ReviewRequestDTO(4.0, "Fantastic", "Loved it");
        when(userRepository.findByEmail("gordon@cooksync.com")).thenReturn(Optional.of(author));
        when(recipeRepository.findById("recipe-1")).thenReturn(Optional.of(sampleRecipe));

        reviewService.addReview("recipe-1", request, "gordon@cooksync.com");

        verify(reviewRepository).save(org.mockito.ArgumentMatchers.any(Review.class));
        assertEquals(1, sampleRecipe.getReviewCount());
        assertEquals(4.0, sampleRecipe.getAverageRating());
        verify(recipeRepository).save(sampleRecipe);
    }

    @Test
    void deleteReview_ShouldThrowUnauthorizedActionException_WhenUserIsNotAuthorOrAdmin() {
        Review review = Review.builder().id("review-1").user(author).recipe(sampleRecipe)
                .rating(BigDecimal.valueOf(4.0)).build();
        when(reviewRepository.findById("review-1")).thenReturn(Optional.of(review));
        when(userRepository.findByEmail("other@cooksync.com")).thenReturn(Optional.of(otherUser));

        assertThrows(UnauthorizedActionException.class,
                () -> reviewService.deleteReview("review-1", "other@cooksync.com"));
    }

    @Test
    void deleteReview_ShouldRemoveReviewAndClearReports_WhenUserIsAuthor() {
        Review review = Review.builder().id("review-1").user(author).recipe(sampleRecipe)
                .rating(BigDecimal.valueOf(4.0)).build();
        sampleRecipe.setReviewCount(1);
        sampleRecipe.getReviews().add(review);
        when(reviewRepository.findById("review-1")).thenReturn(Optional.of(review));
        when(userRepository.findByEmail("gordon@cooksync.com")).thenReturn(Optional.of(author));

        reviewService.deleteReview("review-1", "gordon@cooksync.com");

        verify(reviewReportRepository).deleteByReviewId("review-1");
        verify(reviewRepository).delete(review);
        assertEquals(0, sampleRecipe.getReviewCount());
    }

    @Test
    void reportReview_ShouldPersistReportAndFlagReview() {
        Review review = Review.builder().id("review-1").user(author).recipe(sampleRecipe)
                .rating(BigDecimal.valueOf(4.0)).build();
        ReportReviewRequestDTO request = new ReportReviewRequestDTO("SPAM", "Looks like spam");
        when(userRepository.findByEmail("other@cooksync.com")).thenReturn(Optional.of(otherUser));
        when(reviewRepository.findById("review-1")).thenReturn(Optional.of(review));

        reviewService.reportReview("review-1", request, "other@cooksync.com");

        verify(reviewReportRepository).save(org.mockito.ArgumentMatchers.any());
        assertEquals(Review.ReportReason.SPAM, review.getReportReason());
        assertEquals(true, review.isReported());
    }
}
