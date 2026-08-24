package com.cooksync.app.domain;

import com.dtos.response.recipe.RecipePreviewResponse;

import java.util.List;

/**
 * Specialized state hierarchy for the home feed, extending the basic {@link ApiResult}
 * to support incremental paginated loading and empty states.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/08/2026
 */
public abstract class FeedState {

    private FeedState() {}

    /** Initial state before any network requests. */
    public static final class Idle extends FeedState {}

    /** Feed is currently fetching either the first page or a subsequent page. */
    public static final class Loading extends FeedState {
        private final boolean initial;

        /**
         * Constructs a loading state.
         *
         * Complexity:
         * Time: O(1)
         * Space: O(1)
         *
         * @param initial {@code true} if this is the first page load (no recipes shown yet),
         *                {@code false} if it is a subsequent page appended to an existing list
         */
        public Loading(boolean initial) {
            this.initial = initial;
        }

        /**
         * Returns whether this loading state is for the first page rather than a subsequent one.
         *
         * Complexity:
         * Time: O(1)
         * Space: O(1)
         *
         * @return {@code true} if this is the first page load
         */
        public boolean isInitial() {
            return initial;
        }
    }

    /** Feed has successfully loaded a list of recipes. */
    public static final class Success extends FeedState {
        private final List<RecipePreviewResponse> recipes;
        private final boolean hasMore;

        /**
         * Constructs a successful feed state.
         *
         * Complexity:
         * Time: O(1)
         * Space: O(1)
         *
         * @param recipes the recipes loaded so far (the full accumulated list, not just the
         *                latest page)
         * @param hasMore {@code true} if another page is available to load
         */
        public Success(List<RecipePreviewResponse> recipes, boolean hasMore) {
            this.recipes = recipes;
            this.hasMore = hasMore;
        }

        /**
         * Returns the recipes loaded so far.
         *
         * Complexity:
         * Time: O(1)
         * Space: O(1)
         *
         * @return the full accumulated recipe list
         */
        public List<RecipePreviewResponse> getRecipes() {
            return recipes;
        }

        /**
         * Returns whether another page is available to load.
         *
         * Complexity:
         * Time: O(1)
         * Space: O(1)
         *
         * @return {@code true} if the feed has not yet reached its last page
         */
        public boolean hasMore() {
            return hasMore;
        }
    }

    /** Feed encountered a network or logic error. */
    public static final class Error extends FeedState {
        private final String message;

        /**
         * Constructs a failed feed state.
         *
         * Complexity:
         * Time: O(1)
         * Space: O(1)
         *
         * @param message user-facing description of what went wrong
         */
        public Error(String message) {
            this.message = message;
        }

        /**
         * Returns the user-facing error message.
         *
         * Complexity:
         * Time: O(1)
         * Space: O(1)
         *
         * @return the error message
         */
        public String getMessage() {
            return message;
        }
    }
}
