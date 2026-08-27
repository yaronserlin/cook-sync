package com.cooksync_server.entities;

import com.cooksync_server.constants.SchemaConstants;
import java.time.LocalDateTime;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Entity representing a step-by-step cooking instruction.
 * Maps table columns in "instructions" with many-to-many ingredient links.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
@Entity
@Table(name = "instructions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Instruction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = SchemaConstants.UUID_COLUMN_LENGTH)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Recipe recipe;

    @Column(name = "step_number", nullable = false)
    private int stepNumber;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url")
    private String imageUrl;

    @Builder.Default
    @Column(name = "has_timer", nullable = false)
    private boolean hasTimer = false;

    @Column(name = "time_seconds")
    private Integer timeSeconds;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToMany
    @JoinTable(
            name = "instruction_ingredients",
            joinColumns = @JoinColumn(name = "instruction_id"),
            inverseJoinColumns = @JoinColumn(name = "ingredient_id")
    )
    private Set<Ingredient> ingredients;

    /**
     * Lifecycle callback setting creation and update timestamps.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    /**
     * Lifecycle callback refreshing modification timestamp.
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
