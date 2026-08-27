package com.cooksync_server.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * JPA Entity representing a single structured content block within a recipe description.
 * Blocks are discriminated by type: TEXT blocks carry prose content, IMAGE blocks carry a URL and optional caption.
 * The sortOrder field preserves the author's intended content sequence.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/08/2026
 */
@Entity
@Table(name = "description_blocks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DescriptionBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BlockType type;

    @Column(columnDefinition = "TEXT")
    private String text;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @Column(length = 500)
    private String caption;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    /**
     * Discriminator enum for description block content type.
     */
    public enum BlockType {
        TEXT, IMAGE
    }
}
