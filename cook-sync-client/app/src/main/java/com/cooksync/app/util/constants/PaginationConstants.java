package com.cooksync.app.util.constants;

/**
 * Shared page-size constants for server-paginated endpoints, previously declared separately at
 * each call site as identical independently-maintained copies of the same value.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 27/08/2026
 */
public final class PaginationConstants {

    /** Page size used by the general-purpose recipe feed and search screens. */
    public static final int PAGE_SIZE = 10;

    /** Page size used by the Admin Console's paginated tabs (Reports, Users, Tags). */
    public static final int ADMIN_PAGE_SIZE = 20;

    private PaginationConstants() {}
}
