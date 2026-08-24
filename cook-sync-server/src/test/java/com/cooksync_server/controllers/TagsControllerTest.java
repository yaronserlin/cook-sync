package com.cooksync_server.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cooksync_server.config.JwtUtil;
import com.cooksync_server.services.TagService;
import com.dtos.request.tags.TagRequestDTO;
import com.dtos.response.PagedResponse;
import com.dtos.response.tags.TagResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * Web-layer test suite verifying {@link TagsController}'s request mapping and status-code wiring
 * against a mocked {@link TagService}.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 24/08/2026
 */
@WebMvcTest(controllers = TagsController.class)
@WithMockUser
class TagsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TagService tagService;

    /**
     * {@link com.cooksync_server.config.JwtAuthenticationFilter} is auto-registered by
     * {@code @WebMvcTest} as a servlet {@code Filter}; mocking its {@code JwtUtil} dependency
     * just satisfies that bean's constructor (no {@code Authorization} header is sent here).
     */
    @MockitoBean
    private JwtUtil jwtUtil;

    @Test
    void getAllTags_ShouldReturnPagedTags() throws Exception {
        TagResponse tag = new TagResponse("tag-1", "dessert", null, null);
        when(tagService.getAllTags(0, 20)).thenReturn(new PagedResponse<>(List.of(tag), 0, 20, 1, 1, true));

        mockMvc.perform(get("/api/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].name").value("dessert"));
    }

    @Test
    void getPopularTags_ShouldReturnTagsOrderedByUsage() throws Exception {
        when(tagService.getPopularTags(5)).thenReturn(List.of(new TagResponse("tag-1", "dessert", null, null)));

        mockMvc.perform(get("/api/tags/popular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("dessert"));
    }

    @Test
    void createCustomTag_ShouldReturnCreated_WhenNameValid() throws Exception {
        TagRequestDTO request = new TagRequestDTO("Vegan");
        when(tagService.getOrCreateTag(any())).thenReturn(new TagResponse("tag-2", "vegan", null, null));

        mockMvc.perform(post("/api/tags/custom")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("vegan"));
    }

    @Test
    void createCustomTag_ShouldReturnBadRequest_WhenNameTooShort() throws Exception {
        TagRequestDTO request = new TagRequestDTO("a");

        mockMvc.perform(post("/api/tags/custom")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
