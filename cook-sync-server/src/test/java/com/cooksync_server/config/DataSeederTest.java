package com.cooksync_server.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test suite for {@link DataSeeder}'s Cloudinary dev-folder safety guard — the check that
 * stops the seed-profile-only {@code clearDatabase()} from ever wiping a Cloudinary folder other
 * than the local dev one, even if the "dev" Spring profile is missing/misconfigured and
 * {@code cloudinary.upload.base-folder} falls back to production's value. Targets
 * {@link DataSeeder#isDeletableCloudinaryFolder(String)} directly rather than the full seeder
 * (which would need its entire repository/service dependency graph mocked) since that static
 * predicate is the one piece of logic this guard's safety actually rests on.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 10/09/2026
 */
class DataSeederTest {

    @Test
    void isDeletableCloudinaryFolder_trueOnlyForTheDevFolder() {
        assertTrue(DataSeeder.isDeletableCloudinaryFolder("cooksync-dev"));
    }

    @Test
    void isDeletableCloudinaryFolder_falseForTheProductionFolder() {
        assertFalse(DataSeeder.isDeletableCloudinaryFolder("cooksync-prod"));
    }

    @Test
    void isDeletableCloudinaryFolder_falseForNull() {
        assertFalse(DataSeeder.isDeletableCloudinaryFolder(null));
    }

    @Test
    void isDeletableCloudinaryFolder_falseForAnyOtherOrSimilarLookingValue() {
        assertFalse(DataSeeder.isDeletableCloudinaryFolder(""));
        assertFalse(DataSeeder.isDeletableCloudinaryFolder("cooksync-dev "));
        assertFalse(DataSeeder.isDeletableCloudinaryFolder("COOKSYNC-DEV"));
        assertFalse(DataSeeder.isDeletableCloudinaryFolder("something-else"));
    }
}
