package com.cooksync_server.repositories;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cooksync_server.entities.ContentTranslation;

/**
 * Spring Data JPA Repository interface for {@link ContentTranslation} lookups.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/09/2026
 */
@Repository
public interface ContentTranslationRepository extends JpaRepository<ContentTranslation, String> {

    /**
     * Finds the cached translation for one field of one entity in one locale,
     * if any.
     *
     * @param entityType the translatable field being resolved
     * @param entityId the id of the entity that field belongs to
     * @param locale IETF language tag of the desired translation
     * @return the matching translation row, or empty if none is cached yet
     */
    Optional<ContentTranslation> findByEntityTypeAndEntityIdAndLocale(
            ContentTranslation.EntityType entityType, String entityId, String locale);

    /**
     * Inserts a machine translation unless another request already cached the
     * same target. The database unique key arbitrates concurrent cache misses
     * atomically.
     *
     * @return one when inserted, zero when the target already existed
     */
    @Modifying
    @Query(value = """
            INSERT INTO content_translations
            (id, entity_type, entity_id, locale, value, source, updated_at)
            VALUES (:id, :entityType, :entityId, :locale, :value, :source, :updatedAt)
            ON DUPLICATE KEY UPDATE id = id
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("id") String id,
            @Param("entityType") String entityType,
            @Param("entityId") String entityId,
            @Param("locale") String locale,
            @Param("value") String value,
            @Param("source") String source,
            @Param("updatedAt") LocalDateTime updatedAt);

    /**
     * Deletes every cached translation (in any locale) for one field of one entity, so a stale
     * or orphaned translation is never served after the source text changes or the entity itself
     * is replaced.
     *
     * @param entityType the translatable field
     * @param entityId id of the entity that field belongs to
     * @return the number of rows deleted
     */
    @Modifying
    @Query("DELETE FROM ContentTranslation ct WHERE ct.entityType = :entityType AND ct.entityId = :entityId")
    long deleteByEntityTypeAndEntityId(
            @Param("entityType") ContentTranslation.EntityType entityType, @Param("entityId") String entityId);

    /**
     * Bulk variant of {@link #deleteByEntityTypeAndEntityId} for the common case of invalidating
     * many entities of the same field type at once (e.g. every ingredient replaced by a recipe
     * edit) in a single statement, rather than the per-entity SELECT-then-remove a derived
     * {@code deleteBy...} method (with no {@code @Query}) would otherwise issue.
     *
     * @param entityType the translatable field
     * @param entityIds ids of the entities that field belongs to
     * @return the number of rows deleted
     */
    @Modifying
    @Query("DELETE FROM ContentTranslation ct WHERE ct.entityType = :entityType AND ct.entityId IN :entityIds")
    long deleteByEntityTypeAndEntityIdIn(
            @Param("entityType") ContentTranslation.EntityType entityType, @Param("entityIds") Collection<String> entityIds);
}
