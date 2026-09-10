CREATE TABLE IF NOT EXISTS translation_memory (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    text_hash VARCHAR(64) NOT NULL,
    locale VARCHAR(10) NOT NULL,
    value TEXT NOT NULL,
    source VARCHAR(20) NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT uk_translation_memory_target UNIQUE (
        text_hash,
        locale
    )
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
