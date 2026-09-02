package com.cooksync_server.repositories;

import com.cooksync_server.entities.ContentTranslation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

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
     * Finds the cached translation for one field of one entity in one locale, if any.
     *
     * @param entityType the translatable field being resolved
     * @param entityId the id of the entity that field belongs to
     * @param locale IETF language tag of the desired translation
     * @return the matching translation row, or empty if none is cached yet
     */
    Optional<ContentTranslation> findByEntityTypeAndEntityIdAndLocale(
            ContentTranslation.EntityType entityType, String entityId, String locale);
}
