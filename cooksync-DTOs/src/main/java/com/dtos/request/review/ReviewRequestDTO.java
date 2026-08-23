/**
 * Shared DTO-layer component of the Reviews feature. Defines the request payload the Android
 * client's {@code RecipeRepository.submitReview} sends and {@code ReviewController.addReview}
 * validates and consumes on the server.
 */
package com.dtos.request.review;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Data Transfer Object for creating or updating a user recipe review.
 * Encapsulates numerical rating value, review title, and detailed commentary text.
 *
 * @param rating the numeric rating score between 1.0 and 5.0, must not be null
 * @param title the headline title of the review, must not be blank
 * @param comment optional detailed text commentary from the user
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
public record ReviewRequestDTO(
        @NotNull(message = "Rating is required")
        @DecimalMin(value = "1.0", message = "Minimum rating is 1.0")
        @DecimalMax(value = "5.0", message = "Maximum rating is 5.0")
        Double rating,

        @NotBlank(message = "Review title is required")
        String title,

        String comment
) {
}
