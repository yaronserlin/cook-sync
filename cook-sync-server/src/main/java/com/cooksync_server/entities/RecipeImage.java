package com.cooksync_server.entities;

import com.cooksync_server.constants.SchemaConstants;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Entity representing a photo image associated with a recipe.
 * Maps table columns in "recipe_images".
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
@Entity
@Table(name = "recipe_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecipeImage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = SchemaConstants.UUID_COLUMN_LENGTH)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @Column(name = "image_url", nullable = false, length = 2000)
    private String imageUrl;

    @Builder.Default
    @Column(name = "is_primary", nullable = false)
    private boolean isPrimary = false;
}
