package com.cooksync_server.constants;

/**
 * Centralizes JPA schema-mapping constants shared across entity classes.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 27/08/2026
 */
public final class SchemaConstants {

    private SchemaConstants() {
    }

    /** Column length for UUID-backed identifier and foreign-key columns stored as {@code CHAR(36)}. */
    public static final int UUID_COLUMN_LENGTH = 36;
}
