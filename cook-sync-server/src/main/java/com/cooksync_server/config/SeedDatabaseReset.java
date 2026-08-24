package com.cooksync_server.config;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Shared table-truncation routine used by every {@code @Profile}-gated seeder
 * ({@link DataSeeder}, {@link SkillRecipeDataSeeder}) to wipe the schema before
 * reseeding it from scratch.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 24/08/2026
 */
final class SeedDatabaseReset {

    private SeedDatabaseReset() {
    }

    /**
     * Truncates every seedable table, temporarily disabling foreign-key checks so
     * the truncation order does not need to respect referential constraints.
     *
     * Complexity:
     * Time: O(T) where T is table count
     * Space: O(1)
     *
     * @param jdbcTemplate JDBC template used to issue the DDL statements
     */
    static void truncateAllTables(JdbcTemplate jdbcTemplate) {
        String[] tables = {
                "users", "recipes", "units", "ingredients", "instructions",
                "instruction_ingredients", "reviews", "favorite_recipes",
                "personal_instruction_notes", "tags", "recipe_tags", "recipe_images",
                "description_blocks"
        };

        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        for (String table : tables) {
            jdbcTemplate.execute("TRUNCATE TABLE " + table);
        }
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
    }
}
