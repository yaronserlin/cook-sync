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
 * One source photo of a PHOTO {@link RecipeImportJob}, in the order the user added it — a job can
 * span more than one photo (e.g. successive pages of a handwritten recipe), mirroring how {@link
 * DescriptionBlock} orders a recipe's own content blocks.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 05/09/2026
 */
@Entity
@Table(name = "recipe_import_job_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecipeImportJobImage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = SchemaConstants.UUID_COLUMN_LENGTH)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private RecipeImportJob job;

    @Column(name = "image_url", nullable = false, length = 2048)
    private String imageUrl;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
