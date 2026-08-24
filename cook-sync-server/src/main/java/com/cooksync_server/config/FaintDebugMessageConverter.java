package com.cooksync_server.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.CompositeConverter;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.boot.ansi.AnsiStyle;

/**
 * Dims DEBUG/TRACE log messages (ANSI "faint" style, the terminal equivalent of lowering
 * opacity) so verbose dev-mode output visually recedes behind INFO+ lines, while leaving every
 * other level's message untouched. Registered as the "debugFaint" conversion word in
 * logback-spring.xml, wrapping only {@code %msg} — the level tag itself keeps Spring Boot's
 * standard auto %clr coloring (ERROR red, WARN yellow, INFO/DEBUG green).
 * <p>
 * Mirrors Spring Boot's own {@code ColorConverter} by delegating to
 * {@link AnsiOutput#toString}, so coloring auto-disables the same way the rest of the pattern's
 * %clr tokens do when output isn't a real terminal (redirected to a file/CI).
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 12/08/2026
 */
public class FaintDebugMessageConverter extends CompositeConverter<ILoggingEvent> {

    /**
     * Wraps the already-formatted message in the ANSI "faint" escape sequence when the event is
     * DEBUG level or more verbose, otherwise returns it unchanged.
     *
     * Complexity:
     * Time: O(1)
     * Space: O(1)
     *
     * @param event the logging event being formatted, used only for its level
     * @param in the message text produced by the wrapped {@code %msg} conversion
     * @return the message, faint-styled if the event is DEBUG/TRACE, otherwise unchanged
     */
    @Override
    protected String transform(ILoggingEvent event, String in) {
        boolean isVerbose = event.getLevel().toInt() <= Level.DEBUG_INT;
        return isVerbose ? AnsiOutput.toString(AnsiStyle.FAINT, in) : in;
    }
}
