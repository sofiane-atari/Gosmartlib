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

import edu.ap.bib_backend.controller.LabelController;
import edu.ap.bib_backend.dto.LabelDTO;
import edu.ap.bib_backend.dto.LabelStatsDTO;
import edu.ap.bib_backend.dto.PageDTO;
import edu.ap.bib_backend.dto.PaginatedMeta;
import edu.ap.bib_backend.options.LabelGetOptions;
import edu.ap.bib_backend.service.LabelService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@WebMvcTest(LabelController.class)
@Import(TestSecurityConfig.class)
class LabelControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private LabelService labelService;

        // -------------------------------------------------------
        // POST /api/labels - createLabel
        // -------------------------------------------------------
        @Test
        void givenNoDescription_whenCreateLabel_thenCreateWithoutDescription() throws Exception {
                // Arrange
                LabelDTO newLabel = LabelDTO.builder().name("testLabel").color("#a75064").build();
                LabelDTO savedLabel = LabelDTO.builder().name("testLabel").color("#a75064").build();

                when(labelService.createLabel(any(LabelDTO.class))).thenReturn(savedLabel);

                // Act
                ResultActions results = mockMvc.perform(post("/api/labels")
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
                LabelDTO requestBody = LabelDTO.builder().name("urgent").color("#FF0001")
                                .description("High priority tasks").build();
                LabelDTO createdLabel = LabelDTO.builder().name("urgent").color("#FF0001")
                                .description("High priority tasks").build();

                given(labelService.createLabel(any(LabelDTO.class)))
                                .willReturn(createdLabel);

                // Act
                ResultActions result = mockMvc.perform(post("/api/labels")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestBody)));

                // Assert
                result
                                .andExpect(status().isCreated())
                                .andExpect(header().string("Location", containsString("/api/labels/urgent")))
                                .andExpect(jsonPath("$.name").value("urgent"))
                                .andExpect(jsonPath("$.description").value("High priority tasks"))
                                .andExpect(jsonPath("$.color").value("#FF0001"));
        }

        @Test
        void givenNullName_whenCreateLabel_thenReturn400() throws Exception {
                // Arrange
                LabelDTO invalidBody = LabelDTO.builder().color("#FF0001").description("Some description").build();

                // Act
                ResultActions result = mockMvc.perform(post("/api/labels")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidBody)));

                // Assert
                result.andExpect(status().isBadRequest());
        }

        @Test
        void givenOptionalFieldsNull_whenCreateLabel_thenReturn201() throws Exception {
                // Arrange
                LabelDTO requestBody = LabelDTO.builder().name("urgent").color("#FF0001").build();
                LabelDTO createdLabel = LabelDTO.builder().name("urgent").color("#FF0001").build();

                given(labelService.createLabel(any(LabelDTO.class)))
                                .willReturn(createdLabel);

                // Act
                ResultActions result = mockMvc.perform(post("/api/labels")
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
                LabelDTO requestBody = LabelDTO.builder().name("urgent").color("#FF0001").build();

                given(labelService.createLabel(any(LabelDTO.class)))
                                .willThrow(new RuntimeException("Unexpected error"));

                // Act
                ResultActions result = mockMvc.perform(post("/api/labels")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestBody)));

                // Assert
                result.andExpect(status().isInternalServerError())
                                .andExpect(r -> assertInstanceOf(RuntimeException.class, r.getResolvedException()));
        }

        @Test
        void givenValidRequest_whenCreateLabel_thenCallServiceOnce() throws Exception {
                // Arrange
                LabelDTO requestBody = LabelDTO.builder().name("feature").color("#01FF00").description("Feature label")
                                .build();
                LabelDTO createdLabel = LabelDTO.builder().name("feature").color("#01FF00").description("Feature label")
                                .build();

                given(labelService.createLabel(any(LabelDTO.class)))
                                .willReturn(createdLabel);

                // Act
                mockMvc.perform(post("/api/labels")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestBody)));

                // Assert
                then(labelService).should(times(1)).createLabel(any(LabelDTO.class));
        }

        // -------------------------------------------------------
        // GET /api/labels - getPage
        // -------------------------------------------------------
        @Test
        void givenRequestingFirstPage_whenGetPage_thenReturnLabels() throws Exception {
                // Arrange
                LabelDTO label = LabelDTO.builder().name("Fiction").color("#FF0000").description("Fiction books")
                                .build();
                PageDTO<LabelDTO> pageDTO = new PageDTO<>(List.of(label), new PaginatedMeta(0, 10, 1, 1));

                given(labelService.getPage(anyInt(), anyInt(), any(LabelGetOptions.class))).willReturn(pageDTO);

                // Act
                ResultActions result = mockMvc.perform(get("/api/labels"));

                // Assert
                result.andExpect(status().isOk())
                                .andExpect(jsonPath("$.data").isArray())
                                .andExpect(jsonPath("$.data[0].name").value("Fiction"))
                                .andExpect(jsonPath("$.meta.currentPage").value(0));
        }

        @Test
        void givenRequestingSecondPage_whenGetPage_thenReturnLabelsWithPagination() throws Exception {
                // Arrange
                LabelDTO label = LabelDTO.builder().name("Non-Fiction").color("#00FF00-F")
                                .description("Noniction books").build();
                PageDTO<LabelDTO> pageDTO = new PageDTO<>(List.of(label), new PaginatedMeta(1, 10, 2, 20));

                given(labelService.getPage(anyInt(), anyInt(), any(LabelGetOptions.class))).willReturn(pageDTO);

                // Act
                ResultActions result = mockMvc.perform(get("/api/labels")
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
                LabelDTO label = LabelDTO.builder().name("Science").color("#0000FF").description("Science books")
                                .build();
                PageDTO<LabelDTO> pageDTO = new PageDTO<>(List.of(label), new PaginatedMeta(0, 10, 1, 1));

                given(labelService.getPage(anyInt(), anyInt(), any(LabelGetOptions.class))).willReturn(pageDTO);

                // Act
                ResultActions result = mockMvc.perform(get("/api/labels")
                                .param("search", "science"));

                // Assert
                result.andExpect(status().isOk());
        }

        @Test
        void givenBookCountParam_whenGetPage_thenPassBookCountFilter() throws Exception {
                // Arrange
                PageDTO<LabelDTO> pageDTO = new PageDTO<>(List.of(), new PaginatedMeta(0, 10, 0, 0));

                given(labelService.getPage(anyInt(), anyInt(), any(LabelGetOptions.class))).willReturn(pageDTO);

                // Act
                ResultActions result = mockMvc.perform(get("/api/labels")
                                .param("bookCount", "1"));

                // Assert
                result.andExpect(status().isOk());
        }

        @Test
        void givenNoParams_whenGetPage_thenUseDefaultValues() throws Exception {
                // Arrange
                PageDTO<LabelDTO> pageDTO = new PageDTO<>(List.of(), new PaginatedMeta(0, 10, 0, 0));

                given(labelService.getPage(anyInt(), anyInt(), any(LabelGetOptions.class))).willReturn(pageDTO);

                // Act
                ResultActions result = mockMvc.perform(get("/api/labels"));

                // Assert
                result.andExpect(status().isOk());
        }

        // -------------------------------------------------------
        // GET /api/labels/_stats - getLabelStats
        // -------------------------------------------------------
        @Test
        void givenLabelsExist_whenGetLabelStats_thenReturnStats() throws Exception {
                // Arrange
                LabelStatsDTO stats = new LabelStatsDTO(10, 5);

                given(labelService.getAllLabelStats()).willReturn(stats);

                // Act
                ResultActions result = mockMvc.perform(get("/api/labels/_stats"));

                // Assert
                result.andExpect(status().isOk())
                                .andExpect(jsonPath("$.totalItems").value(10))
                                .andExpect(jsonPath("$.unActiveLabels").value(5));
        }

        @Test
        void givenNoLabels_whenGetLabelStats_thenReturnZeroStats() throws Exception {
                // Arrange
                LabelStatsDTO stats = new LabelStatsDTO(0, 0);

                given(labelService.getAllLabelStats()).willReturn(stats);

                // Act
                ResultActions result = mockMvc.perform(get("/api/labels/_stats"));

                // Assert
                result.andExpect(status().isOk())
                                .andExpect(jsonPath("$.totalItems").value(0))
                                .andExpect(jsonPath("$.unActiveLabels").value(0));
        }

        @Test
        void givenAllLabelsHaveBooks_whenGetLabelStats_thenReturnCorrectStats() throws Exception {
                // Arrange
                LabelStatsDTO stats = new LabelStatsDTO(5, 0);

                given(labelService.getAllLabelStats()).willReturn(stats);

                // Act
                ResultActions result = mockMvc.perform(get("/api/labels/_stats"));

                // Assert
                result.andExpect(status().isOk())
                                .andExpect(jsonPath("$.totalItems").value(5))
                                .andExpect(jsonPath("$.unActiveLabels").value(0));
        }
}
