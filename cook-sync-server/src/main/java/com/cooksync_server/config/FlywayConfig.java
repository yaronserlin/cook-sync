package com.cooksync_server.config;

import java.util.Arrays;

import org.flywaydb.core.api.MigrationState;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

/**
 * Configuration class supplying the Flyway migration strategy, which clears a previously
 * failed migration from the schema history before migrating.
 *
 * MySQL and TiDB do not support transactional DDL, so a migration that fails partway leaves
 * its already-executed statements applied and its schema history row marked unsuccessful.
 * Flyway then refuses every subsequent run with "Detected failed migration to version N"
 * until someone repairs the history, which normally means reaching the database with the
 * Flyway CLI. A managed TiDB instance behind a container platform offers no such access, so
 * without this the deployment is stuck permanently and the service stays down.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 16/08/2026
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
public class FlywayConfig {

    /**
     * Builds the migration strategy run at startup in place of a plain migrate.
     *
     * Repair is deliberately conditional rather than unconditional: besides clearing failed
     * rows it also realigns recorded checksums to whatever the migration files currently say,
     * which would silently accept a migration edited in place after deployment - the very
     * mistake that made this repair necessary. Restricting it to the case where a failed
     * migration is actually present keeps that drift detection intact on every healthy boot.
     *
     * @return strategy repairing the schema history when needed, then migrating
     */
    @Bean
    public FlywayMigrationStrategy repairFailedThenMigrate() {
        return flyway -> {
            boolean failedMigrationPresent = Arrays.stream(flyway.info().all())
                    .anyMatch(info -> info.getState() == MigrationState.FAILED);

            if (failedMigrationPresent) {
                log.warn("Schema history contains a failed migration; repairing it before migrating");
                flyway.repair();
            }

            flyway.migrate();
        };
    }
}
