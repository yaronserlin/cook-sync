package com.cooksync_server.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.dtos.response.PagedResponse;

import com.cooksync_server.constants.PaginationDefaults;
import com.cooksync_server.services.TagService;
import com.dtos.request.tags.TagRequestDTO;
import com.dtos.response.ApiResponse;
import com.dtos.response.tags.TagResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST Controller managing the public recipe tag catalog: browsing, popularity ranking, and
 * on-the-fly custom-tag creation during recipe editing. All endpoints here are public;
 * duplicate-tag detection and merging is an admin-only concern handled by
 * {@link AdminController} instead.
 *
 * @author Yaron Serlin
 * @version 1.1
 * @since 02/08/2026
 */
@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagsController {

    private final TagService tagService;

    /**
     * Retrieves all recipe tags available in the catalog.
     *
     * @param page page number index
     * @param size page size limit
     * @return response entity containing list of TagResponse DTOs
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<TagResponse>>> getAllTags(
            @RequestParam(defaultValue = PaginationDefaults.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = PaginationDefaults.DEFAULT_PAGE_SIZE) int size) {
        PagedResponse<TagResponse> tags = tagService.getAllTags(page, size);
        return ResponseEntity.ok(new ApiResponse<>(true, tags, null, "All tags retrieved successfully"));
    }

    /**
     * Retrieves the most-used tags across all recipes, ranked by descending recipe count. Only
     * tags actually attached to at least one recipe are included.
     *
     * @param limit maximum number of popular tags to return
     * @return response entity containing list of TagResponse DTOs ordered by descending usage
     */
    @GetMapping("/popular")
    public ResponseEntity<ApiResponse<List<TagResponse>>> getPopularTags(
            @RequestParam(defaultValue = PaginationDefaults.POPULAR_TAGS_LIMIT) int limit) {
        List<TagResponse> tags = tagService.getPopularTags(limit);
        return ResponseEntity.ok(new ApiResponse<>(true, tags, null, "Popular tags retrieved successfully"));
    }

    /**
     * Creates or retrieves an existing custom tag on-the-fly during recipe editing.
     *
     * @param request tag request DTO
     * @return response entity containing TagResponse DTO
     */
    @PostMapping("/custom")
    public ResponseEntity<ApiResponse<TagResponse>> createCustomTag(@Valid @RequestBody TagRequestDTO request) {
        TagResponse tag = tagService.getOrCreateTag(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, tag, null, "Tag ready"));
    }
}
