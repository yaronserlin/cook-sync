package com.cooksync_server.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cooksync_server.entities.DeviceToken;

/**
 * Spring Data JPA Repository interface for DeviceToken entity management.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/09/2026
 */
@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, String> {

    /**
     * Finds an existing registration for the given push token, regardless of which user it
     * currently belongs to — used to upsert on re-registration (e.g. a reinstalled app issuing a
     * fresh FCM token that happens to collide, or the same token being re-sent by the same
     * device) rather than violating the unique constraint on {@code pushToken}.
     *
     * @param pushToken the device's FCM registration token
     * @return the existing registration, if any
     */
    Optional<DeviceToken> findByPushToken(String pushToken);

    /**
     * Retrieves every device token belonging to users who currently have push notifications
     * enabled, for broadcast fan-out. Push is opt-out, not opt-in: a user has no
     * {@code notification_preferences} row at all until they first visit that screen (nothing
     * creates one proactively at registration), so this deliberately excludes only users who
     * explicitly disabled push, rather than requiring a row to exist and be {@code true} — an
     * inner join on that condition would silently exclude every device belonging to a user who
     * simply never opened the preferences screen, which in practice is most users.
     *
     * @return device tokens eligible to receive a broadcast push
     */
    @Query("SELECT dt FROM DeviceToken dt WHERE dt.user.id NOT IN "
            + "(SELECT np.userId FROM NotificationPreferences np WHERE np.pushEnabled = false)")
    List<DeviceToken> findAllEligibleForBroadcast();

    /**
     * Deletes a device's push-token registration, e.g. on logout.
     *
     * @param pushToken the device's FCM registration token
     */
    @Modifying
    @Query("DELETE FROM DeviceToken dt WHERE dt.pushToken = :pushToken")
    void deleteByPushToken(@Param("pushToken") String pushToken);
}
