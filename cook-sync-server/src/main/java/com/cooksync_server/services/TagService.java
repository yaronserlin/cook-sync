package com.cooksync_server.services;

import java.util.List;

import com.dtos.request.common.PageRequestDTO;
import com.dtos.request.tags.TagRequestDTO;
import com.dtos.response.PagedResponse;
import com.dtos.response.tags.TagResponse;

/**
 * Service interface for recipe tag catalog browsing and custom tag creation.
 *
 * @author Yaron Serlin
 * @version 1.1
 * @since 02/08/2026
 */
public interface TagService {

    /**
     * Retrieves a paginated list of all tags in the catalog.
     *
     * @param request pagination parameters
     * @return PagedResponse of TagResponse DTOs
     */
    PagedResponse<TagResponse> getAllTags(PageRequestDTO request);

    /**
     * Retrieves the most-used tags across all recipes, ranked by descending recipe count.
     *
     * @param limit maximum number of popular tags to return
     * @return list of TagResponse DTOs ordered by descending usage
     */
    List<TagResponse> getPopularTags(int limit);

    /**
     * Finds an existing tag by name, or creates a new one if it does not already exist.
     *
     * @param request tag request DTO
     * @return TagResponse DTO
     */
    TagResponse getOrCreateTag(TagRequestDTO request);
}
