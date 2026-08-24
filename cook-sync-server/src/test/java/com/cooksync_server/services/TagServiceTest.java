package com.cooksync_server.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.cooksync_server.entities.Tag;
import com.cooksync_server.repositories.TagRepository;
import com.dtos.request.tags.TagRequestDTO;
import com.dtos.response.PagedResponse;
import com.dtos.response.tags.TagResponse;

/**
 * Unit test suite verifying tag catalog retrieval and get-or-create de-duplication in TagServiceImp.
 *
 * @author Yaron Serlin
 * @version 1.1
 * @since 12/08/2026
 */
@ExtendWith(MockitoExtension.class)
class TagServiceTest {

    @Mock
    private TagRepository tagRepository;

    @InjectMocks
    private TagServiceImp tagService;

    private Tag sampleTag;

    @BeforeEach
    void setUp() {
        sampleTag = Tag.builder().id("tag-1").name("dessert").build();
    }

    @Test
    void getAllTags_ShouldReturnPagedResponse() {
        Page<Tag> page = new PageImpl<>(java.util.List.of(sampleTag), PageRequest.of(0, 10), 1);
        when(tagRepository.findAll(org.mockito.ArgumentMatchers.any(Pageable.class))).thenReturn(page);

        PagedResponse<TagResponse> response = tagService.getAllTags(0, 10);

        assertEquals(1, response.content().size());
        assertEquals("dessert", response.content().get(0).name());
    }

    @Test
    void getOrCreateTag_ShouldReturnExistingTag_WhenAlreadyPresent() {
        TagRequestDTO request = new TagRequestDTO("Dessert");
        when(tagRepository.findByNameIgnoreCase("dessert")).thenReturn(Optional.of(sampleTag));

        TagResponse response = tagService.getOrCreateTag(request);

        assertEquals("tag-1", response.id());
        verify(tagRepository, org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void getOrCreateTag_ShouldCreateNewTag_WhenNotFound() {
        TagRequestDTO request = new TagRequestDTO("Vegan");
        when(tagRepository.findByNameIgnoreCase("vegan")).thenReturn(Optional.empty());
        when(tagRepository.save(org.mockito.ArgumentMatchers.any(Tag.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TagResponse response = tagService.getOrCreateTag(request);

        assertEquals("vegan", response.name());
    }
}
