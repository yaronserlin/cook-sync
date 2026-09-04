package com.cooksync_server.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cooksync_server.entities.AppVersionConfig;

/**
 * Spring Data JPA Repository interface for AppVersionConfig entity management. The entity's ID
 * is the platform name itself, so the inherited {@code findById}/{@code existsById} already
 * serve as "find/exists by platform" without a custom query method.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/09/2026
 */
@Repository
public interface AppVersionConfigRepository extends JpaRepository<AppVersionConfig, String> {
}
