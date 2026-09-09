package com.cooksync_server.recipeimport;

/**
 * Signals that a {@link RecipeExtractionProvider} call itself failed (network error, non-2xx
 * response, quota/rate-limit rejection, or a malformed response envelope) — as opposed to the
 * call succeeding but determining the given content isn't a recipe. {@code
 * RecipeImportServiceImp} maps this to a distinct {@code EXTRACTION_SERVICE_UNAVAILABLE} job
 * error code, so a transient outage or an exhausted API quota isn't reported to the user as the
 * misleading "we couldn't find a recipe here."
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 05/09/2026
 */
public class ExtractionServiceException extends RuntimeException {

    public ExtractionServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
