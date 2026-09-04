CREATE TABLE IF NOT EXISTS system_announcements (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    severity VARCHAR(16) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by VARCHAR(36) NOT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_system_announcements_created_by FOREIGN KEY (created_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS announcement_dismissals (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    announcement_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    dismissed_at DATETIME NOT NULL,
    UNIQUE KEY uq_announcement_dismissals_announcement_user (announcement_id, user_id),
    CONSTRAINT fk_announcement_dismissals_announcement FOREIGN KEY (announcement_id) REFERENCES system_announcements(id) ON DELETE CASCADE,
    CONSTRAINT fk_announcement_dismissals_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
