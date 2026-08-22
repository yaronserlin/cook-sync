-- Realigns the two OTP-backed tables with the entities as of the OTP
-- email-verification feature.
--
-- Why this exists as a separate migration rather than an edit to V1:
-- V1 was edited in place after it had already been deployed, changing
-- password_reset_tokens (token/used -> code_hash/attempt_count) and adding
-- pending_registrations. Neither change can reach a database that already has
-- these tables, because every statement in V1 is CREATE TABLE IF NOT EXISTS,
-- which no-ops against an existing table instead of altering it. On top of
-- that, spring.flyway.baseline-on-migrate marks pre-Flyway schemas as already
-- at a baseline version, so V1 may never be replayed at all. A forward
-- migration is the only form that reaches every database regardless of
-- whether V1 ran, was skipped, or ran before the edit.
--
-- Both tables hold short-lived OTP state only: password_reset_tokens rows are
-- deleted the moment a reset is consumed and otherwise expire in minutes, and
-- pending_registrations rows expire in 10 minutes and are purged daily by
-- PendingRegistrationCleanupScheduler. Recreating password_reset_tokens
-- outright is therefore preferred over a column-by-column ALTER: it converges
-- the old and new shapes to the same result without needing conditional DDL
-- (TiDB supports neither stored procedures nor MySQL-portable
-- "ADD COLUMN IF NOT EXISTS"), and the only data lost is reset codes that
-- would have expired within minutes anyway.
--
-- Deliberately declared with no column charset/collation and no foreign key to
-- users. The production schema predates Flyway -- it was created by Hibernate
-- ddl-auto=update, so users.id carries whatever width and collation that
-- database defaults to, not the VARCHAR(36)/utf8mb4_unicode_ci this file would
-- otherwise assume. A foreign key requires the referencing and referenced
-- columns to agree on exactly those attributes, so naming them here fails with
-- "foreign key constraint is incorrectly formed" (MySQL errno 150, TiDB error
-- 3780) on precisely the databases this migration exists to repair. Omitting
-- both lets each table adopt its own database's defaults and keeps the
-- migration portable across a Hibernate-built schema and a V1-built one.
--
-- Dropping the constraint costs only its ON DELETE CASCADE, which nothing
-- relies on: AccountDeletionService#purgeAccountImmediately already deletes
-- these rows explicitly via passwordResetTokenRepository.deleteByUserId before
-- removing the user, and Hibernate's ddl-auto=validate checks tables and
-- columns, not foreign keys. The index the constraint used to imply is
-- declared explicitly below so PasswordResetTokenRepository#findByUserId and
-- #deleteByUserId keep their lookup path.

DROP TABLE IF EXISTS password_reset_tokens;

CREATE TABLE password_reset_tokens (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    code_hash VARCHAR(255) NOT NULL,
    expiry_date DATETIME(6) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    INDEX idx_password_reset_tokens_user (user_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS pending_registrations (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    terms_accepted BOOLEAN NOT NULL DEFAULT FALSE,
    marketing_opt_in BOOLEAN NOT NULL DEFAULT FALSE,
    otp_code_hash VARCHAR(255) NOT NULL,
    otp_expires_at DATETIME(6) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0
) ENGINE=InnoDB;
