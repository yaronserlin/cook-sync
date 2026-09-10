package com.cooksync_server.entities;

import java.time.LocalDateTime;

import com.cooksync_server.constants.SchemaConstants;

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
 * JPA Entity acting as a shared translation-memory keyed purely by the source text's content
 * hash and target locale — independent of which entity (ingredient, recipe title, ...) the text
 * came from. This is a read-through/write-through dedup layer in front of
 * {@link com.cooksync_server.translation.TranslationProvider}: identical source strings (e.g. the
 * ingredient name "flour" appearing on many recipes) share one row instead of triggering a
 * separate provider call and {@link ContentTranslation} row per occurrence.
 *
 * <p>Deliberately a separate table from {@link ContentTranslation} rather than overloading that
 * entity's {@code entityId} column with two incompatible meanings (a real entity id vs. a text
 * hash) — {@link ContentTranslation}'s per-entity semantics are unchanged and remain the only
 * thing any mapper reads.</p>
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 10/09/2026
 */
@Entity
@Table(name = "translation_memory", uniqueConstraints = {
        @UniqueConstraint(name = "uk_translation_memory_target", columnNames = {"text_hash", "locale"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TranslationMemory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = SchemaConstants.UUID_COLUMN_LENGTH)
    private String id;

    /** SHA-256 hex digest of the whitespace-normalized source text, see {@code TranslationTextHasher}. */
    @Column(name = "text_hash", nullable = false, length = 64)
    private String textHash;

    /** IETF language tag the translated {@link #value} is written in, e.g. {@code "he"}. */
    @Column(nullable = false, length = 10)
    private String locale;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String value;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContentTranslation.Source source;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void touch() {
        updatedAt = LocalDateTime.now();
    }
}
