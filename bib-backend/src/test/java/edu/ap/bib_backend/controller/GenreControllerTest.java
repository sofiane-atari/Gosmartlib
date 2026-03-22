package edu.ap.bib_backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.ap.bib_backend.dto.GenreDTO;
import edu.ap.bib_backend.dto.GenreStatsDTO;
import edu.ap.bib_backend.dto.PageDTO;
import edu.ap.bib_backend.dto.PaginatedMeta;
import edu.ap.bib_backend.options.LabelGetOptions;
import edu.ap.bib_backend.service.GenreService;

@WebMvcTest(GenreController.class)
@AutoConfigureMockMvc(addFilters = false)
class GenreControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GenreService genreService;

    @Test
    void givenValidGenre_whenPost_thenCreated() throws Exception {
        GenreDTO input = new GenreDTO("Roman", "#8B5CF6", "Literaire fictie", null);
        GenreDTO output = new GenreDTO("Roman", "#8B5CF6", "Literaire fictie", null);

        when(genreService.createLabel(any(GenreDTO.class))).thenReturn(output);

        mockMvc.perform(post("/api/genres")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/genres/Roman"))
                .andExpect(jsonPath("$.name").value("Roman"))
                .andExpect(jsonPath("$.color").value("#8B5CF6"))
                .andExpect(jsonPath("$.description").value("Literaire fictie"));
    }

    @Test
    void givenRequestingFirstPage_whenGetPage_thenReturnGenres() throws Exception {
        GenreDTO genre1 = new GenreDTO("Roman", "#8B5CF6", "Literaire fictie", null);
        GenreDTO genre2 = new GenreDTO("Thriller", "#EF4444", "Spannend verhaal", null);

        PaginatedMeta meta = new PaginatedMeta(1, 10, 2, 1);
        PageDTO<GenreDTO> pageDTO = new PageDTO<>(List.of(genre1, genre2), meta);

        when(genreService.getPage(eq(1), eq(10), any(LabelGetOptions.class))).thenReturn(pageDTO);

        mockMvc.perform(get("/api/genres")
                .param("page", "1")
                .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Roman"))
                .andExpect(jsonPath("$.data[1].name").value("Thriller"))
                .andExpect(jsonPath("$.meta.currentPage").value(1))
                .andExpect(jsonPath("$.meta.pageSize").value(10))
                .andExpect(jsonPath("$.meta.totalItems").value(2))
                .andExpect(jsonPath("$.meta.totalPages").value(1));
    }

    @Test
    void givenRequestingAllGenres_whenGetAll_thenReturnList() throws Exception {
        List<GenreDTO> genres = List.of(
                new GenreDTO("Roman", "#8B5CF6", "Literaire fictie", null),
                new GenreDTO("Thriller", "#EF4444", "Spannend verhaal", null));

        when(genreService.getAllGenres()).thenReturn(genres);

        mockMvc.perform(get("/api/genres/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Roman"))
                .andExpect(jsonPath("$[1].name").value("Thriller"));
    }

    @Test
    void givenRequestingStats_whenGetStats_thenReturnStats() throws Exception {
        GenreStatsDTO stats = new GenreStatsDTO(2, 1);

        when(genreService.getAllGenreStats()).thenReturn(stats);

        mockMvc.perform(get("/api/genres/_stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.unActiveLabels").value(1));
    }

    @Test
    void givenExistingGenreName_whenDelete_thenNoContent() throws Exception {
        doNothing().when(genreService).deleteById("Roman");

        mockMvc.perform(delete("/api/genres/Roman"))
                .andExpect(status().isNoContent());
    }

    @Test
    void givenValidGenre_whenPut_thenReturnUpdatedGenre() throws Exception {
        GenreDTO input = new GenreDTO("Roman", "#000000", "Nieuwe beschrijving", null);
        GenreDTO output = new GenreDTO("Roman", "#000000", "Nieuwe beschrijving", null);

        when(genreService.update(eq("Roman"), any(GenreDTO.class))).thenReturn(output);

        mockMvc.perform(put("/api/genres/Roman")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Roman"))
                .andExpect(jsonPath("$.color").value("#000000"))
                .andExpect(jsonPath("$.description").value("Nieuwe beschrijving"));
    }
}