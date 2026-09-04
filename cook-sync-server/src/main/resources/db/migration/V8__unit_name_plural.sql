-- Adds a plural display name for each measurement unit alongside the existing singular `name`
-- (e.g. "Grams" vs "Gram"), so the client can grammatically match whatever quantity it's paired
-- with ("1 Gram" vs "200 Grams") instead of always showing the singular form.
--
-- Backfilled by `code` (a stable, unique business key), not `id` (auto-generated, differs per
-- environment/seed run) — safe to run against any database state: units with an unrecognized
-- code, or none at all yet, are simply skipped by the UPDATE/INSERT...SELECT statements below
-- rather than failing.
--
-- The Hebrew plural equivalent is seeded into content_translations the same way V2 seeded
-- UNIT_NAME's Hebrew singulars, under a new UNIT_NAME_PLURAL entity type
-- (ContentTranslation.EntityType) — also matched by code through a join, for the same reason.

ALTER TABLE units ADD COLUMN name_plural VARCHAR(255) NULL;

UPDATE units SET name_plural = 'Cups' WHERE code = 'cup';
UPDATE units SET name_plural = 'Tablespoons' WHERE code = 'tbsp';
UPDATE units SET name_plural = 'Teaspoons' WHERE code = 'tsp';
UPDATE units SET name_plural = 'Grams' WHERE code = 'g';
UPDATE units SET name_plural = 'Kilograms' WHERE code = 'kg';
UPDATE units SET name_plural = 'Milliliters' WHERE code = 'ml';
UPDATE units SET name_plural = 'Liters' WHERE code = 'l';
UPDATE units SET name_plural = 'Pinches' WHERE code = 'pinch';
UPDATE units SET name_plural = 'Cloves' WHERE code = 'clove';
UPDATE units SET name_plural = 'Pieces' WHERE code = 'piece';
UPDATE units SET name_plural = 'Slices' WHERE code = 'slice';
UPDATE units SET name_plural = 'Cans' WHERE code = 'can';
UPDATE units SET name_plural = 'Packages' WHERE code = 'pkg';
UPDATE units SET name_plural = 'Handfuls' WHERE code = 'handful';
UPDATE units SET name_plural = 'Sprigs' WHERE code = 'sprig';
UPDATE units SET name_plural = 'Bundles' WHERE code = 'bundle';

-- Any unit with a code not covered above (e.g. one created via the admin console between this
-- migration being written and applied) falls back to repeating its own singular name rather than
-- being left NULL, so the NOT NULL constraint below never fails on an unexpected row.
UPDATE units SET name_plural = name WHERE name_plural IS NULL;

ALTER TABLE units MODIFY COLUMN name_plural VARCHAR(255) NOT NULL;

INSERT INTO content_translations (id, entity_type, entity_id, locale, value, source, updated_at)
SELECT UUID(), 'UNIT_NAME_PLURAL', id, 'he', 'כוסות', 'HUMAN', NOW() FROM units WHERE code = 'cup';
INSERT INTO content_translations (id, entity_type, entity_id, locale, value, source, updated_at)
SELECT UUID(), 'UNIT_NAME_PLURAL', id, 'he', 'כפות', 'HUMAN', NOW() FROM units WHERE code = 'tbsp';
INSERT INTO content_translations (id, entity_type, entity_id, locale, value, source, updated_at)
SELECT UUID(), 'UNIT_NAME_PLURAL', id, 'he', 'כפיות', 'HUMAN', NOW() FROM units WHERE code = 'tsp';
INSERT INTO content_translations (id, entity_type, entity_id, locale, value, source, updated_at)
SELECT UUID(), 'UNIT_NAME_PLURAL', id, 'he', 'גרמים', 'HUMAN', NOW() FROM units WHERE code = 'g';
INSERT INTO content_translations (id, entity_type, entity_id, locale, value, source, updated_at)
SELECT UUID(), 'UNIT_NAME_PLURAL', id, 'he', 'קילוגרמים', 'HUMAN', NOW() FROM units WHERE code = 'kg';
INSERT INTO content_translations (id, entity_type, entity_id, locale, value, source, updated_at)
SELECT UUID(), 'UNIT_NAME_PLURAL', id, 'he', 'מיליליטרים', 'HUMAN', NOW() FROM units WHERE code = 'ml';
INSERT INTO content_translations (id, entity_type, entity_id, locale, value, source, updated_at)
SELECT UUID(), 'UNIT_NAME_PLURAL', id, 'he', 'ליטרים', 'HUMAN', NOW() FROM units WHERE code = 'l';
INSERT INTO content_translations (id, entity_type, entity_id, locale, value, source, updated_at)
SELECT UUID(), 'UNIT_NAME_PLURAL', id, 'he', 'קורטים', 'HUMAN', NOW() FROM units WHERE code = 'pinch';
INSERT INTO content_translations (id, entity_type, entity_id, locale, value, source, updated_at)
SELECT UUID(), 'UNIT_NAME_PLURAL', id, 'he', 'שיניים', 'HUMAN', NOW() FROM units WHERE code = 'clove';
INSERT INTO content_translations (id, entity_type, entity_id, locale, value, source, updated_at)
SELECT UUID(), 'UNIT_NAME_PLURAL', id, 'he', 'יחידות', 'HUMAN', NOW() FROM units WHERE code = 'piece';
INSERT INTO content_translations (id, entity_type, entity_id, locale, value, source, updated_at)
SELECT UUID(), 'UNIT_NAME_PLURAL', id, 'he', 'פרוסות', 'HUMAN', NOW() FROM units WHERE code = 'slice';
INSERT INTO content_translations (id, entity_type, entity_id, locale, value, source, updated_at)
SELECT UUID(), 'UNIT_NAME_PLURAL', id, 'he', 'פחיות', 'HUMAN', NOW() FROM units WHERE code = 'can';
INSERT INTO content_translations (id, entity_type, entity_id, locale, value, source, updated_at)
SELECT UUID(), 'UNIT_NAME_PLURAL', id, 'he', 'חבילות', 'HUMAN', NOW() FROM units WHERE code = 'pkg';
INSERT INTO content_translations (id, entity_type, entity_id, locale, value, source, updated_at)
SELECT UUID(), 'UNIT_NAME_PLURAL', id, 'he', 'חופנים', 'HUMAN', NOW() FROM units WHERE code = 'handful';
INSERT INTO content_translations (id, entity_type, entity_id, locale, value, source, updated_at)
SELECT UUID(), 'UNIT_NAME_PLURAL', id, 'he', 'ענפים', 'HUMAN', NOW() FROM units WHERE code = 'sprig';
INSERT INTO content_translations (id, entity_type, entity_id, locale, value, source, updated_at)
SELECT UUID(), 'UNIT_NAME_PLURAL', id, 'he', 'צרורות', 'HUMAN', NOW() FROM units WHERE code = 'bundle';
