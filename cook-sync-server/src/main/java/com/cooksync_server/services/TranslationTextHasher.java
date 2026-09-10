package com.cooksync_server.services;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Computes the {@link com.cooksync_server.entities.TranslationMemory} lookup key for a piece of
 * source text, so {@link TranslationService} (read) and {@link TranslationCacheWriter} (write)
 * never drift on how a string maps to its shared-memory row.
 *
 * <p>Normalization is whitespace-only (trimmed, internal runs collapsed to a single space) and
 * deliberately case-sensitive: two differently-cased strings are treated as distinct text rather
 * than assumed to share a translation, since casing sometimes carries meaning (e.g. a proper
 * noun) that a shared cache should not silently discard.</p>
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 10/09/2026
 */
final class TranslationTextHasher {

    private TranslationTextHasher() {
    }

    /**
     * Hashes {@code text} into its {@code TranslationMemory} lookup key.
     *
     * @param text the source text to hash, after whitespace normalization
     * @return the SHA-256 digest of the normalized text, as lowercase hex
     */
    static String hash(String text) {
        String normalized = text.strip().replaceAll("\\s+", " ");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(normalized.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is guaranteed to be available on every JDK platform", e);
        }
    }
}
