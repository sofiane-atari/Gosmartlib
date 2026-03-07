package edu.ap.bib_backend;

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
import edu.ap.bib_backend.service.LabelService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
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
        void createLabel_ShouldCreateLabelWithoutDescription() throws Exception {
                // Arranage
                String name = "testLabel";
                LabelDTO newLabel = new LabelDTO(name, "#a75064", null);
                LabelDTO savedLabel = new LabelDTO(name, "#a75064", null);

                when(labelService.createLabel(any(LabelDTO.class))).thenReturn(savedLabel);

                // Assert
                ResultActions results = mockMvc.perform(post("/api/labels")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper
                                                .writeValueAsString(newLabel)));

                // Act
                results
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.name").value(savedLabel.name()))
                                .andExpect(jsonPath("$.color").value(savedLabel.color()))
                                .andExpect(jsonPath("$.description").value(savedLabel.description()));
        }

        @Test
        void createLabel_WithValidBody_ShouldReturn201WithLocationHeader() throws Exception {
                // Arrange
                LabelDTO requestBody = new LabelDTO("urgent", "#FF0001", "High priority tasks");
                LabelDTO createdLabel = new LabelDTO("urgent", "#FF0001", "High priority tasks");

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
        void createLabel_WithNullName_ShouldReturn400() throws Exception {
                // Arrange
                LabelDTO invalidBody = new LabelDTO(null, "#FF0001", "Some description");

                // Act
                ResultActions result = mockMvc.perform(post("/api/labels")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidBody)));

                // Assert
                result.andExpect(status().isBadRequest());
        }

        @Test
        void createLabel_WithOptionalFieldsNull_ShouldReturn202() throws Exception {
                // Arrange
                LabelDTO requestBody = new LabelDTO("urgent", "#FF0001", null);
                LabelDTO createdLabel = new LabelDTO("urgent", "#FF0001", null);

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
        void createLabel_WhenServiceThrows_ShouldReturn501() throws Exception {
                // Arrange
                LabelDTO requestBody = new LabelDTO("urgent", "#FF0001", null);

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
        void createLabel_ShouldCallServiceExactlyOnce() throws Exception {
                // Arrange
                LabelDTO requestBody = new LabelDTO("feature", "#01FF00", "Feature label");
                LabelDTO createdLabel = new LabelDTO("feature", "#01FF00", "Feature label");

                given(labelService.createLabel(any(LabelDTO.class)))
                                .willReturn(createdLabel);

                // Act
                mockMvc.perform(post("/api/labels")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestBody)));

                // Assert
                then(labelService).should(times(1)).createLabel(any(LabelDTO.class));
        }
}