package com.cooksync_server.services;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dtos.request.tags.TagRequestDTO;
import com.dtos.response.PagedResponse;
import com.dtos.response.tags.TagResponse;
import com.cooksync_server.entities.Tag;
import com.cooksync_server.exceptions.ResourceAllReadyExistsException;
import com.cooksync_server.exceptions.ResourceNotFoundException;
import com.cooksync_server.mappers.TagMapper;
import com.cooksync_server.repositories.TagRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service class handling recipe tag catalog management and custom tag creation.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
@Service
@RequiredArgsConstructor
public class TagService implements ITagService{

    private final TagRepository tagRepository;

    /**
     * Retrieves all tag entries configured in the system.
     *
     * Complexity:
     * Time: O(T) where T is total tag count
     * Space: O(T)
     *
     * @return list of TagResponse DTOs
     */
    @Transactional(readOnly = true)
    public PagedResponse<TagResponse> getAllTags(int page, int size) {
        return PagedResponseMapper.findAllPaged(tagRepository, page, size, TagMapper::toResponse);
    }

    /**
     * Retrieves the most-used tags across all recipes, ranked by descending recipe count.
     *
     * Complexity:
     * Time: O(T log T) where T is total tag count (server-side grouping and ordering)
     * Space: O(limit)
     *
     * @param limit maximum number of popular tags to return
     * @return list of TagResponse DTOs ordered by descending usage
     */
    @Transactional(readOnly = true)
    public List<TagResponse> getPopularTags(int limit) {
        List<Tag> popularTags = tagRepository.findPopularTags(PageRequest.of(0, limit));
        return popularTags.stream()
                .map(TagMapper::toResponse)
                .toList();
    }

    /**
     * Retrieves a tag by unique ID.
     *
     * Complexity:
     * Time: O(1)
     * Space: O(1)
     *
     * @param id target tag ID
     * @return TagResponse DTO
     */
    @Transactional(readOnly = true)
    public TagResponse getTagById(String id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tag", id));
        return TagMapper.toResponse(tag);
    }

    /**
     * Finds an existing tag by name or creates a new one if not existing.
     *
     * Complexity:
     * Time: O(1)
     * Space: O(1)
     *
     * @param request tag request DTO
     * @return TagResponse DTO
     */
    @Transactional
    public TagResponse getOrCreateTag(TagRequestDTO request) {
        String formattedName = request.name().trim().toLowerCase();
        return tagRepository.findByNameIgnoreCase(formattedName)
                .map(TagMapper::toResponse)
                .orElseGet(() -> TagMapper.toResponse(
                        tagRepository.save(Tag.builder().name(formattedName).build())));
    }

    /**
     * Creates a new tag ensuring uniqueness against existing tag names.
     *
     * Complexity:
     * Time: O(1)
     * Space: O(1)
     *
     * @param request tag creation request DTO
     * @return TagResponse DTO of created tag
     */
    @Transactional
    public TagResponse createTag(TagRequestDTO request) {
        String formattedName = request.name().trim().toLowerCase();
        ensureNameAvailable(formattedName, null);

        Tag newTag = Tag.builder()
                .name(formattedName)
                .build();

        return TagMapper.toResponse(tagRepository.save(newTag));
    }

    /**
     * Updates an existing tag name.
     *
     * Complexity:
     * Time: O(1)
     * Space: O(1)
     *
     * @param id target tag ID
     * @param request tag update request DTO
     * @return TagResponse DTO of updated tag
     */
    @Transactional
    public TagResponse updateTag(String id, TagRequestDTO request) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tag", id));

        String formattedName = request.name().trim().toLowerCase();
        ensureNameAvailable(formattedName, id);

        tag.setName(formattedName);
        return TagMapper.toResponse(tagRepository.save(tag));
    }

    /**
     * Deletes a tag by ID.
     *
     * Complexity:
     * Time: O(1)
     * Space: O(1)
     *
     * @param id target tag ID
     */
    @Transactional
    public void deleteTag(String id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tag", id));
        tagRepository.delete(tag);
    }

    private void ensureNameAvailable(String formattedName, String excludeId) {
        tagRepository.findByNameIgnoreCase(formattedName)
                .filter(existing -> excludeId == null || !existing.getId().equals(excludeId))
                .ifPresent(existing -> {
                    throw new ResourceAllReadyExistsException("Tag: '" + formattedName + "'", existing.getId());
                });
    }
}
