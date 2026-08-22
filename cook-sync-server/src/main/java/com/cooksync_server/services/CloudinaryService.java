package com.cooksync_server.services;

import java.util.List;

import com.dtos.response.cloudinary.CloudinarySignatureResponse;

/**
 * Service interface for Cloudinary operations including signature generation and asset deletion.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
public interface CloudinaryService {

    /**
     * Generates a signed upload signature payload for client direct uploads.
     *
     * @return CloudinarySignatureResponse DTO containing signature details and timestamp
     */
    CloudinarySignatureResponse generateUploadSignature();

    /**
     * Generates a signed upload signature payload for client direct uploads targeting a specific folder and public ID.
     *
     * @param folder target folder path, or null for default
     * @param publicId target asset public ID, or null for auto-generated
     * @return CloudinarySignatureResponse DTO containing signature details and timestamp
     */
    CloudinarySignatureResponse generateUploadSignature(String folder, String publicId);

    /**
     * Returns the configured root Cloudinary folder for the active environment
     * (e.g. {@code "cooksync-dev"} locally, {@code "CookSyncApp"} in production).
     *
     * @return the configured base folder name
     */
    String getBaseFolder();

    /**
     * Builds a per-user Cloudinary folder path rooted at {@link #getBaseFolder()}, in the
     * form {@code "<baseFolder>/<userEmail>[/<subPath>]"}.
     *
     * @param userEmail the owning user's email address
     * @param subPath   optional trailing path segment (e.g. {@code "avatar"} or a recipe
     *                  title), or {@code null}/blank to target the user's root folder
     * @return the fully qualified Cloudinary folder path
     */
    String buildUserFolder(String userEmail, String subPath);

    /**
     * Deletes a single image asset from Cloudinary storage given its public URL.
     *
     * @param imageUrl target image URL string
     */
    void deleteImage(String imageUrl);

    /**
     * Deletes a collection of image assets from Cloudinary storage given their public URLs.
     * Runs asynchronously so callers holding a database transaction don't block on the external
     * HTTP round-trip.
     *
     * @param imageUrls list of target image URL strings
     */
    void deleteImages(List<String> imageUrls);

    /**
     * Deletes all image assets and the folder itself from Cloudinary storage.
     * Runs asynchronously so callers holding a database transaction don't block on the external
     * HTTP round-trip.
     *
     * @param folderPath target folder path to delete
     */
    void deleteFolder(String folderPath);
}