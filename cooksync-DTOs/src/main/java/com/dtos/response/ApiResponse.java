package com.dtos.response;

/**
 * Generic Data Transfer Object wrapper for standardizing API response structures.
 * Encapsulates status flag, data payload, error details, and user-facing message.
 *
 * @param <T> the generic type of the success payload
 * @param success boolean flag indicating request execution outcome
 * @param data primary data object returned on success
 * @param error error detail payload returned on failure
 * @param message contextual message summarizing result
 * @author Yaron Serlin
 * @version 1.1
 * @since 02/08/2026
 */
public record ApiResponse<T>(
        boolean success,
        T data,
        Object error,
        String message
) {

    /**
     * Constructs a successful API response with data payload and custom message.
     *
     * Complexity:
     * Time: O(1)
     * Space: O(1)
     *
     * @param <T> the type of the payload
     * @param data the response data object
     * @param message custom success context string
     * @return a new successful ApiResponse instance
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, data, null, message);
    }

    /**
     * Constructs a successful API response with data payload and default message.
     *
     * Complexity:
     * Time: O(1)
     * Space: O(1)
     *
     * @param <T> the type of the payload
     * @param data the response data object
     * @return a new successful ApiResponse instance
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, "Operation completed successfully");
    }

    /**
     * Constructs a failed API response with error payload and message.
     *
     * Complexity:
     * Time: O(1)
     * Space: O(1)
     *
     * @param <T> the expected payload type
     * @param error error details object
     * @param message error description message
     * @return a new failed ApiResponse instance
     */
    public static <T> ApiResponse<T> error(Object error, String message) {
        return new ApiResponse<>(false, null, error, message);
    }

    /**
     * Constructs a failed API response with an error message.
     *
     * Complexity:
     * Time: O(1)
     * Space: O(1)
     *
     * @param <T> the expected payload type
     * @param message error description message
     * @return a new failed ApiResponse instance
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, null, null, message);
    }
}
