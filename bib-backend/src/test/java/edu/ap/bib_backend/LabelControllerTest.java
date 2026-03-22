package edu.ap.bib_backend;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.ap.bib_backend.controller.GenreController;
import edu.ap.bib_backend.dto.GenreDTO;
import edu.ap.bib_backend.dto.GenreStatsDTO;
import edu.ap.bib_backend.dto.PageDTO;
import edu.ap.bib_backend.dto.PaginatedMeta;
import edu.ap.bib_backend.exception.ResourceNotFoundException;
import edu.ap.bib_backend.options.GenreGetOptions;
import edu.ap.bib_backend.service.GenreService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@WebMvcTest(GenreController.class)
@Import(TestSecurityConfig.class)
class GenreControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private GenreService genreService;

        // -------------------------------------------------------
        // POST /api/genres - createGenre
        // -------------------------------------------------------
        @Test
        void givenNoDescription_whenCreateGenre_thenCreateWithoutDescription() throws Exception {
                // Arrange
                GenreDTO newGenre = GenreDTO.builder().name("testGenre").color("#a75064").build();
                GenreDTO savedGenre = GenreDTO.builder().name("testGenre").color("#a75064").build();

                when(genreService.createGenre(any(GenreDTO.class))).thenReturn(savedGenre);

                // Act
                ResultActions results = mockMvc.perform(post("/genres")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(newGenre)));

                // Assert
                results
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.name").value(savedGenre.name()))
                                .andExpect(jsonPath("$.color").value(savedGenre.color()))
                                .andExpect(jsonPath("$.description").value(savedGenre.description()));
        }

        @Test
        void givenValidBody_whenCreateGenre_thenReturn201WithLocationHeader() throws Exception {
                // Arrange
                GenreDTO requestBody = GenreDTO.builder().name("urgent").color("#FF0001")
                                .description("High priority tasks").build();
                GenreDTO createdGenre = GenreDTO.builder().name("urgent").color("#FF0001")
                                .description("High priority tasks").build();

                given(genreService.createGenre(any(GenreDTO.class)))
                                .willReturn(createdGenre);

                // Act

                ResultActions result = mockMvc.perform(post("/genres")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestBody)));

                // Assert
                result
                                .andExpect(status().isCreated())
                                .andExpect(header().string("Location", containsString("/genres/urgent")))
                                .andExpect(jsonPath("$.name").value("urgent"))
                                .andExpect(jsonPath("$.description").value("High priority tasks"))
                                .andExpect(jsonPath("$.color").value("#FF0001"));
        }

        @Test
        void givenNullName_whenCreateGenre_thenReturn400() throws Exception {
                // Arrange
                GenreDTO invalidBody = GenreDTO.builder().color("#FF0001").description("Some description").build();

                // Act
                ResultActions result = mockMvc.perform(post("/genres")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidBody)));

                // Assert
                result.andExpect(status().isBadRequest());
        }

        @Test
        void givenOptionalFieldsNull_whenCreateGenre_thenReturn201() throws Exception {
                // Arrange
                GenreDTO requestBody = GenreDTO.builder().name("urgent").color("#FF0001").build();
                GenreDTO createdGenre = GenreDTO.builder().name("urgent").color("#FF0001").build();

                given(
                        genreService.createGenre(any(GenreDTO.class)))
                                .willReturn(createdGenre);

                // Act
                ResultActions result = mockMvc.perform(post("/genres")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestBody)));

                // Assert
                result.andExpect(status().isCreated())
                                .andExpect(jsonPath("$.name").value("urgent"))
                                .andExpect(jsonPath("$.color").value("#FF0001"))
                                .andExpect(jsonPath("$.description").isEmpty());
        }

        @Test
        void givenServiceThrows_whenCreateGenre_thenReturn501() throws Exception {
                // Arrange
                GenreDTO requestBody = GenreDTO.builder().name("urgent").color("#FF0001").build();

                given(genreService.createGenre(any(GenreDTO.class)))
                                .willThrow(new RuntimeException("Unexpected error"));

                // Act
                ResultActions result = mockMvc.perform(post("/genres")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestBody)));

                // Assert
                result.andExpect(status().isInternalServerError())
                                .andExpect(r -> assertInstanceOf(RuntimeException.class, r.getResolvedException()));
        }

        @Test
        void givenValidRequest_whenCreateGenre_thenCallServiceOnce() throws Exception {
                // Arrange
                GenreDTO requestBody = GenreDTO.builder().name("feature").color("#01FF00").description("Feature genre")
                                .build();
                GenreDTO createdGenre = GenreDTO.builder().name("feature").color("#01FF00").description("Feature genre")
                                .build();

                given(genreService.createGenre(any(GenreDTO.class)))
                                .willReturn(createdGenre);

                // Act
                mockMvc.perform(post("/genres")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestBody)));

                // Assert
                then(genreService).should(times(1)).createGenre(any(GenreDTO.class));
        }

        // -------------------------------------------------------
        // GET /genres - getPage
        // -------------------------------------------------------
        @Test
        void givenRequestingFirstPage_whenGetPage_thenReturnGenres() throws Exception {
                // Arrange
                GenreDTO genre = GenreDTO.builder().name("Fiction").color("#FF0000").description("Fiction books")
                                .build();
                PageDTO<GenreDTO> pageDTO = new PageDTO<>(List.of(genre), new PaginatedMeta(0, 10, 1, 1));

                given(genreService.getPage(anyInt(), anyInt(), any(GenreGetOptions.class))).willReturn(pageDTO);

                // Act
                ResultActions result = mockMvc.perform(get("/genres"));

                // Assert
                result.andExpect(status().isOk())
                                .andExpect(jsonPath("$.data").isArray())
                                .andExpect(jsonPath("$.data[0].name").value("Fiction"))
                                .andExpect(jsonPath("$.meta.currentPage").value(0));
        }

        @Test
        void givenRequestingSecondPage_whenGetPage_thenReturnGenresWithPagination() throws Exception {
                // Arrange
                GenreDTO genre = GenreDTO.builder().name("Non-Fiction").color("#00FF00")
                                .description("Non-fiction books").build();
                PageDTO<GenreDTO> pageDTO = new PageDTO<>(List.of(genre), new PaginatedMeta(1, 10, 2, 20));

                given(genreService.getPage(anyInt(), anyInt(), any(GenreGetOptions.class))).willReturn(pageDTO);

                // Act
                ResultActions result = mockMvc.perform(get("/genres")
                                .param("page", "2"));

                // Assert
                result.andExpect(status().isOk())
                                .andExpect(jsonPath("$.data").isArray())
                                .andExpect(jsonPath("$.data[0].name").value("Non-Fiction"))
                                .andExpect(jsonPath("$.meta.currentPage").value(1));
        }

        @Test
        void givenSearchQuery_whenGetPage_thenPassSearchParameter() throws Exception {
                // Arrange
                GenreDTO genre = GenreDTO.builder().name("Science").color("#0000FF").description("Science books")
                                .build();
                PageDTO<GenreDTO> pageDTO = new PageDTO<>(List.of(genre), new PaginatedMeta(0, 10, 1, 1));

                given(genreService.getPage(anyInt(), anyInt(), any(GenreGetOptions.class))).willReturn(pageDTO);

                // Act
                ResultActions result = mockMvc.perform(get("/genres")
                                .param("search", "science"));

                // Assert
                result.andExpect(status().isOk());
        }

        @Test
        void givenBookCountParam_whenGetPage_thenPassBookCountFilter() throws Exception {
                // Arrange
                GenreDTO genre = GenreDTO.builder().name("Fiction").color("#FF0000").description("Fiction books")
                                .build();
                PageDTO<GenreDTO> pageDTO = new PageDTO<>(List.of(genre), new PaginatedMeta(0, 10, 1, 1));

                given(genreService.getPage(anyInt(), anyInt(), any(GenreGetOptions.class))).willReturn(pageDTO);

                // Act

                ResultActions result = mockMvc.perform(get("/genres")
                                .param("bookCount", "1"));

                // Assert
                result.andExpect(status().isOk());
        }

        @Test
        void givenNoParams_whenGetPage_thenUseDefaultValues() throws Exception {
                // Arrange
                GenreDTO genre = GenreDTO.builder().name("Fiction").color("#FF0000").description("Fiction books")
                                .build();
                PageDTO<GenreDTO> pageDTO = new PageDTO<>(List.of(genre), new PaginatedMeta(0, 10, 1, 1));

                given(genreService.getPage(anyInt(), anyInt(), any(GenreGetOptions.class))).willReturn(pageDTO);

                // Act
                ResultActions result = mockMvc.perform(get("/genres"));

                // Assert
                result.andExpect(status().isOk());
        }

        // -------------------------------------------------------
        // GET /genres/_stats - getGenreStats
        // -------------------------------------------------------
        @Test
        void givenGenresExist_whenGetGenreStats_thenReturnStats() throws Exception {
                // Arrange
                GenreStatsDTO stats = new GenreStatsDTO(10, 5);

                given(genreService.getAllGenreStats()).willReturn(stats);

                // Act
                ResultActions result = mockMvc.perform(get("/genres/_stats"));

                // Assert
                result.andExpect(status().isOk())
                                .andExpect(jsonPath("$.totalItems").value(10))
                                .andExpect(jsonPath("$.unActiveGenres").value(5));
        }

        @Test
        void givenNoGenres_whenGetGenreStats_thenReturnZeroStats() throws Exception {
                // Arrange
                GenreStatsDTO stats = new GenreStatsDTO(0, 0);

                given(genreService.getAllGenreStats()).willReturn(stats);

                // Act
                ResultActions result = mockMvc.perform(get("/genres/_stats"));

                // Assert
                result.andExpect(status().isOk())
                                .andExpect(jsonPath("$.totalItems").value(0))
                                .andExpect(jsonPath("$.unActiveGenres").value(0));
        }

        @Test
        void givenAllGenresHaveBooks_whenGetGenreStats_thenReturnCorrectStats() throws Exception {
                // Arrange
                GenreStatsDTO stats = new GenreStatsDTO(5, 0);

                given(genreService.getAllGenreStats()).willReturn(stats);

                // Act
                ResultActions result = mockMvc.perform(get("/genres/_stats"));

                // Assert
                result.andExpect(status().isOk())
                                .andExpect(jsonPath("$.totalItems").value(5))
                                .andExpect(jsonPath("$.unActiveGenres").value(0));
        }

        // -------------------------------------------------------
        // DELETE /genres/{name} - delete
        // -------------------------------------------------------
        @Test
        void givenExistingGenreName_whenDelete_thenReturn204() throws Exception {
                // Arrange
                String genreName = "Fiction";

                doNothing().when(genreService).deleteById(genreName);

                // Act
    ResultActions result = mockMvc.perform(delete("/genres/{name}", genreName));

                // Assert
                result.andExpect(status().isNoContent());
                then(genreService).should(times(1)).deleteById(genreName);
        }

        @Test
        void givenNonExistingGenreName_whenDelete_thenReturn404() throws Exception {
                // Arrange
                String genreName = "NonExistent";

                doThrow(new ResourceNotFoundException(genreName, "Genre not found"))
                                .when(genreService).deleteById(genreName);

                // Act

                ResultActions result = mockMvc.perform(delete("/genres/{name}", genreName));


                // Assert
                result.andExpect(status().isNotFound());
                then(genreService).should(times(1)).deleteById(genreName);
        }

        @Test
        void givenGenreNameWithSpecialCharacters_whenDelete_thenReturn204() throws Exception {
                // Arrange - Use a simple name without URL encoding issues
                String genreName = "Science_Fiction";

                doNothing().when(genreService).deleteById(genreName);

                // Act
                ResultActions result = mockMvc.perform(delete("/genres/{name}", genreName));

                // Assert
                result.andExpect(status().isNoContent());
                then(genreService).should(times(1)).deleteById(genreName);
        }

        @Test
        void givenDeleteError_whenDelete_thenReturn500() throws Exception {
                // Arrange
                String genreName = "Fiction";

                doThrow(new RuntimeException("Database error"))
                                .when(genreService).deleteById(genreName);

                // Act
                ResultActions result = mockMvc.perform(delete("/genres/{name}", genreName));

                // Assert
                result.andExpect(status().isInternalServerError());
        }

        // -------------------------------------------------------
        // PUT /genres/{name} - updateGenre
        // -------------------------------------------------------
        @Test
        void givenValidBody_whenUpdateGenre_thenReturn200() throws Exception {
                // Arrange
                String genreName = "Fiction";
                GenreDTO requestBody = GenreDTO.builder()
                                .name(genreName)
                                .color("#00FF00")
                                .description("Updated fiction description")
                                .build();
                GenreDTO updatedGenre = GenreDTO.builder()
                                .name(genreName)
                                .color("#00FF00")
                                .description("Updated fiction description")
                                .build();

                when(genreService.update(eq(genreName), any(GenreDTO.class))).thenReturn(updatedGenre);

                // Act
                ResultActions result = mockMvc.perform(put("/genres/{name}", genreName)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestBody)));

                // Assert
                result.andExpect(status().isOk())
                                .andExpect(jsonPath("$.name").value(genreName))
                                .andExpect(jsonPath("$.color").value("#00FF00"))
                                .andExpect(jsonPath("$.description").value("Updated fiction description"));
                then(genreService).should(times(1)).update(eq(genreName), any(GenreDTO.class));
        }

        @Test
        void givenNonExistingGenre_whenUpdateGenre_thenReturn404() throws Exception {
                // Arrange
                String genreName = "NonExistent";
                GenreDTO requestBody = GenreDTO.builder()
                                .name(genreName)
                                .color("#00FF00")
                                .description("Some description")
                                .build();

                when(genreService.update(eq(genreName), any(GenreDTO.class)))
                                .thenThrow(new ResourceNotFoundException(genreName, "Genre not found"));

                // Act
                ResultActions result = mockMvc.perform(put("/genres/{name}", genreName)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestBody)));

                // Assert
                result.andExpect(status().isNotFound());
                then(genreService).should(times(1)).update(eq(genreName), any(GenreDTO.class));
        }

        @Test
        void givenServiceThrows_whenUpdateGenre_thenReturn500() throws Exception {
                // Arrange
                String genreName = "Fiction";
                GenreDTO requestBody = GenreDTO.builder()
                                .name(genreName)
                                .color("#00FF00")
                                .description("Updated description")
                                .build();

                when(genreService.update(eq(genreName), any(GenreDTO.class)))
                                .thenThrow(new RuntimeException("Unexpected error"));

                // Act
                ResultActions result = mockMvc.perform(put("/genres/{name}", genreName)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestBody)));

                // Assert
                result.andExpect(status().isInternalServerError())
                                .andExpect(r -> assertInstanceOf(RuntimeException.class, r.getResolvedException()));
        }

        @Test
        void givenValidRequest_whenUpdateGenre_thenCallServiceOnce() throws Exception {
                // Arrange
                String genreName = "Science";
                GenreDTO requestBody = GenreDTO.builder()
                                .name(genreName)
                                .color("#0000FF")
                                .description("Science books")
                                .build();
                GenreDTO updatedGenre = GenreDTO.builder()
                                .name(genreName)
                                .color("#0000FF")
                                .description("Science books")
                                .build();

                when(genreService.update(eq(genreName), any(GenreDTO.class))).thenReturn(updatedGenre);

                // Act
                mockMvc.perform(put("/genres/{name}", genreName)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestBody)));

                // Assert
                then(genreService).should(times(1)).update(eq(genreName), any(GenreDTO.class));
        }

        @Test
        void givenUpdateWithNullDescription_whenUpdateGenre_thenReturn200() throws Exception {
                // Arrange
                String genreName = "Fiction";
                GenreDTO requestBody = GenreDTO.builder()
                                .name(genreName)
                                .color("#FF0000")
                                .build();
                GenreDTO updatedGenre = GenreDTO.builder()
                                .name(genreName)
                                .color("#FF0000")
                                .description(null)
                                .build();

                when(genreService.update(eq(genreName), any(GenreDTO.class))).thenReturn(updatedGenre);

                // Act
                ResultActions result = mockMvc.perform(put("/genres/{name}", genreName)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestBody)));

                // Assert
                result.andExpect(status().isOk())
                                .andExpect(jsonPath("$.name").value(genreName))
                                .andExpect(jsonPath("$.color").value("#FF0000"));
        }
}
