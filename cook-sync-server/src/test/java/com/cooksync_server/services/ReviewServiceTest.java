/**
 * Server-side test-layer component of the Reviews feature. Unit-tests {@code ReviewServiceImp}
 * in isolation from {@code ReviewRepository}, {@code RecipeRepository}, {@code UserRepository},
 * and {@code ReviewReportRepository} via Mockito, verifying review creation, average-rating
 * recomputation, deletion authorization, and moderation-report submission.
 */
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
 * and moderation reporting in ReviewServiceImp.
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
    private ReviewServiceImp reviewService;

    private User author;
    private User otherUser;
    private Recipe sampleRecipe;

    /**
     * Builds a review author, an unrelated second user, and an empty sample recipe shared as a
     * fixture across every test in this suite.
     */
    @BeforeEach
    void setUp() {
        author = User.builder().id("user-1").email("gordon@cooksync.com").build();
        otherUser = User.builder().id("user-2").email("other@cooksync.com").build();
        sampleRecipe = Recipe.builder().id("recipe-1").title("Beef Wellington").createdBy(author)
                .reviewCount(0).reviews(new HashSet<>()).build();
    }

    /**
     * Verifies that listing reviews for a recipe ID with no matching recipe fails fast with
     * {@link ResourceNotFoundException} rather than returning an empty page.
     */
    @Test
    void getReviewsForRecipe_ShouldThrowResourceNotFoundException_WhenRecipeMissing() {
        when(recipeRepository.existsById("missing")).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> reviewService.getReviewsForRecipe("missing", 0, 10));
    }

    /**
     * Verifies that submitting a review persists the entity and recomputes the recipe's review
     * count and average rating in the same call.
     */
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

    /**
     * Verifies that adding a review fails fast with {@link ResourceNotFoundException} when the
     * acting user cannot be resolved, before any review or recipe mutation happens.
     */
    @Test
    void addReview_ShouldThrowResourceNotFoundException_WhenUserMissing() {
        ReviewRequestDTO request = new ReviewRequestDTO(4.0, "Fantastic", "Loved it");
        when(userRepository.findByEmail("missing@cooksync.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> reviewService.addReview("recipe-1", request, "missing@cooksync.com"));

        verify(reviewRepository, org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.any(Review.class));
    }

    /**
     * Verifies that adding a review fails fast with {@link ResourceNotFoundException} when the
     * target recipe cannot be resolved.
     */
    @Test
    void addReview_ShouldThrowResourceNotFoundException_WhenRecipeMissing() {
        ReviewRequestDTO request = new ReviewRequestDTO(4.0, "Fantastic", "Loved it");
        when(userRepository.findByEmail("gordon@cooksync.com")).thenReturn(Optional.of(author));
        when(recipeRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> reviewService.addReview("missing", request, "gordon@cooksync.com"));

        verify(reviewRepository, org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.any(Review.class));
    }

    /**
     * Verifies that deleting a review authored by someone else is rejected with
     * {@link UnauthorizedActionException}.
     */
    @Test
    void deleteReview_ShouldThrowUnauthorizedActionException_WhenUserIsNotAuthorOrAdmin() {
        Review review = Review.builder().id("review-1").user(author).recipe(sampleRecipe)
                .rating(BigDecimal.valueOf(4.0)).build();
        when(reviewRepository.findById("review-1")).thenReturn(Optional.of(review));
        when(userRepository.findByEmail("other@cooksync.com")).thenReturn(Optional.of(otherUser));

        assertThrows(UnauthorizedActionException.class,
                () -> reviewService.deleteReview("review-1", "other@cooksync.com"));
    }

    /**
     * Verifies that the review's own author can delete it, that any moderation reports against it
     * are cleared first, and that the recipe's review count drops accordingly.
     */
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

    /**
     * Verifies that reporting a review persists a {@code ReviewReport} record and flags the
     * review's own {@code reported}/{@code reportReason} fields to match.
     */
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

    /**
     * Verifies that reporting a review fails fast with {@link ResourceNotFoundException} when the
     * reporting user cannot be resolved, before any report is persisted.
     */
    @Test
    void reportReview_ShouldThrowResourceNotFoundException_WhenUserMissing() {
        ReportReviewRequestDTO request = new ReportReviewRequestDTO("SPAM", "Looks like spam");
        when(userRepository.findByEmail("missing@cooksync.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> reviewService.reportReview("review-1", request, "missing@cooksync.com"));

        verify(reviewReportRepository, org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.any());
    }

    /**
     * Verifies that reporting a review fails fast with {@link ResourceNotFoundException} when the
     * target review cannot be resolved.
     */
    @Test
    void reportReview_ShouldThrowResourceNotFoundException_WhenReviewMissing() {
        ReportReviewRequestDTO request = new ReportReviewRequestDTO("SPAM", "Looks like spam");
        when(userRepository.findByEmail("other@cooksync.com")).thenReturn(Optional.of(otherUser));
        when(reviewRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> reviewService.reportReview("missing", request, "other@cooksync.com"));

        verify(reviewReportRepository, org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.any());
    }

    /**
     * Verifies that reporting a review with an unrecognized reason string propagates
     * {@link IllegalArgumentException} from {@code Review.ReportReason.valueOf}, since the service
     * itself performs no validation of the reason beyond what the DTO's bean validation already
     * guards at the controller boundary.
     */
    @Test
    void reportReview_ShouldThrowIllegalArgumentException_WhenReasonInvalid() {
        Review review = Review.builder().id("review-1").user(author).recipe(sampleRecipe)
                .rating(BigDecimal.valueOf(4.0)).build();
        ReportReviewRequestDTO request = new ReportReviewRequestDTO("NOT_A_REAL_REASON", "Looks like spam");
        when(userRepository.findByEmail("other@cooksync.com")).thenReturn(Optional.of(otherUser));
        when(reviewRepository.findById("review-1")).thenReturn(Optional.of(review));

        assertThrows(IllegalArgumentException.class,
                () -> reviewService.reportReview("review-1", request, "other@cooksync.com"));

        verify(reviewReportRepository, org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.any());
    }
}
