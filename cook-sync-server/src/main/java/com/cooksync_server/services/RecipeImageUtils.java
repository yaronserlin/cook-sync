package com.cooksync_server.services;

import java.util.ArrayList;
import java.util.List;

import com.cooksync_server.entities.DescriptionBlock;
import com.cooksync_server.entities.Instruction;
import com.cooksync_server.entities.Recipe;
import com.cooksync_server.entities.RecipeImage;

/**
 * Utility class collecting every Cloudinary-hosted image URL referenced by a recipe entity
 * (primary/gallery images, instruction step images, and description block images). Shared by
 * {@link RecipeServiceImp} (update/delete) and {@link AccountDeletionServiceImp} (account purge), both
 * of which need the same URL set to clean up Cloudinary media.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 11/08/2026
 */
final class RecipeImageUtils {

    private RecipeImageUtils() {
    }

    /**
     * Collects all Cloudinary image URLs associated with a given recipe entity (primary/gallery
     * images, instruction step images, and description block images).
     *
     * @param recipe target recipe entity
     * @return list of image URL strings
     */
    static List<String> extractAllImageUrls(Recipe recipe) {
        List<String> urls = new ArrayList<>();
        if (recipe.getImages() != null) {
            for (RecipeImage img : recipe.getImages()) {
                if (img.getImageUrl() != null && !img.getImageUrl().isBlank()) {
                    urls.add(img.getImageUrl());
                }
            }
        }
        if (recipe.getInstructions() != null) {
            for (Instruction inst : recipe.getInstructions()) {
                if (inst.getImageUrl() != null && !inst.getImageUrl().isBlank()) {
                    urls.add(inst.getImageUrl());
                }
            }
        }
        if (recipe.getDescriptionBlocks() != null) {
            for (DescriptionBlock block : recipe.getDescriptionBlocks()) {
                if (block.getImageUrl() != null && !block.getImageUrl().isBlank()) {
                    urls.add(block.getImageUrl());
                }
            }
        }
        return urls;
    }
}
