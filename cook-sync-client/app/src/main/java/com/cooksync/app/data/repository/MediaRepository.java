package com.cooksync.app.data.repository;

import androidx.lifecycle.MutableLiveData;

import com.cooksync.app.domain.ApiResult;
import com.dtos.response.cloudinary.CloudinarySignatureResponse;

/**
 * Declares the contract for fetching signed Cloudinary upload credentials, implemented against
 * the server's Cloudinary-signing endpoint and consumed by any screen that uploads an image
 * directly from the client — the account-details avatar picker today, recipe photos in future.
 * Actual upload signature verification, and knowledge of the Cloudinary API secret, stay entirely
 * server-side; the client only ever receives a short-lived signed payload.
 *
 * @author Yaron Serlin
 * @version 1.1
 * @since 04/08/2026
 */
public interface MediaRepository {

    /**
     * Fetches a fresh signed upload signature from the server for a specific folder and public
     * ID.
     *
     * @param folder target folder path
     * @param publicId target asset public ID
     * @param resultTarget live data target the result will be posted to
     */
    void getUploadSignature(String folder, String publicId, MutableLiveData<ApiResult<CloudinarySignatureResponse>> resultTarget);

    /**
     * Fetches the environment-specific root Cloudinary folder from the server, used to build
     * upload folder paths without hardcoding an environment-specific value on the client.
     *
     * @param resultTarget live data target the result will be posted to
     */
    void getBaseFolder(MutableLiveData<ApiResult<String>> resultTarget);
}
