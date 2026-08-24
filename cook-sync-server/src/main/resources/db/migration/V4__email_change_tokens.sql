-- Adds email_change_tokens, the OTP-backed table for the email-change verification
-- flow: a one-time 6-digit code emailed to the requested NEW address, which must be
-- confirmed before the account's email is actually updated.
--
-- Follows the same conventions as password_reset_tokens (see V2__realign_otp_tables.sql
-- for the full rationale): no column charset/collation pinned and no foreign key to
-- users, since the production schema predates Flyway and users.id may not match the
-- VARCHAR(36)/utf8mb4_unicode_ci this file would otherwise assume. The index below
-- keeps EmailChangeTokenRepository#findByUserId and #deleteByUserId indexed despite
-- the missing foreign key.
--
-- Rows are short-lived: deleted the moment a change is confirmed, and otherwise expire
-- in minutes.

CREATE TABLE email_change_tokens (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    new_email VARCHAR(255) NOT NULL,
    code_hash VARCHAR(255) NOT NULL,
    expiry_date DATETIME(6) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    INDEX idx_email_change_tokens_user (user_id)
) ENGINE=InnoDB;
