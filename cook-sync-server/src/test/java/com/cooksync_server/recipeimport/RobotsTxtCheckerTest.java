package com.cooksync_server.recipeimport;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit test suite for {@link RobotsTxtChecker}'s pure {@code User-agent: *} / {@code Disallow}
 * parsing logic. Does not exercise {@link RobotsTxtChecker#isAllowed}, which fetches a real URL.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 05/09/2026
 */
class RobotsTxtCheckerTest {

    @Test
    void isDisallowed_returnsTrue_forExactDisallowedPath() {
        String robotsTxt = """
                User-agent: *
                Disallow: /private/
                """;

        assertTrue(RobotsTxtChecker.isDisallowed(robotsTxt, "/private/secret-recipe"));
    }

    @Test
    void isDisallowed_returnsFalse_forUnlistedPath() {
        String robotsTxt = """
                User-agent: *
                Disallow: /private/
                """;

        assertFalse(RobotsTxtChecker.isDisallowed(robotsTxt, "/recipes/lemon-chicken"));
    }

    @Test
    void isDisallowed_ignoresRulesUnderOtherUserAgents() {
        // A rule scoped to a specific bot (e.g. one this app doesn't identify as) must not apply
        // to us - only a "*" (applies to everyone) block should.
        String robotsTxt = """
                User-agent: GPTBot
                Disallow: /

                User-agent: *
                Disallow: /admin/
                """;

        assertFalse(RobotsTxtChecker.isDisallowed(robotsTxt, "/recipes/lemon-chicken"));
        assertTrue(RobotsTxtChecker.isDisallowed(robotsTxt, "/admin/dashboard"));
    }

    @Test
    void isDisallowed_returnsFalse_whenRobotsTxtIsEmpty() {
        assertFalse(RobotsTxtChecker.isDisallowed("", "/anything"));
    }

    @Test
    void isDisallowed_ignoresComments() {
        String robotsTxt = """
                # Block everything under /private/
                User-agent: *
                Disallow: /private/ # internal only
                """;

        assertTrue(RobotsTxtChecker.isDisallowed(robotsTxt, "/private/notes"));
    }

    @Test
    void isDisallowed_returnsFalse_whenNoWildcardBlockExists() {
        String robotsTxt = """
                User-agent: SomeOtherBot
                Disallow: /
                """;

        assertFalse(RobotsTxtChecker.isDisallowed(robotsTxt, "/anything"));
    }
}
