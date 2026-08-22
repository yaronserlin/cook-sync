-- Recipe module cleanup, from the recipe-module code review:
--
-- 1. recipe_images.created_at/updated_at are populated by RecipeImage's
--    @PrePersist/@PreUpdate callbacks but were never read by any mapper, query,
--    or DTO (RecipeMapper only ever calls getImageUrl()/isPrimary()). The
--    entity no longer declares these fields, so the columns are dropped here
--    to match.
--
-- 2. reviews.title was nullable at the column level despite ReviewRequestDTO
--    requiring it via @NotBlank, unlike every sibling entity/DTO pair in this
--    module (Recipe.title, Ingredient.name, Instruction.description, Tag.name
--    all enforce their mandatory string columns with nullable = false). Any
--    pre-existing row that predates the DTO validation is backfilled with a
--    placeholder before the column is tightened, since MODIFY COLUMN ... NOT
--    NULL fails outright if a NULL value is still present.

UPDATE reviews SET title = '(untitled review)' WHERE title IS NULL;

ALTER TABLE reviews MODIFY COLUMN title VARCHAR(255) NOT NULL;

ALTER TABLE recipe_images DROP COLUMN created_at;
ALTER TABLE recipe_images DROP COLUMN updated_at;
