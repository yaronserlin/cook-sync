package com.cooksync_server.services;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import com.dtos.response.PagedResponse;

/**
 * Utility class converting a Spring Data {@link Page} of entities into a {@link PagedResponse}
 * of DTOs. Centralizes the entity-to-DTO page mapping shape shared by every paginated service
 * method (e.g. {@link RecipeService}, {@link FavoriteService}, {@link TagService}) so each call
 * site only supplies the entity-to-DTO mapper.
 *
 * @author Yaron Serlin
 * @version 1.1
 * @since 12/08/2026
 */
final class PagedResponseMapper {

    private PagedResponseMapper() {
    }

    /**
     * Maps every entity in a {@link Page} to its DTO form and wraps the result in a {@link PagedResponse}
     * carrying the original page's pagination metadata.
     *
     * Complexity:
     * Time: O(N) where N is the page's content size
     * Space: O(N)
     *
     * @param page source page of entities
     * @param mapper entity-to-DTO mapping function
     * @param <E> source entity type
     * @param <D> target DTO type
     * @return PagedResponse containing the mapped DTOs and the source page's pagination metadata
     */
    static <E, D> PagedResponse<D> toPagedResponse(Page<E> page, Function<E, D> mapper) {
        List<D> content = page.getContent().stream().map(mapper).collect(Collectors.toList());
        return new PagedResponse<>(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isLast());
    }

    /**
     * Fetches an unfiltered, unsorted page directly from a repository and maps it to a
     * {@link PagedResponse} of DTOs in one call. Covers the plain "list everything, paginated"
     * shape shared by simple reference-data lookups (e.g. {@link UnitService#getAllUnits},
     * {@link TagService#getAllTags}) that need no filtering or sorting beyond page/size.
     *
     * Complexity:
     * Time: O(N) where N is the page's content size
     * Space: O(N)
     *
     * @param repository source JPA repository to page through
     * @param page zero-based page index
     * @param size page size limit
     * @param mapper entity-to-DTO mapping function
     * @param <E> source entity type
     * @param <D> target DTO type
     * @return PagedResponse containing the mapped DTOs and pagination metadata
     */
    static <E, D> PagedResponse<D> findAllPaged(JpaRepository<E, ?> repository, int page, int size, Function<E, D> mapper) {
        return toPagedResponse(repository.findAll(PageRequest.of(page, size)), mapper);
    }
}
