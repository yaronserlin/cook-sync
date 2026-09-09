package com.cooksync.app.data.datasource.remote;

import com.dtos.response.ApiResponse;
import com.dtos.response.cloudinary.CloudinarySignatureResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Retrofit contract for Cloudinary media-upload support endpoints.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/08/2026
 */
public interface MediaApiService {

    /**
     * Fetches a short-lived signed payload the client uses to upload media directly to
     * Cloudinary, bypassing the application server for the binary transfer itself.
     *
     * @return call yielding Cloudinary upload credentials
     */
    @GET("api/cloudinary/signature")
    Call<ApiResponse<CloudinarySignatureResponse>> getMediaSignature(
            @Query("folder") String folder,
            @Query("publicId") String publicId
    );

    /**
     * Fetches the environment-specific root Cloudinary folder (e.g. {@code "cooksync-dev"}
     * locally, {@code "CookSyncApp"} in production) so upload folder paths can be built
     * without hardcoding an environment-specific value on the client.
     *
     * @return call yielding the configured base folder name
     */
    @GET("api/cloudinary/base-folder")
    Call<ApiResponse<String>> getCloudinaryBaseFolder();
}
