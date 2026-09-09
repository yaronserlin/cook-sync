package com.cooksync_server.exceptions;

/**
 * Custom runtime exception thrown when a per-user usage limit on a costly operation (currently:
 * starting a recipe-import job, which spends a real LLM/OCR call) is exceeded within its tracking
 * window.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 05/09/2026
 */
public class RateLimitExceededException extends RuntimeException {

    /**
     * Constructs a RateLimitExceededException with context details.
     *
     * @param message exception message context
     */
    public RateLimitExceededException(String message) {
        super(message);
    }
}
