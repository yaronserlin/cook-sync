package com.cooksync_server.repositories;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cooksync_server.entities.TranslationMemory;

/**
 * Spring Data JPA Repository interface for {@link TranslationMemory} lookups — the shared,
 * text-hash-keyed dedup layer in front of {@code TranslationProvider}, independent of
 * {@link ContentTranslationRepository}'s per-entity cache.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 10/09/2026
 */
@Repository
public interface TranslationMemoryRepository extends JpaRepository<TranslationMemory, String> {

    /**
     * Finds a previously translated value for the given source-text hash and target locale,
     * regardless of which entity originally produced that text.
     *
     * @param textHash SHA-256 hex digest of the whitespace-normalized source text
     * @param locale IETF language tag of the desired translation
     * @return the matching row, or empty if this exact text has never been translated into
     *         {@code locale} before
     */
    Optional<TranslationMemory> findByTextHashAndLocale(String textHash, String locale);

    /**
     * Inserts a shared translation-memory row unless another request already cached the same
     * target. The database unique key arbitrates concurrent cache misses atomically, mirroring
     * {@link ContentTranslationRepository#insertIfAbsent}.
     *
     * @return one when inserted, zero when the target already existed
     */
    @Modifying
    @Query(value = """
            INSERT INTO translation_memory
            (id, text_hash, locale, value, source, updated_at)
            VALUES (:id, :textHash, :locale, :value, :source, :updatedAt)
            ON DUPLICATE KEY UPDATE id = id
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("id") String id,
            @Param("textHash") String textHash,
            @Param("locale") String locale,
            @Param("value") String value,
            @Param("source") String source,
            @Param("updatedAt") LocalDateTime updatedAt);
}
