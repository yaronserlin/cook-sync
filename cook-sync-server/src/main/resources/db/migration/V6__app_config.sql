CREATE TABLE IF NOT EXISTS app_config (
    platform                    VARCHAR(16) NOT NULL PRIMARY KEY,
    min_supported_version_code  INT NOT NULL DEFAULT 1,
    download_url                VARCHAR(2048) NULL,
    updated_by                  VARCHAR(36) NULL,
    updated_at                  DATETIME NOT NULL,
    CONSTRAINT fk_app_config_updated_by FOREIGN KEY (updated_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seeded at 1 (the current versionCode) so the gate is a no-op until an admin actually raises it.
INSERT INTO app_config (platform, min_supported_version_code, updated_at)
VALUES ('ANDROID', 1, UTC_TIMESTAMP());
