package com.cooksync_server.controllers;

import com.cooksync_server.services.CloudinaryService;
import com.dtos.response.ApiResponse;
import com.dtos.response.cloudinary.CloudinarySignatureResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller providing Cloudinary signed authorization details to authenticated client apps.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
@RestController
@RequestMapping("/api/cloudinary")
@RequiredArgsConstructor
public class CloudinaryController {

    private final CloudinaryService cloudinaryService;

    /**
     * Generates a signed upload signature payload for client-side direct media uploads.
     *
     * Complexity:
     * Time: O(1)
     * Space: O(1)
     *
     * @param folder target folder path the client intends to upload into, or {@code null} to
     *               use the server-configured default
     * @param publicId target asset public ID the client intends to upload as, or {@code null}
     *                 to let Cloudinary auto-generate one
     * @return response entity containing CloudinarySignatureResponse payload
     */
    @GetMapping("/signature")
    public ResponseEntity<ApiResponse<CloudinarySignatureResponse>> getSignature(
            @RequestParam(required = false) String folder,
            @RequestParam(required = false) String publicId) {
        CloudinarySignatureResponse response = cloudinaryService.generateUploadSignature(folder, publicId);
        return ResponseEntity.ok(new ApiResponse<>(true, response, null, "Cloudinary signature generated"));
    }

    /**
     * Returns the environment-specific root Cloudinary folder (e.g. {@code "cooksync-dev"}
     * locally, {@code "CookSyncApp"} in production), so client apps can build upload folder
     * paths without hardcoding an environment-specific value.
     *
     * Complexity:
     * Time: O(1)
     * Space: O(1)
     *
     * @return response entity containing the configured base folder name
     */
    @GetMapping("/base-folder")
    public ResponseEntity<ApiResponse<String>> getBaseFolder() {
        String baseFolder = cloudinaryService.getBaseFolder();
        return ResponseEntity.ok(new ApiResponse<>(true, baseFolder, null, "Cloudinary base folder resolved"));
    }
}
