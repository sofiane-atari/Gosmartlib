package edu.ap.bib_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ap.bib_backend.dto.PageDTO;
import edu.ap.bib_backend.dto.PaginatedMeta;
import edu.ap.bib_backend.dto.TagDTO;
import edu.ap.bib_backend.dto.TagStatsDTO;
import edu.ap.bib_backend.service.TagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TagControllerIntegrationTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private TagService tagService;
    private TagController tagController;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        tagService = Mockito.mock(TagService.class);
        tagController = new TagController(tagService);
        authentication = new UsernamePasswordAuthenticationToken("librarian-a", null);

        mockMvc = MockMvcBuilders
                .standaloneSetup(tagController)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void createTag_shouldReturnCreatedTag() throws Exception {
        TagDTO request = new TagDTO(
                null,
                "Populair",
                "Veel gelezen boeken",
                null,
                null);

        TagDTO response = new TagDTO(
                1L,
                "Populair",
                "Veel gelezen boeken",
                0L,
                null);

        Mockito.when(tagService.createTag(any(TagDTO.class)))
                .thenReturn(response);

        mockMvc.perform(post("/tags")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Populair"))
                .andExpect(jsonPath("$.description").value("Veel gelezen boeken"))
                .andExpect(jsonPath("$.bookCount").value(0));
    }

    @Test
    void getTags_shouldReturnPagedTags() throws Exception {
        PageDTO<TagDTO> response = new PageDTO<>(
                List.of(
                        new TagDTO(1L, "Fantasy", "Fantasy boeken", 0L, null),
                        new TagDTO(2L, "Thriller", "Spannende boeken", 0L, null)),
                new PaginatedMeta(0, 2, 3, 2));

        Mockito.when(tagService.getTagsByPage(
                eq(0),
                eq(2),
                isNull(),
                isNull(),
                eq(true)))
                .thenReturn(response);

        mockMvc.perform(get("/tags")
                .param("page", "0")
                .param("pageSize", "2")
                .param("includeBookCount", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].name").value("Fantasy"))
                .andExpect(jsonPath("$.data[1].name").value("Thriller"))
                .andExpect(jsonPath("$.meta.currentPage").value(0))
                .andExpect(jsonPath("$.meta.pageSize").value(2))
                .andExpect(jsonPath("$.meta.totalItems").value(3))
                .andExpect(jsonPath("$.meta.totalPages").value(2));
    }

    @Test
    void getTags_shouldFilterBySearch() throws Exception {
        PageDTO<TagDTO> response = new PageDTO<>(
                List.of(
                        new TagDTO(1L, "Fantasy", "Fantasy boeken", 0L, null)),
                new PaginatedMeta(0, 10, 1, 1));

        Mockito.when(tagService.getTagsByPage(
                eq(0),
                eq(10),
                eq("fant"),
                isNull(),
                eq(true)))
                .thenReturn(response);

        mockMvc.perform(get("/tags")
                .param("search", "fant")
                .param("includeBookCount", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Fantasy"));
    }

    @Test
    void getTagStats_shouldReturnStats() throws Exception {
        Mockito.when(tagService.getTagStats())
                .thenReturn(new TagStatsDTO(2L, 2L));

        mockMvc.perform(get("/tags/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.unActiveTags").value(2));
    }

    @Test
    void updateTag_shouldUpdateTag() throws Exception {
        TagDTO request = new TagDTO(
                1L,
                "Nieuw",
                "Nieuwe beschrijving",
                null,
                null);

        TagDTO response = new TagDTO(
                1L,
                "Nieuw",
                "Nieuwe beschrijving",
                0L,
                null);

        Mockito.when(tagService.updateTag(eq(1L), any(TagDTO.class)))
                .thenReturn(response);

        mockMvc.perform(put("/tags/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Nieuw"))
                .andExpect(jsonPath("$.description").value("Nieuwe beschrijving"));
    }

    @Test
    void deleteTag_shouldDeleteTag() throws Exception {
        mockMvc.perform(delete("/tags/{id}", 1L))
                .andExpect(status().isOk());

        Mockito.verify(tagService).deleteTag(1L);
    }

    @Test
    void createTag_withAuthentication_shouldUseSchoolScopedService() throws Exception {
        TagDTO request = new TagDTO(
                null,
                "Populair",
                "Veel gelezen boeken",
                null,
                null);

        TagDTO response = new TagDTO(
                1L,
                "Populair",
                "Veel gelezen boeken",
                0L,
                null);

        Mockito.when(tagService.createTag(any(TagDTO.class), eq(authentication)))
                .thenReturn(response);

        mockMvc.perform(post("/tags")
                .principal(authentication)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Populair"));

        Mockito.verify(tagService).createTag(any(TagDTO.class), eq(authentication));
    }

    @Test
    void getTags_withAuthentication_shouldUseSchoolScopedService() throws Exception {
        PageDTO<TagDTO> response = new PageDTO<>(
                List.of(new TagDTO(1L, "Fantasy", "Fantasy boeken", 0L, null)),
                new PaginatedMeta(0, 10, 1, 1));

        Mockito.when(tagService.getTagsByPage(
                eq(0),
                eq(10),
                eq("fant"),
                isNull(),
                eq(true),
                eq(authentication)))
                .thenReturn(response);

        mockMvc.perform(get("/tags")
                .principal(authentication)
                .param("search", "fant")
                .param("includeBookCount", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Fantasy"));

        Mockito.verify(tagService).getTagsByPage(0, 10, "fant", null, true, authentication);
    }

    @Test
    void getTagStats_withAuthentication_shouldUseSchoolScopedService() throws Exception {
        Mockito.when(tagService.getTagStats(authentication))
                .thenReturn(new TagStatsDTO(2L, 2L));

        mockMvc.perform(get("/tags/stats")
                .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.unActiveTags").value(2));

        Mockito.verify(tagService).getTagStats(authentication);
    }

    @Test
    void updateTag_withAuthentication_shouldUseSchoolScopedService() throws Exception {
        TagDTO request = new TagDTO(
                1L,
                "Nieuw",
                "Nieuwe beschrijving",
                null,
                null);

        TagDTO response = new TagDTO(
                1L,
                "Nieuw",
                "Nieuwe beschrijving",
                0L,
                null);

        Mockito.when(tagService.updateTag(eq(1L), any(TagDTO.class), eq(authentication)))
                .thenReturn(response);

        mockMvc.perform(put("/tags/{id}", 1L)
                .principal(authentication)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Nieuw"));

        Mockito.verify(tagService).updateTag(eq(1L), any(TagDTO.class), eq(authentication));
    }

    @Test
    void deleteTag_withAuthentication_shouldUseSchoolScopedService() throws Exception {
        mockMvc.perform(delete("/tags/{id}", 1L)
                .principal(authentication))
                .andExpect(status().isOk());

        Mockito.verify(tagService).deleteTag(1L, authentication);
    }
}
