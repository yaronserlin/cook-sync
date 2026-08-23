/**
 * Shared DTO-layer component of the Cloudinary image-upload feature. Defines the response shape
 * {@code CloudinaryController.getSignature} returns and the Android client's
 * {@code CloudinaryUploader} consumes to authorize a direct-to-Cloudinary upload without the
 * client ever holding the account's API secret.
 */
package com.dtos.response.cloudinary;

/**
 * Response payload carrying short-lived Cloudinary signed-upload credentials, issued by the
 * server's {@code CloudinaryController.getSignature} endpoint via the Cloudinary service. The
 * Android client's {@code CloudinaryUploader} attaches these values to its direct upload request
 * so Cloudinary can authorize the transfer without the client ever holding the account's API secret.
 *
 * @param signature the HMAC signature computed server-side over the upload parameters, proving the request was authorized
 * @param timestamp the Unix epoch time, in seconds, at which the signature was generated
 * @param apiKey the Cloudinary account's public API key
 * @param cloudName the identifier of the target Cloudinary cloud that will store the uploaded media
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
public record CloudinarySignatureResponse(
        String signature,
        long timestamp,
        String apiKey,
        String cloudName
) {
}
