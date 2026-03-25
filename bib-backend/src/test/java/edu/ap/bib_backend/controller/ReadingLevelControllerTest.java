package edu.ap.bib_backend.controller;

import edu.ap.bib_backend.jpa.entity.ReadingLevel;
import edu.ap.bib_backend.jpa.repository.ReadingLevelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ReadingLevelControllerTest {

    private MockMvc mockMvc;
    private ReadingLevelRepository readingLevelRepository;

    @BeforeEach
    void setUp() {
        readingLevelRepository = mock(ReadingLevelRepository.class);
        ReadingLevelController controller = new ReadingLevelController(readingLevelRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("Given reading levels exist, when getAllReadingLevels is called, then return all reading levels")
    void givenReadingLevelsExist_whenGetAllReadingLevels_thenReturnAllReadingLevels() throws Exception {
        // Given
        ReadingLevel levelA = new ReadingLevel("A", "eenvoudig taalgebruik");
        ReadingLevel levelB = new ReadingLevel("B", "gemiddeld taalgebruik");

        given(readingLevelRepository.findAll()).willReturn(List.of(levelA, levelB));

        // When & Then
        mockMvc.perform(get("/reading-levels"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].niveauName").value("A"))
                .andExpect(jsonPath("$[1].niveauName").value("B"));

        then(readingLevelRepository).should().findAll();
    }

    @Test
    @DisplayName("Given no reading levels exist, when getAllReadingLevels is called, then return empty list")
    void givenNoReadingLevelsExist_whenGetAllReadingLevels_thenReturnEmptyList() throws Exception {
        // Given
        given(readingLevelRepository.findAll()).willReturn(List.of());

        // When & Then
        mockMvc.perform(get("/reading-levels"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        then(readingLevelRepository).should().findAll();
    }
}