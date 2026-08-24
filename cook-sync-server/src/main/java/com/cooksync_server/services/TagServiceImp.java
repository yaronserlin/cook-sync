package com.cooksync_server.services;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dtos.request.tags.TagRequestDTO;
import com.dtos.response.PagedResponse;
import com.dtos.response.tags.TagResponse;
import com.cooksync_server.entities.Tag;
import com.cooksync_server.mappers.TagMapper;
import com.cooksync_server.repositories.TagRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service class handling recipe tag catalog management and custom tag creation.
 *
 * @author Yaron Serlin
 * @version 1.1
 * @since 02/08/2026
 */
@Service
@RequiredArgsConstructor
public class TagServiceImp implements TagService{

    private final TagRepository tagRepository;

    /**
     * Retrieves all tag entries configured in the system.
     *
     * Complexity:
     * Time: O(T) where T is total tag count
     * Space: O(T)
     *
     * @param page page number index
     * @param size page size limit
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
     * Finds an existing tag by name or creates a new one if not existing.
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
}
