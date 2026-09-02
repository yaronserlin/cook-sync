package com.cooksync_server.entities;

import com.cooksync_server.constants.SchemaConstants;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Entity acting as a generic translation cache/memory for any translatable field on any
 * entity: one row holds a single field's value in a single locale, addressed by
 * ({@link #entityType}, {@link #entityId}, {@link #locale}) rather than a dedicated column per
 * language, so adding a new language never requires a schema change. {@link #source}
 * distinguishes a professionally pre-populated seed translation from one produced on-demand by
 * a {@link com.cooksync_server.translation.TranslationProvider}, which callers surface to the
 * client as an "auto-translated" indicator rather than presenting it with the same confidence
 * as human-reviewed content.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/09/2026
 */
@Entity
@Table(name = "content_translations", uniqueConstraints = {
        @UniqueConstraint(name = "uk_content_translation_target", columnNames = {"entity_type", "entity_id", "locale"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentTranslation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = SchemaConstants.UUID_COLUMN_LENGTH)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 40)
    private EntityType entityType;

    @Column(name = "entity_id", nullable = false, length = SchemaConstants.UUID_COLUMN_LENGTH)
    private String entityId;

    /** IETF language tag the translated {@link #value} is written in, e.g. {@code "he"}. */
    @Column(nullable = false, length = 10)
    private String locale;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String value;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Source source;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void touch() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * The field a translation row belongs to. Naming the field explicitly (rather than one row
     * per whole entity) lets a single recipe carry independently-sourced translations for its
     * title versus its description, ingredients, etc.
     */
    public enum EntityType {
        RECIPE_TITLE, RECIPE_DESCRIPTION, RECIPE_DESCRIPTION_BLOCK, INGREDIENT_NAME, INSTRUCTION_TEXT, UNIT_NAME, TAG_NAME
    }

    /**
     * Provenance of a translation row: {@code HUMAN} for professionally pre-populated seed
     * content (never overwritten by on-demand translation), {@code MACHINE} for a translation
     * produced automatically by a {@link com.cooksync_server.translation.TranslationProvider}.
     */
    public enum Source {
        HUMAN, MACHINE
    }
}
