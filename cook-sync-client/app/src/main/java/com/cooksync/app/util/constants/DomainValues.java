package com.cooksync.app.util.constants;

/**
 * String constants for domain vocabularies previously hardcoded as raw literals scattered across
 * many call sites (recipe visibility, review report reasons, recipe difficulty), each mirroring
 * an enum name on the server.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 27/08/2026
 */
public final class DomainValues {

    // ── Recipe visibility ─────────────────────────────────────────────

    /** A recipe visible to every user. */
    public static final String VISIBILITY_PUBLIC = "PUBLIC";

    /** A recipe visible only to its author. */
    public static final String VISIBILITY_PRIVATE = "PRIVATE";

    /** Client-side filter value meaning "no visibility filter". */
    public static final String VISIBILITY_ALL = "ALL";

    // ── Review report reasons ─────────────────────────────────────────

    /** Report reason: unsolicited/promotional content. */
    public static final String REPORT_REASON_SPAM = "SPAM";

    /** Report reason: abusive or harassing content. */
    public static final String REPORT_REASON_ABUSE = "ABUSE";

    /** Report reason: content unrelated to the recipe it's attached to. */
    public static final String REPORT_REASON_OFF_TOPIC = "OFF_TOPIC";

    // ── Recipe difficulty ──────────────────────────────────────────────

    /** Difficulty level: easy. */
    public static final String DIFFICULTY_EASY = "EASY";

    /** Difficulty level: medium. */
    public static final String DIFFICULTY_MEDIUM = "MEDIUM";

    /** Difficulty level: hard. */
    public static final String DIFFICULTY_HARD = "HARD";

    private DomainValues() {}
}
