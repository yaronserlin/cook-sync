package com.cooksync_server.services;

import java.util.List;

import com.dtos.request.tags.TagRequestDTO;
import com.dtos.response.PagedResponse;
import com.dtos.response.tags.TagResponse;

/**
 * Service interface for recipe tag catalog management and custom tag creation.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
public interface ITagService {

    /**
     * Retrieves a paginated list of all tags in the catalog.
     *
     * @param page page number index
     * @param size page size limit
     * @return PagedResponse of TagResponse DTOs
     */
    PagedResponse<TagResponse> getAllTags(int page, int size);

    /**
     * Retrieves the most-used tags across all recipes, ranked by descending recipe count.
     *
     * @param limit maximum number of popular tags to return
     * @return list of TagResponse DTOs ordered by descending usage
     */
    List<TagResponse> getPopularTags(int limit);

    /**
     * Retrieves a tag by unique ID.
     *
     * @param id target tag ID
     * @return TagResponse DTO
     */
    TagResponse getTagById(String id);

    /**
     * Finds an existing tag by name, or creates a new one if it does not already exist.
     *
     * @param request tag request DTO
     * @return TagResponse DTO
     */
    TagResponse getOrCreateTag(TagRequestDTO request);

    /**
     * Creates a new tag, ensuring uniqueness against existing tag names.
     *
     * @param request tag creation request DTO
     * @return TagResponse DTO of the created tag
     */
    TagResponse createTag(TagRequestDTO request);

    /**
     * Updates an existing tag's name.
     *
     * @param id target tag ID
     * @param request tag update request DTO
     * @return TagResponse DTO of the updated tag
     */
    TagResponse updateTag(String id, TagRequestDTO request);

    /**
     * Deletes a tag by ID.
     *
     * @param id target tag ID
     */
    void deleteTag(String id);
}
