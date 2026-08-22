package com.cooksync_server.repositories;

import com.cooksync_server.entities.DescriptionBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA Repository interface for DescriptionBlock entity operations. Description
 * blocks are normally persisted and fetched through their owning {@code Recipe} entity's cascade
 * relationship; this repository exists to satisfy Spring Data JPA's entity-repository convention
 * rather than for direct standalone queries.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
@Repository
public interface DescriptionBlockRepository extends JpaRepository<DescriptionBlock, String> {
}
