/**
 * Server-side test-layer component of the Cloudinary image-upload feature. Unit-tests
 * {@code CloudinaryServiceImp} against a real (test-credentialed) {@code Cloudinary} SDK
 * instance, verifying signature generation, per-user folder-path construction, and public-ID
 * extraction from delivery URLs — without making any network call to Cloudinary itself.
 */
package com.cooksync_server.services;

import com.cloudinary.Api;
import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.cloudinary.utils.ObjectUtils;
import com.dtos.response.cloudinary.CloudinarySignatureResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test suite verifying Cloudinary direct upload signature generation.
 *
 * @author Yaron Serlin
 * @version 1.1
 * @since 09/08/2026
 */
class CloudinaryServiceTest {

    private CloudinaryServiceImp cloudinaryService;

    /** Mocked Cloudinary client/sub-clients, used to verify delete calls without hitting the network. */
    private Cloudinary mockCloudinary;
    private Uploader mockUploader;
    private Api mockApi;
    private CloudinaryServiceImp mockedCloudinaryService;

    /**
     * Constructs a {@code CloudinaryServiceImp} against a test-credentialed Cloudinary client
     * and a fixed base folder, so signature generation is deterministic and offline. Also
     * constructs a second {@code CloudinaryServiceImp} backed by a fully mocked
     * {@code Cloudinary}/{@code Uploader}/{@code Api}, used by the delete-path tests below to
     * verify Cloudinary SDK interactions without making any real network call.
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

        mockCloudinary = mock(Cloudinary.class);
        mockUploader = mock(Uploader.class);
        mockApi = mock(Api.class);
        mockedCloudinaryService = new CloudinaryServiceImp(mockCloudinary);
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

    /**
     * Verifies that a non-blank {@code folder} argument is signed into the request instead of
     * the configured base folder, by independently recomputing the expected signature from the
     * same timestamp and comparing it against the one the service produced.
     */
    @Test
    void generateUploadSignature_ShouldSignGivenFolder_WhenFolderIsNonBlank() {
        Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", "test-cloud-name",
                "api_key", "test-api-key",
                "api_secret", "test-api-secret",
                "secure", true
        ));
        CloudinaryServiceImp service = new CloudinaryServiceImp(cloudinary);
        ReflectionTestUtils.setField(service, "baseFolder", "CookSyncApp");

        CloudinarySignatureResponse response = service.generateUploadSignature("CustomFolder", null);

        Map<String, Object> expectedParams = new HashMap<>();
        expectedParams.put("timestamp", response.timestamp());
        expectedParams.put("folder", "CustomFolder");
        String expectedSignature = cloudinary.apiSignRequest(expectedParams, "test-api-secret", 2);

        assertEquals(expectedSignature, response.signature());
    }

    /**
     * Verifies that a non-blank {@code publicId} argument is folded into the signed request
     * params, by independently recomputing the expected signature from the same timestamp and
     * comparing it against the one the service produced.
     */
    @Test
    void generateUploadSignature_ShouldSignGivenPublicId_WhenPublicIdIsNonBlank() {
        Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", "test-cloud-name",
                "api_key", "test-api-key",
                "api_secret", "test-api-secret",
                "secure", true
        ));
        CloudinaryServiceImp service = new CloudinaryServiceImp(cloudinary);
        ReflectionTestUtils.setField(service, "baseFolder", "CookSyncApp");

        CloudinarySignatureResponse response = service.generateUploadSignature(null, "custom-public-id");

        Map<String, Object> expectedParams = new HashMap<>();
        expectedParams.put("timestamp", response.timestamp());
        expectedParams.put("folder", "CookSyncApp");
        expectedParams.put("public_id", "custom-public-id");
        String expectedSignature = cloudinary.apiSignRequest(expectedParams, "test-api-secret", 2);

        assertEquals(expectedSignature, response.signature());
    }

    /**
     * Verifies that {@code deleteImages} does nothing and never touches the Cloudinary client
     * when given a {@code null} or empty URL list.
     */
    @Test
    void deleteImages_ShouldDoNothing_WhenUrlListIsNullOrEmpty() {
        mockedCloudinaryService.deleteImages(null);
        mockedCloudinaryService.deleteImages(List.of());

        verify(mockCloudinary, never()).uploader();
    }

    /**
     * Verifies that {@code deleteImages} requests deletion of every image in the list by
     * delegating to the Cloudinary uploader's {@code destroy} call for each extracted public ID.
     */
    @Test
    void deleteImages_ShouldDeleteEachImage_WhenUrlsAreProvided() throws Exception {
        when(mockCloudinary.uploader()).thenReturn(mockUploader);

        mockedCloudinaryService.deleteImages(List.of(
                "https://res.cloudinary.com/demo/image/upload/v12345/CookSyncApp/one.jpg",
                "https://res.cloudinary.com/demo/image/upload/v12345/CookSyncApp/two.jpg"
        ));

        verify(mockUploader).destroy(eq("CookSyncApp/one"), anyMap());
        verify(mockUploader).destroy(eq("CookSyncApp/two"), anyMap());
    }

    /**
     * Verifies that {@code deleteFolder} does nothing and never touches the Cloudinary client
     * when given a {@code null} or blank folder path.
     */
    @Test
    void deleteFolder_ShouldDoNothing_WhenFolderPathIsNullOrBlank() {
        mockedCloudinaryService.deleteFolder(null);
        mockedCloudinaryService.deleteFolder("");
        mockedCloudinaryService.deleteFolder("   ");

        verify(mockCloudinary, never()).api();
    }

    /**
     * Verifies that {@code deleteFolder} first purges every resource under the folder prefix and
     * then deletes the now-empty folder itself.
     */
    @Test
    void deleteFolder_ShouldDeleteResourcesAndFolder_WhenFolderPathIsProvided() throws Exception {
        when(mockCloudinary.api()).thenReturn(mockApi);

        mockedCloudinaryService.deleteFolder("CookSyncApp/user@example.com");

        verify(mockApi).deleteResourcesByPrefix(eq("CookSyncApp/user@example.com/"), anyMap());
        verify(mockApi).deleteFolder(eq("CookSyncApp/user@example.com"), anyMap());
    }

    /**
     * Verifies that {@code deleteImage} returns early without contacting Cloudinary when the URL
     * cannot be parsed into a public ID (the {@code extractPublicId(...) == null} branch).
     */
    @Test
    void deleteImage_ShouldDoNothing_WhenUrlIsUnparseable() {
        mockedCloudinaryService.deleteImage("https://images.unsplash.com/photo-1500648767791-00dcc994a43e");

        verify(mockCloudinary, never()).uploader();
    }

    /**
     * Verifies that {@code deleteImage} requests deletion via the Cloudinary uploader when given
     * a valid, parseable Cloudinary URL.
     */
    @Test
    void deleteImage_ShouldRequestCloudinaryDestroy_WhenUrlIsValid() throws Exception {
        when(mockCloudinary.uploader()).thenReturn(mockUploader);

        mockedCloudinaryService.deleteImage("https://res.cloudinary.com/demo/image/upload/v12345/CookSyncApp/avatar.jpg");

        verify(mockUploader).destroy(eq("CookSyncApp/avatar"), anyMap());
    }
}
