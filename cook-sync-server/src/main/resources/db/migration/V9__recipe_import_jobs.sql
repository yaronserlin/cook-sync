-- Smart recipe import: a user pastes a recipe-page URL or submits a photo, an LLM extracts
-- structured recipe data, and the result lands as a private draft the user reviews/edits (in the
-- existing wizard) before ever becoming visible. This table tracks that async job's lifecycle
-- independently of `recipes`, since a job can fail before any recipe exists to attach an error to.
--
-- result_recipe_id points at a real, already-created Recipe (PRIVATE visibility, forced
-- regardless of what the extraction produced) once extraction succeeds — the "draft" the user
-- reviews is that actual recipe, edited through the same wizard/publish flow as any other recipe,
-- not a separate not-yet-a-recipe payload.
CREATE TABLE recipe_import_jobs (
    id                VARCHAR(36) NOT NULL PRIMARY KEY,
    user_id           VARCHAR(36) NOT NULL,
    source_type       VARCHAR(16) NOT NULL,
    source_url        VARCHAR(2048) NULL,
    status            VARCHAR(16) NOT NULL,
    error_message     VARCHAR(1024) NULL,
    result_recipe_id  VARCHAR(36) NULL,
    created_at        DATETIME NOT NULL,
    updated_at        DATETIME NOT NULL,
    CONSTRAINT fk_recipe_import_jobs_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_recipe_import_jobs_recipe FOREIGN KEY (result_recipe_id) REFERENCES recipes(id) ON DELETE SET NULL,
    INDEX idx_recipe_import_jobs_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- A PHOTO import can span more than one photo (e.g. successive pages of a handwritten recipe, or
-- an ingredients list and a method written on separate cards) — each row is one source image, in
-- the order the user added them.
CREATE TABLE recipe_import_job_images (
    id         VARCHAR(36) NOT NULL PRIMARY KEY,
    job_id     VARCHAR(36) NOT NULL,
    image_url  VARCHAR(2048) NOT NULL,
    sort_order INT NOT NULL,
    CONSTRAINT fk_recipe_import_job_images_job FOREIGN KEY (job_id) REFERENCES recipe_import_jobs(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Shown as a small credit link on the recipe detail screen when a recipe originated from an
-- import. Both nullable: a manually-created recipe (the vast majority) leaves them unset.
ALTER TABLE recipes
    ADD COLUMN source_attribution_url  VARCHAR(2048) NULL,
    ADD COLUMN source_attribution_note VARCHAR(255) NULL;
