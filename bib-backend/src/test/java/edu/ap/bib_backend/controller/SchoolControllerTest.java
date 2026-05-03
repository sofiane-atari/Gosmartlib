package edu.ap.bib_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ap.bib_backend.dto.SchoolDTO;
import edu.ap.bib_backend.exception.ResourceAlreadyExistsException;
import edu.ap.bib_backend.service.AdminOverrideService;
import edu.ap.bib_backend.service.JwtService;
import edu.ap.bib_backend.service.SchoolService;
import edu.ap.bib_backend.service.TokenStorageService;
import edu.ap.bib_backend.service.oneroster.OneRosterAudienceService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SchoolController.class)
@AutoConfigureMockMvc(addFilters = false)
class SchoolControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private SchoolService schoolService;

        @MockitoBean
        private edu.ap.bib_backend.service.JwtService jwtService;

        @MockitoBean
        private AdminOverrideService adminOverrideService;

        @MockitoBean
        private OneRosterAudienceService oneRosterAudienceService;


        @MockitoBean
        private TokenStorageService tokenStorageService;



        @Test
        void givenSchools_whenGetAll_thenReturnOkWithList() throws Exception {
                SchoolDTO school = schoolDto(
                                1L,
                                "ap.be",
                                "AP",
                                "Noorderplaats 2",
                                2000);

                when(schoolService.getAll()).thenReturn(List.of(school));

                mockMvc.perform(get("/schools"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].id").value(1))
                                .andExpect(jsonPath("$[0].domainName").value("ap.be"))
                                .andExpect(jsonPath("$[0].name").value("AP"))
                                .andExpect(jsonPath("$[0].address").value("Noorderplaats 2"))
                                .andExpect(jsonPath("$[0].postalCode").value(2000));
        }

        @Test
        void givenValidSchool_whenCreate_thenReturnCreatedWithLocation() throws Exception {
                SchoolDTO request = schoolDto(
                                null,
                                "ap.be",
                                "AP",
                                "Noorderplaats 2",
                                2000);

                SchoolDTO response = schoolDto(
                                1L,
                                "ap.be",
                                "AP",
                                "Noorderplaats 2",
                                2000);

                when(schoolService.create(any(SchoolDTO.class))).thenReturn(response);

                mockMvc.perform(post("/schools")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(header().string("Location", "/schools/1"))
                                .andExpect(jsonPath("$.id").value(1))
                                .andExpect(jsonPath("$.domainName").value("ap.be"));
        }

        @Test
        void givenInvalidBody_whenCreate_thenReturnBadRequest() throws Exception {
                SchoolDTO request = schoolDto(
                                null,
                                "",
                                "",
                                "",
                                2000);

                mockMvc.perform(post("/schools")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void givenDuplicateSchool_whenCreate_thenReturnConflict() throws Exception {
                SchoolDTO request = schoolDto(
                                null,
                                "ap.be",
                                "AP",
                                "Noorderplaats 2",
                                2000);

                when(schoolService.create(any(SchoolDTO.class))).thenThrow(
                                new ResourceAlreadyExistsException("domainName", "ap.be", "School bestaat al."));

                mockMvc.perform(post("/schools")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isConflict());
        }

        @Test
        void givenValidSchool_whenUpdate_thenReturnOkWithUpdatedBody() throws Exception {
                SchoolDTO request = schoolDto(
                                null,
                                "ap.be",
                                "AP Campus Noord",
                                "Ellermanstraat 33",
                                2030);

                SchoolDTO response = schoolDto(
                                1L,
                                "ap.be",
                                "AP Campus Noord",
                                "Ellermanstraat 33",
                                2030);

                when(schoolService.update(eq(1L), any(SchoolDTO.class))).thenReturn(response);

                mockMvc.perform(put("/schools/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(1))
                                .andExpect(jsonPath("$.domainName").value("ap.be"))
                                .andExpect(jsonPath("$.name").value("AP Campus Noord"))
                                .andExpect(jsonPath("$.address").value("Ellermanstraat 33"))
                                .andExpect(jsonPath("$.postalCode").value(2030));
        }

        @Test
        void givenInvalidBody_whenUpdate_thenReturnBadRequest() throws Exception {
                String invalidRequestJson = """
                                {
                                  "domainName": "ap.be",
                                  "name": "",
                                  "address": "",
                                  "postalCode": 0,
                                  "contactPhone": "",
                                  "contactEmail": ""
                                }
                                """;

                mockMvc.perform(put("/schools/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidRequestJson))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void givenSchoolId_whenGetBookCountBySchool_thenReturnOkWithCount() throws Exception {
                when(schoolService.getBookCountBySchool(1L)).thenReturn(12L);

                mockMvc.perform(get("/schools/1/books/count"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").value(12));
        }

        @Test
        void whenGetTotalBookCount_thenReturnOkWithCount() throws Exception {
                when(schoolService.getTotalBookCount()).thenReturn(50L);

                mockMvc.perform(get("/schools/books/count"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").value(50));
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
                                address,
                                postalCode,
                                "03 123 45 67",
                                "info@ap.be",
                                LocalDateTime.of(2026, 4, 28, 12, 0));
        }
}