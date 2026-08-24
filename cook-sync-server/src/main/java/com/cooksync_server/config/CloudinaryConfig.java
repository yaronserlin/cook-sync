package com.cooksync_server.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class initializing the Cloudinary SDK client bean.
 * Injecting environment credentials for media storage and upload signature operations.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
@Configuration
public class CloudinaryConfig {

    /** Cloudinary account's cloud name, e.g. {@code "cooksync"}, from the {@code CLOUDINARY_CLOUD_NAME} env var. */
    @Value("${CLOUDINARY_CLOUD_NAME}")
    private String cloudName;

    /** Cloudinary account's public API key, from the {@code CLOUDINARY_API_KEY} env var. */
    @Value("${CLOUDINARY_API_KEY}")
    private String apiKey;

    /** Cloudinary account's private API secret, from the {@code CLOUDINARY_API_SECRET} env var; never sent to the client. */
    @Value("${CLOUDINARY_API_SECRET}")
    private String apiSecret;

    /**
     * Instantiates and configures the Cloudinary API client bean.
     *
     * Complexity:
     * Time: O(1)
     * Space: O(1)
     *
     * @return configured Cloudinary client instance
     */
    @Bean
    public Cloudinary cloudinary() {
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));
    }
}
