package com.cooksync_server.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.dtos.response.PagedResponse;

import com.cooksync_server.services.TagService;
import com.dtos.request.tags.TagRequestDTO;
import com.dtos.response.ApiResponse;
import com.dtos.response.tags.TagResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST Controller managing recipe tag creation, retrieval, updates, and deletion.
 * Reading tags is public; administration actions require 'ADMIN' authority.
 *
 * @author Yaron Serlin
 * @version 1.0
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
     * Complexity:
     * Time: O(T) where T is total tag count
     * Space: O(T)
     *
     * @param page page number index
     * @param size page size limit
     * @return response entity containing list of TagResponse DTOs
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<TagResponse>>> getAllTags(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<TagResponse> tags = tagService.getAllTags(page, size);
        return ResponseEntity.ok(new ApiResponse<>(true, tags, null, "All tags retrieved successfully"));
    }

    /**
     * Retrieves the most-used tags across all recipes, ranked by descending recipe count. Only
     * tags actually attached to at least one recipe are included.
     *
     * Complexity:
     * Time: O(T log T) where T is total tag count
     * Space: O(limit)
     *
     * @param limit maximum number of popular tags to return
     * @return response entity containing list of TagResponse DTOs ordered by descending usage
     */
    @GetMapping("/popular")
    public ResponseEntity<ApiResponse<List<TagResponse>>> getPopularTags(
            @RequestParam(defaultValue = "5") int limit) {
        List<TagResponse> tags = tagService.getPopularTags(limit);
        return ResponseEntity.ok(new ApiResponse<>(true, tags, null, "Popular tags retrieved successfully"));
    }

    /**
     * Retrieves a tag by unique ID.
     *
     * @param id target tag ID
     * @return response entity containing TagResponse DTO
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TagResponse>> getTagById(@PathVariable String id) {
        TagResponse tag = tagService.getTagById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, tag, null, "Tag retrieved successfully"));
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

    /**
     * Creates a new tag with administrative validation against existing tag names.
     *
     * @param request tag request DTO
     * @return response entity containing created TagResponse DTO
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<TagResponse>> createTag(@Valid @RequestBody TagRequestDTO request) {
        TagResponse createdTag = tagService.createTag(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, createdTag, null, "Tag created successfully"));
    }

    /**
     * Updates an existing tag name.
     *
     * @param id target tag ID
     * @param request tag update request DTO
     * @return response entity containing updated TagResponse DTO
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TagResponse>> updateTag(
            @PathVariable String id,
            @Valid @RequestBody TagRequestDTO request) {
        TagResponse updatedTag = tagService.updateTag(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, updatedTag, null, "Tag updated successfully"));
    }

    /**
     * Deletes a tag by ID.
     *
     * @param id target tag ID
     * @return response entity acknowledging tag deletion
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTag(@PathVariable String id) {
        tagService.deleteTag(id);
        return ResponseEntity.ok(new ApiResponse<>(true, null, null, "Tag deleted successfully"));
    }
}
