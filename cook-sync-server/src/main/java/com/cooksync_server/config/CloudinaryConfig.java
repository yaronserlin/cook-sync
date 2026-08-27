package com.cooksync_server.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class initializing the Cloudinary SDK client bean.
 * Injecting environment credentials for media storage and upload signature operations.
 * <p>
 * All three credentials default to blank when unset, rather than failing application startup, so
 * the app (and the "seed" data seeder) still run without a Cloudinary account configured; callers
 * that need real uploads (e.g. {@code DataSeeder}) detect the blank credentials themselves and
 * skip upload attempts accordingly.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
@Configuration
public class CloudinaryConfig {

    /** Cloudinary account's cloud name, e.g. {@code "cooksync"}, from the {@code CLOUDINARY_CLOUD_NAME} env var. */
    @Value("${CLOUDINARY_CLOUD_NAME:}")
    private String cloudName;

    /** Cloudinary account's public API key, from the {@code CLOUDINARY_API_KEY} env var. */
    @Value("${CLOUDINARY_API_KEY:}")
    private String apiKey;

    /** Cloudinary account's private API secret, from the {@code CLOUDINARY_API_SECRET} env var; never sent to the client. */
    @Value("${CLOUDINARY_API_SECRET:}")
    private String apiSecret;

    /**
     * Instantiates and configures the Cloudinary API client bean.
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
