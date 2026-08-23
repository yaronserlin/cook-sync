/**
 * Server-side test-layer component of the Cloudinary image-upload feature. Unit-tests
 * {@code CloudinaryServiceImp} against a real (test-credentialed) {@code Cloudinary} SDK
 * instance, verifying signature generation, per-user folder-path construction, and public-ID
 * extraction from delivery URLs — without making any network call to Cloudinary itself.
 */
package com.cooksync_server.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.dtos.response.cloudinary.CloudinarySignatureResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test suite verifying Cloudinary direct upload signature generation.
 *
 * @author Yaron Serlin
 * @version 1.1
 * @since 09/08/2026
 */
class CloudinaryServiceTest {

    private CloudinaryServiceImp cloudinaryService;

    /**
     * Constructs a {@code CloudinaryServiceImp} against a test-credentialed Cloudinary client
     * and a fixed base folder, so signature generation is deterministic and offline.
     */
    @BeforeEach
    void setUp() {
        Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", "test-cloud-name",
                "api_key", "test-api-key",
                "api_secret", "test-api-secret",
                "secure", true
        ));
        cloudinaryService = new CloudinaryServiceImp(cloudinary);
        ReflectionTestUtils.setField(cloudinaryService, "baseFolder", "CookSyncApp");
    }

    /**
     * Verifies that a generated signature carries a non-blank signature string and echoes back
     * the configured API key, cloud name, and a positive timestamp.
     */
    @Test
    void generateUploadSignature_ShouldReturnValidSignatureResponse() {
        CloudinarySignatureResponse response = cloudinaryService.generateUploadSignature(null, null);

        assertNotNull(response);
        assertNotNull(response.signature());
        assertFalse(response.signature().isBlank());
        assertEquals("test-api-key", response.apiKey());
        assertEquals("test-cloud-name", response.cloudName());
        assertTrue(response.timestamp() > 0);
    }

    /**
     * Verifies that the configured base folder is returned as-is.
     */
    @Test
    void getBaseFolder_ShouldReturnConfiguredValue() {
        assertEquals("CookSyncApp", cloudinaryService.getBaseFolder());
    }

    /**
     * Verifies the {@code baseFolder/userEmail[/subPath]} folder-path format for a blank,
     * {@code null}, and non-blank {@code subPath}.
     */
    @Test
    void buildUserFolder_ShouldNamespaceUnderBaseFolderAndEmail() {
        assertEquals("CookSyncApp/user@example.com", cloudinaryService.buildUserFolder("user@example.com", null));
        assertEquals("CookSyncApp/user@example.com", cloudinaryService.buildUserFolder("user@example.com", ""));
        assertEquals("CookSyncApp/user@example.com/avatar", cloudinaryService.buildUserFolder("user@example.com", "avatar"));
    }

    /**
     * Verifies public-ID extraction from Cloudinary delivery URLs with and without a version
     * segment, and that non-Cloudinary or missing URLs yield {@code null} rather than throwing.
     */
    @Test
    void extractPublicId_ShouldCorrectlyExtractPublicIdFromCloudinaryUrl() {
        String urlWithVersion = "https://res.cloudinary.com/demo/image/upload/v1570979139/CookSyncApp/sample.jpg";
        assertEquals("CookSyncApp/sample", cloudinaryService.extractPublicId(urlWithVersion));

        String urlWithoutVersion = "https://res.cloudinary.com/demo/image/upload/CookSyncApp/avatar.png";
        assertEquals("CookSyncApp/avatar", cloudinaryService.extractPublicId(urlWithoutVersion));

        String nonCloudinaryUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e";
        assertNull(cloudinaryService.extractPublicId(nonCloudinaryUrl));

        assertNull(cloudinaryService.extractPublicId(null));
        assertNull(cloudinaryService.extractPublicId(""));
    }
}
