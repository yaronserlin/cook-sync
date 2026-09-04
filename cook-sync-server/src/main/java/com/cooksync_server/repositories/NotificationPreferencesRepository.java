package com.cooksync_server.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cooksync_server.entities.NotificationPreferences;

/**
 * Spring Data JPA Repository interface for NotificationPreferences entity management. The
 * entity's ID is the owning user's own ID (see {@link NotificationPreferences}), so this
 * repository's inherited {@code findById}/{@code existsById} already serve as
 * "find/exists by user ID" without a custom query method.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/09/2026
 */
@Repository
public interface NotificationPreferencesRepository extends JpaRepository<NotificationPreferences, String> {
}
