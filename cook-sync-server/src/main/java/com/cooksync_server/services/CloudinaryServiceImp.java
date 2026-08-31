package com.cooksync_server.services;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.dtos.response.cloudinary.CloudinarySignatureResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service class generating short-lived signed upload authorizations for Cloudinary SDK and
 * managing Cloudinary image asset deletions.
 * Ensures the API secret remains securely on the backend server.
 *
 * @author Yaron Serlin
 * @version 1.1
 * @since 02/08/2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryServiceImp implements CloudinaryService {

    private final Cloudinary cloudinary;

    /** Root Cloudinary folder for the active environment, e.g. {@code "cooksync-dev"} locally. */
    @Value("${cloudinary.upload.base-folder}")
    private String baseFolder;

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudinarySignatureResponse generateUploadSignature(String folder, String publicId) {
        long timestamp = System.currentTimeMillis() / 1000;
        String targetFolder = (folder == null || folder.isBlank()) ? baseFolder : folder;

        Map<String, Object> params = new java.util.HashMap<>();
        params.put("timestamp", timestamp);
        params.put("folder", targetFolder);
        if (publicId != null && !publicId.isBlank()) {
            params.put("public_id", publicId);
        }

        String signature = cloudinary.apiSignRequest(params, cloudinary.config.apiSecret, 2);

        return new CloudinarySignatureResponse(
                signature,
                timestamp,
                cloudinary.config.apiKey,
                cloudinary.config.cloudName
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getBaseFolder() {
        return baseFolder;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String buildUserFolder(String userEmail, String subPath) {
        String folder = baseFolder + "/" + userEmail;
        return (subPath == null || subPath.isBlank()) ? folder : folder + "/" + subPath;
    }

    /**
     * Deletes a single image asset from Cloudinary storage by parsing its public ID.
     * Safely catches any exception to ensure Cloudinary communication errors do not break DB transactions.
     *
     * @param imageUrl target image URL string
     */
    @Override
    public void deleteImage(String imageUrl) {
        String publicId = extractPublicId(imageUrl);
        if (publicId == null) {
            return;
        }

        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.info("Successfully requested deletion of Cloudinary image with publicId: {}", publicId);
        } catch (Exception e) {
            log.warn("Failed to delete Cloudinary image with publicId: {} - Error: {}", publicId, e.getMessage());
        }
    }

    /**
     * Deletes multiple image assets from Cloudinary storage. Runs asynchronously (on the
     * {@code @EnableAsync} default executor) so this call returns immediately rather than
     * blocking the caller's database transaction for the duration of the Cloudinary round-trip.
     *
     * @param imageUrls list of target image URL strings
     */
    @Override
    @Async
    public void deleteImages(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return;
        }

        for (String url : imageUrls) {
            deleteImage(url);
        }
    }

    /**
     * Deletes all image assets and the folder itself from Cloudinary storage. Runs
     * asynchronously for the same reason as {@link #deleteImages(List)}.
     *
     * @param folderPath target folder path to delete
     */
    @Override
    @Async
    public void deleteFolder(String folderPath) {
        if (folderPath == null || folderPath.isBlank()) {
            return;
        }
        try {
            cloudinary.api().deleteResourcesByPrefix(folderPath + "/", ObjectUtils.emptyMap());
            cloudinary.api().deleteFolder(folderPath, ObjectUtils.emptyMap());
            log.info("Successfully requested deletion of Cloudinary folder: {}", folderPath);
        } catch (Exception e) {
            log.warn("Failed to delete Cloudinary folder {}: {}", folderPath, e.getMessage());
        }
    }

    /**
     * Extracts the Cloudinary public ID from a full Cloudinary URL.
     * <p>
     * Example input: {@code https://res.cloudinary.com/demo/image/upload/v1570979139/CookSyncApp/sample.jpg}
     * Example output: {@code CookSyncApp/sample}
     *
     * @param imageUrl full URL string
     * @return extracted public ID string, or {@code null} if the URL is not a valid Cloudinary upload URL
     */
    public String extractPublicId(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }

        int uploadIdx = imageUrl.indexOf("/upload/");
        if (uploadIdx == -1) {
            return null;
        }

        String pathAfterUpload = imageUrl.substring(uploadIdx + "/upload/".length());

        if (pathAfterUpload.matches("^v\\d+/.*")) {
            pathAfterUpload = pathAfterUpload.substring(pathAfterUpload.indexOf('/') + 1);
        }

        int lastDotIdx = pathAfterUpload.lastIndexOf('.');
        if (lastDotIdx != -1) {
            pathAfterUpload = pathAfterUpload.substring(0, lastDotIdx);
        }

        return pathAfterUpload.isEmpty() ? null : pathAfterUpload;
    }
}
