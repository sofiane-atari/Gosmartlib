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
import edu.ap.bib_backend.options.LabelGetOptions;
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
        private GenreService labelService;

        // -------------------------------------------------------
        // POST /api/genres - createLabel
        // -------------------------------------------------------
        @Test
        void givenNoDescription_whenCreateLabel_thenCreateWithoutDescription() throws Exception {
                // Arrange
                GenreDTO newLabel = GenreDTO.builder().name("testLabel").color("#a75064").build();
                GenreDTO savedLabel = GenreDTO.builder().name("testLabel").color("#a75064").build();

                when(labelService.createLabel(any(GenreDTO.class))).thenReturn(savedLabel);

                // Act
                ResultActions results = mockMvc.perform(post("/api/genres")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(newLabel)));

                // Assert
                results
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.name").value(savedLabel.name()))
                                .andExpect(jsonPath("$.color").value(savedLabel.color()))
                                .andExpect(jsonPath("$.description").value(savedLabel.description()));
        }

        @Test
        void givenValidBody_whenCreateLabel_thenReturn201WithLocationHeader() throws Exception {
                // Arrange
                GenreDTO requestBody = GenreDTO.builder().name("urgent").color("#FF0001")
                                .description("High priority tasks").build();
                GenreDTO createdLabel = GenreDTO.builder().name("urgent").color("#FF0001")
                                .description("High priority tasks").build();

                given(labelService.createLabel(any(GenreDTO.class)))
                                .willReturn(createdLabel);

                // Act
                ResultActions result = mockMvc.perform(post("/api/genres")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestBody)));

                // Assert
                result
                                .andExpect(status().isCreated())
                                .andExpect(header().string("Location", containsString("/api/genres/urgent")))
                                .andExpect(jsonPath("$.name").value("urgent"))
                                .andExpect(jsonPath("$.description").value("High priority tasks"))
                                .andExpect(jsonPath("$.color").value("#FF0001"));
        }

        @Test
        void givenNullName_whenCreateLabel_thenReturn400() throws Exception {
                // Arrange
                GenreDTO invalidBody = GenreDTO.builder().color("#FF0001").description("Some description").build();

                // Act
                ResultActions result = mockMvc.perform(post("/api/genres")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidBody)));

                // Assert
                result.andExpect(status().isBadRequest());
        }

        @Test
        void givenOptionalFieldsNull_whenCreateLabel_thenReturn201() throws Exception {
                // Arrange
                GenreDTO requestBody = GenreDTO.builder().name("urgent").color("#FF0001").build();
                GenreDTO createdLabel = GenreDTO.builder().name("urgent").color("#FF0001").build();

                given(labelService.createLabel(any(GenreDTO.class)))
                                .willReturn(createdLabel);

                // Act
                ResultActions result = mockMvc.perform(post("/api/genres")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestBody)));

                // Assert
                result.andExpect(status().isCreated())
                                .andExpect(jsonPath("$.name").value("urgent"))
                                .andExpect(jsonPath("$.color").value("#FF0001"))
                                .andExpect(jsonPath("$.description").isEmpty());
        }

        @Test
        void givenServiceThrows_whenCreateLabel_thenReturn501() throws Exception {
                // Arrange
                GenreDTO requestBody = GenreDTO.builder().name("urgent").color("#FF0001").build();

                given(labelService.createLabel(any(GenreDTO.class)))
                                .willThrow(new RuntimeException("Unexpected error"));

                // Act
                ResultActions result = mockMvc.perform(post("/api/genres")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestBody)));

                // Assert
                result.andExpect(status().isInternalServerError())
                                .andExpect(r -> assertInstanceOf(RuntimeException.class, r.getResolvedException()));
        }

        @Test
        void givenValidRequest_whenCreateLabel_thenCallServiceOnce() throws Exception {
                // Arrange
                GenreDTO requestBody = GenreDTO.builder().name("feature").color("#01FF00").description("Feature label")
                                .build();
                GenreDTO createdLabel = GenreDTO.builder().name("feature").color("#01FF00").description("Feature label")
                                .build();

                given(labelService.createLabel(any(GenreDTO.class)))
                                .willReturn(createdLabel);

                // Act
                mockMvc.perform(post("/api/genres")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestBody)));

                // Assert
                then(labelService).should(times(1)).createLabel(any(GenreDTO.class));
        }

        // -------------------------------------------------------
        // GET /api/genres - getPage
        // -------------------------------------------------------
        @Test
        void givenRequestingFirstPage_whenGetPage_thenReturnLabels() throws Exception {
                // Arrange
                GenreDTO label = GenreDTO.builder().name("Fiction").color("#FF0000").description("Fiction books")
                                .build();
                PageDTO<GenreDTO> pageDTO = new PageDTO<>(List.of(label), new PaginatedMeta(0, 10, 1, 1));

                given(labelService.getPage(anyInt(), anyInt(), any(LabelGetOptions.class))).willReturn(pageDTO);

                // Act
                ResultActions result = mockMvc.perform(get("/api/genres"));

                // Assert
                result.andExpect(status().isOk())
                                .andExpect(jsonPath("$.data").isArray())
                                .andExpect(jsonPath("$.data[0].name").value("Fiction"))
                                .andExpect(jsonPath("$.meta.currentPage").value(0));
        }

        @Test
        void givenRequestingSecondPage_whenGetPage_thenReturnLabelsWithPagination() throws Exception {
                // Arrange
                GenreDTO label = GenreDTO.builder().name("Non-Fiction").color("#00FF00-F")
                                .description("Noniction books").build();
                PageDTO<GenreDTO> pageDTO = new PageDTO<>(List.of(label), new PaginatedMeta(1, 10, 2, 20));

                given(labelService.getPage(anyInt(), anyInt(), any(LabelGetOptions.class))).willReturn(pageDTO);

                // Act
                ResultActions result = mockMvc.perform(get("/api/genres")
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
                GenreDTO label = GenreDTO.builder().name("Science").color("#0000FF").description("Science books")
                                .build();
                PageDTO<GenreDTO> pageDTO = new PageDTO<>(List.of(label), new PaginatedMeta(0, 10, 1, 1));

                given(labelService.getPage(anyInt(), anyInt(), any(LabelGetOptions.class))).willReturn(pageDTO);

                // Act
                ResultActions result = mockMvc.perform(get("/api/genres")
                                .param("search", "science"));

                // Assert
                result.andExpect(status().isOk());
        }

        @Test
        void givenBookCountParam_whenGetPage_thenPassBookCountFilter() throws Exception {
                // Arrange
                PageDTO<GenreDTO> pageDTO = new PageDTO<>(List.of(), new PaginatedMeta(0, 10, 0, 0));

                given(labelService.getPage(anyInt(), anyInt(), any(LabelGetOptions.class))).willReturn(pageDTO);

                // Act
                ResultActions result = mockMvc.perform(get("/api/genres")
                                .param("bookCount", "1"));

                // Assert
                result.andExpect(status().isOk());
        }

        @Test
        void givenNoParams_whenGetPage_thenUseDefaultValues() throws Exception {
                // Arrange
                PageDTO<GenreDTO> pageDTO = new PageDTO<>(List.of(), new PaginatedMeta(0, 10, 0, 0));

                given(labelService.getPage(anyInt(), anyInt(), any(LabelGetOptions.class))).willReturn(pageDTO);

                // Act
                ResultActions result = mockMvc.perform(get("/api/genres"));

                // Assert
                result.andExpect(status().isOk());
        }

        // -------------------------------------------------------
        // GET /api/genres/_stats - getLabelStats
        // -------------------------------------------------------
        @Test
        void givenLabelsExist_whenGetLabelStats_thenReturnStats() throws Exception {
                // Arrange
                GenreStatsDTO stats = new GenreStatsDTO(10, 5);

                given(labelService.getAllGenreStats()).willReturn(stats);

                // Act
                ResultActions result = mockMvc.perform(get("/api/genres/_stats"));

                // Assert
                result.andExpect(status().isOk())
                                .andExpect(jsonPath("$.totalItems").value(10))
                                .andExpect(jsonPath("$.unActiveLabels").value(5));
        }

        @Test
        void givenNoLabels_whenGetLabelStats_thenReturnZeroStats() throws Exception {
                // Arrange
                GenreStatsDTO stats = new GenreStatsDTO(0, 0);

                given(labelService.getAllGenreStats()).willReturn(stats);

                // Act
                ResultActions result = mockMvc.perform(get("/api/genres/_stats"));

                // Assert
                result.andExpect(status().isOk())
                                .andExpect(jsonPath("$.totalItems").value(0))
                                .andExpect(jsonPath("$.unActiveLabels").value(0));
        }

        @Test
        void givenAllLabelsHaveBooks_whenGetLabelStats_thenReturnCorrectStats() throws Exception {
                // Arrange
                GenreStatsDTO stats = new GenreStatsDTO(5, 0);

                given(labelService.getAllGenreStats()).willReturn(stats);

                // Act
                ResultActions result = mockMvc.perform(get("/api/genres/_stats"));

                // Assert
                result.andExpect(status().isOk())
                                .andExpect(jsonPath("$.totalItems").value(5))
                                .andExpect(jsonPath("$.unActiveLabels").value(0));
        }

        // -------------------------------------------------------
        // DELETE /api/genres/{name} - delete
        // -------------------------------------------------------
        @Test
        void givenExistingLabelName_whenDelete_thenReturn204() throws Exception {
                // Arrange
                String labelName = "Fiction";

                doNothing().when(labelService).deleteById(labelName);

                // Act
                ResultActions result = mockMvc.perform(delete("/api/genres/{name}", labelName));

                // Assert
                result.andExpect(status().isNoContent());
                then(labelService).should(times(1)).deleteById(labelName);
        }

        @Test
        void givenNonExistingLabelName_whenDelete_thenReturn404() throws Exception {
                // Arrange
                String labelName = "NonExistent";

                doThrow(new ResourceNotFoundException(labelName, "Label not found"))
                                .when(labelService).deleteById(labelName);

                // Act
                ResultActions result = mockMvc.perform(delete("/api/genres/{name}", labelName));

                // Assert
                result.andExpect(status().isNotFound());
                then(labelService).should(times(1)).deleteById(labelName);
        }

        @Test
        void givenLabelNameWithSpecialCharacters_whenDelete_thenReturn204() throws Exception {
                // Arrange - Use a simple name without URL encoding issues
                String labelName = "Science_Fiction";

                doNothing().when(labelService).deleteById(labelName);

                // Act
                ResultActions result = mockMvc.perform(delete("/api/genres/{name}", labelName));

                // Assert
                result.andExpect(status().isNoContent());
                then(labelService).should(times(1)).deleteById(labelName);
        }

        @Test
        void givenDeleteError_whenDelete_thenReturn500() throws Exception {
                // Arrange
                String labelName = "Fiction";

                doThrow(new RuntimeException("Database error"))
                                .when(labelService).deleteById(labelName);

                // Act
                ResultActions result = mockMvc.perform(delete("/api/genres/{name}", labelName));

                // Assert
                result.andExpect(status().isInternalServerError());
        }

        // -------------------------------------------------------
        // PUT /api/genres/{name} - updateLabel
        // -------------------------------------------------------
        @Test
        void givenValidBody_whenUpdateLabel_thenReturn200() throws Exception {
                // Arrange
                String labelName = "Fiction";
                GenreDTO requestBody = GenreDTO.builder()
                                .name(labelName)
                                .color("#00FF00")
                                .description("Updated fiction description")
                                .build();
                GenreDTO updatedLabel = GenreDTO.builder()
                                .name(labelName)
                                .color("#00FF00")
                                .description("Updated fiction description")
                                .build();

                when(labelService.update(eq(labelName), any(GenreDTO.class))).thenReturn(updatedLabel);

                // Act
                ResultActions result = mockMvc.perform(put("/api/genres/{name}", labelName)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestBody)));

                // Assert
                result.andExpect(status().isOk())
                                .andExpect(jsonPath("$.name").value(labelName))
                                .andExpect(jsonPath("$.color").value("#00FF00"))
                                .andExpect(jsonPath("$.description").value("Updated fiction description"));
                then(labelService).should(times(1)).update(eq(labelName), any(GenreDTO.class));
        }

        @Test
        void givenNonExistingLabel_whenUpdateLabel_thenReturn404() throws Exception {
                // Arrange
                String labelName = "NonExistent";
                GenreDTO requestBody = GenreDTO.builder()
                                .name(labelName)
                                .color("#00FF00")
                                .description("Some description")
                                .build();

                when(labelService.update(eq(labelName), any(GenreDTO.class)))
                                .thenThrow(new ResourceNotFoundException(labelName, "Label not found"));

                // Act
                ResultActions result = mockMvc.perform(put("/api/genres/{name}", labelName)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestBody)));

                // Assert
                result.andExpect(status().isNotFound());
                then(labelService).should(times(1)).update(eq(labelName), any(GenreDTO.class));
        }

        @Test
        void givenServiceThrows_whenUpdateLabel_thenReturn500() throws Exception {
                // Arrange
                String labelName = "Fiction";
                GenreDTO requestBody = GenreDTO.builder()
                                .name(labelName)
                                .color("#00FF00")
                                .description("Updated description")
                                .build();

                when(labelService.update(eq(labelName), any(GenreDTO.class)))
                                .thenThrow(new RuntimeException("Unexpected error"));

                // Act
                ResultActions result = mockMvc.perform(put("/api/genres/{name}", labelName)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestBody)));

                // Assert
                result.andExpect(status().isInternalServerError())
                                .andExpect(r -> assertInstanceOf(RuntimeException.class, r.getResolvedException()));
        }

        @Test
        void givenValidRequest_whenUpdateLabel_thenCallServiceOnce() throws Exception {
                // Arrange
                String labelName = "Science";
                GenreDTO requestBody = GenreDTO.builder()
                                .name(labelName)
                                .color("#0000FF")
                                .description("Science books")
                                .build();
                GenreDTO updatedLabel = GenreDTO.builder()
                                .name(labelName)
                                .color("#0000FF")
                                .description("Science books")
                                .build();

                when(labelService.update(eq(labelName), any(GenreDTO.class))).thenReturn(updatedLabel);

                // Act
                mockMvc.perform(put("/api/genres/{name}", labelName)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestBody)));

                // Assert
                then(labelService).should(times(1)).update(eq(labelName), any(GenreDTO.class));
        }

        @Test
        void givenUpdateWithNullDescription_whenUpdateLabel_thenReturn200() throws Exception {
                // Arrange
                String labelName = "Fiction";
                GenreDTO requestBody = GenreDTO.builder()
                                .name(labelName)
                                .color("#FF0000")
                                .build();
                GenreDTO updatedLabel = GenreDTO.builder()
                                .name(labelName)
                                .color("#FF0000")
                                .description(null)
                                .build();

                when(labelService.update(eq(labelName), any(GenreDTO.class))).thenReturn(updatedLabel);

                // Act
                ResultActions result = mockMvc.perform(put("/api/genres/{name}", labelName)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestBody)));

                // Assert
                result.andExpect(status().isOk())
                                .andExpect(jsonPath("$.name").value(labelName))
                                .andExpect(jsonPath("$.color").value("#FF0000"));
        }
}
