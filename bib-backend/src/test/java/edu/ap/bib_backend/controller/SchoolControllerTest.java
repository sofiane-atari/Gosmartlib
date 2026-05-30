package edu.ap.bib_backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import edu.ap.bib_backend.TestSecurityConfig;
import edu.ap.bib_backend.dto.SchoolDTO;
import edu.ap.bib_backend.dto.SchoolSummaryDto;
import edu.ap.bib_backend.service.AdminOverrideService;
import edu.ap.bib_backend.service.JwtService;
import edu.ap.bib_backend.service.SchoolService;
import edu.ap.bib_backend.service.TokenStorageService;

@WebMvcTest(SchoolController.class)
@Import(TestSecurityConfig.class)
@TestPropertySource(properties = "app.dev-mode-enabled=true")
class SchoolControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SchoolService schoolService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private AdminOverrideService adminOverrideService;

    @MockitoBean
    private TokenStorageService tokenStorageService;

    @Test
    void GivenSchoolsExist_WhenGetSchools_ThenReturn200WithSchools() throws Exception {
        // Arrange
        SchoolDTO firstSchool = schoolDto(1L, "apsmartschool.be", "AP Hogeschool", "Noorderplaats 2", 2000);
        SchoolDTO secondSchool = schoolDto(2L, "gasmartschool.be", "Geestelijke Hogeschool", "Grote Markt 1", 2200);
        given(schoolService.getAll()).willReturn(List.of(firstSchool, secondSchool));

        // Act & Assert
        mockMvc.perform(get("/schools"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("AP Hogeschool"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].name").value("Geestelijke Hogeschool"));
    }

    @Test
    void GivenNoSchoolsExist_WhenGetSchools_ThenReturn200WithEmptyList() throws Exception {
        // Arrange
        given(schoolService.getAll()).willReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/schools"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void GivenValidSchool_WhenCreateSchool_ThenReturn201() throws Exception {
        // Arrange
        SchoolDTO school = schoolDto(null, "newschool.be", "Nieuwe School", "Nieuwstraat 1", 1000);
        SchoolDTO savedSchool = schoolDto(3L, "newschool.be", "Nieuwe School", "Nieuwstraat 1", 1000);
        given(schoolService.create(any(SchoolDTO.class))).willReturn(savedSchool);

        // Act & Assert
        mockMvc.perform(post("/schools")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"domainName\":\"newschool.be\",\"name\":\"Nieuwe School\",\"address\":\"Nieuwstraat 1\",\"postalCode\":1000}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.name").value("Nieuwe School"));
    }

    @Test
    void GivenExistingSchool_WhenUpdateSchool_ThenReturn200() throws Exception {
        // Arrange
        SchoolDTO updatedSchool = schoolDto(1L, "apsmartschool.be", "AP Hogeschool Gewijzigd", "Noorderplaats 2", 2000);
        given(schoolService.update(eq(1L), any(SchoolDTO.class))).willReturn(updatedSchool);

        // Act & Assert
        mockMvc.perform(put("/schools/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"domainName\":\"apsmartschool.be\",\"name\":\"AP Hogeschool Gewijzigd\",\"address\":\"Noorderplaats 2\",\"postalCode\":2000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("AP Hogeschool Gewijzigd"));
    }

    @Test
    void GivenExistingSchool_WhenDeleteSchool_ThenReturn204() throws Exception {
        // Arrange
        given(schoolService.getAll()).willReturn(List.of());

        // Act & Assert
        mockMvc.perform(delete("/schools/{id}", 1L))
                .andExpect(status().isNoContent());

        verify(schoolService, times(1)).delete(1L);
    }

    @Test
    void GivenExistingSchool_WhenGetBookCount_ThenReturnBookCount() throws Exception {
        // Arrange
        given(schoolService.getBookCountBySchool(1L)).willReturn(50L);

        // Act & Assert
        mockMvc.perform(get("/schools/{id}/books/count", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(50));
    }

    @Test
    void GivenDevModeEnabled_WhenPurgeSchool_ThenReturn204() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/schools/{id}/purge", 1L))
                .andExpect(status().isNoContent());

        verify(schoolService, times(1)).purge(1L);
    }

    @Test
    void GivenDevModeEnabled_WhenGetDevModeEnabled_ThenReturnTrue() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/schools/dev-mode-enabled"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    void WhenGetTotalBookCount_ThenReturnTotalBookCount() throws Exception {
        // Arrange
        given(schoolService.getTotalBookCount()).willReturn(100L);

        // Act & Assert
        mockMvc.perform(get("/schools/books/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(100));
    }

    @Test
    void GivenExistingSchool_WhenDisableSchool_ThenReturn200() throws Exception {
        // Arrange
        SchoolDTO disabledSchool = schoolDto(1L, "apsmartschool.be", "AP Hogeschool", "Noorderplaats 2", 2000);
        given(schoolService.disable(1L)).willReturn(disabledSchool);

        // Act & Assert
        mockMvc.perform(put("/schools/{id}/disable", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("AP Hogeschool"));

        verify(schoolService, times(1)).disable(1L);
    }

    @Test
    void GivenExistingSchool_WhenEnableSchool_ThenReturn200() throws Exception {
        // Arrange
        SchoolDTO enabledSchool = schoolDto(1L, "apsmartschool.be", "AP Hogeschool", "Noorderplaats 2", 2000);
        given(schoolService.enable(1L)).willReturn(enabledSchool);

        // Act & Assert
        mockMvc.perform(put("/schools/{id}/enable", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("AP Hogeschool"));

        verify(schoolService, times(1)).enable(1L);
    }

    @Test
    void GivenAvailableDomain_WhenCheckDomain_ThenReturn200() throws Exception {
        // Arrange - void method, no setup needed for mock

        // Act & Assert
        mockMvc.perform(get("/schools/check-domain")
                        .param("domainName", "new.school.be"))
                .andExpect(status().isOk());

        verify(schoolService, times(1)).checkDomain("new.school.be");
    }

    @Test
    void Given_ValidRequest_When_SetCatalogVisibility_Then_Returns200() throws Exception {
        mockMvc.perform(put("/schools/{id}/catalog-visibility", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visible\":true}"))
                .andExpect(status().isOk());

        verify(schoolService).setCatalogVisibility(eq(1L), eq(true), any());
    }

    @Test
    void Given_SchoolsExist_When_GetAccessibleSchools_Then_Returns200() throws Exception {
        SchoolSummaryDto school1 = new SchoolSummaryDto(1L, "AP Hogeschool", "ap.be", 15, null, true);
        SchoolSummaryDto school2 = new SchoolSummaryDto(2L, "Andere School", "andere.be", 15, null, true);
        given(schoolService.getAccessibleSchools(any())).willReturn(List.of(school1, school2));

        mockMvc.perform(get("/schools/accessible"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("AP Hogeschool"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].name").value("Andere School"));

        verify(schoolService).getAccessibleSchools(any());
    }

    private SchoolDTO schoolDto(
            Long id,
            String domainName,
            String name,
            String address,
            Integer postalCode) {
        return new SchoolDTO(
                id,
                domainName,
                name,
                15,
                java.util.List.of(),
                LocalDateTime.of(2026, 4, 28, 12, 0),
                false,
                null,
                null,
                null,
                null,
                null);
    }
}
