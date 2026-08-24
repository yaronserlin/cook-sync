package com.cooksync_server.repositories;

import com.cooksync_server.entities.Unit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA Repository interface for Unit entity operations.
 *
 * @author Yaron Serlin
 * @version 1.1
 * @since 02/08/2026
 */
@Repository
public interface UnitRepository extends JpaRepository<Unit, String> {

    /**
     * Checks whether a unit exists with the given name, ignoring case.
     *
     * @param name unit display name
     * @return true if a matching unit exists
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * Checks whether a unit exists with the given code, ignoring case.
     *
     * @param code unit symbol code
     * @return true if a matching unit exists
     */
    boolean existsByCodeIgnoreCase(String code);

    /**
     * Retrieves all measurement units with pagination.
     *
     * @param pageable pagination parameters
     * @return page of Unit entities
     */
    org.springframework.data.domain.Page<Unit> findAll(org.springframework.data.domain.Pageable pageable);
}