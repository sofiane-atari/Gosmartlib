package edu.ap.bib_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ap.bib_backend.dto.SchoolDTO;
import edu.ap.bib_backend.exception.ResourceAlreadyExistsException;
import edu.ap.bib_backend.service.AdminOverrideService;
import edu.ap.bib_backend.service.SchoolService;
import edu.ap.bib_backend.service.oneroster.OneRosterAudienceService;
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

        // @Test
        // void givenSchools_whenGetAll_thenReturnOkWithList() throws Exception {
        //         SchoolDTO school = new SchoolDTO("ap.be", "AP", "Noorderplaats 2", 2000);
        //         when(schoolService.getAll()).thenReturn(List.of(school));

        //         mockMvc.perform(get("/schools"))
        //                         .andExpect(status().isOk())
        //                         .andExpect(jsonPath("$[0].domainName").value("ap.be"))
        //                         .andExpect(jsonPath("$[0].name").value("AP"));
        // }

        // @Test
        // void givenValidSchool_whenCreate_thenReturnCreatedWithLocation() throws Exception {
        //         SchoolDTO request = new SchoolDTO("ap.be", "AP", "Noorderplaats 2", 2000);
        //         when(schoolService.createSchool(any(SchoolDTO.class))).thenReturn(request);

        //         mockMvc.perform(post("/schools")
        //                         .contentType(MediaType.APPLICATION_JSON)
        //                         .content(objectMapper.writeValueAsString(request)))
        //                         .andExpect(status().isCreated())
        //                         .andExpect(header().string("Location", "http://localhost/schools/ap.be"))
        //                         .andExpect(jsonPath("$.domainName").value("ap.be"));
        // }

        // @Test
        // void givenInvalidBody_whenCreate_thenReturnBadRequest() throws Exception {
        //         SchoolDTO request = new SchoolDTO("", "", "", 2000);

        //         mockMvc.perform(post("/schools")
        //                         .contentType(MediaType.APPLICATION_JSON)
        //                         .content(objectMapper.writeValueAsString(request)))
        //                         .andExpect(status().isBadRequest());
        // }

        // @Test
        // void givenDuplicateSchool_whenCreate_thenReturnConflict() throws Exception {
        //         SchoolDTO request = new SchoolDTO("ap.be", "AP", "Noorderplaats 2", 2000);
        //         when(schoolService.createSchool(any(SchoolDTO.class))).thenThrow(
        //                         new ResourceAlreadyExistsException("domainName", "ap.be", "School bestaat al."));

        //         mockMvc.perform(post("/schools")
        //                         .contentType(MediaType.APPLICATION_JSON)
        //                         .content(objectMapper.writeValueAsString(request)))
        //                         .andExpect(status().isConflict());
        // }

        // @Test
        // void givenValidSchool_whenUpdate_thenReturnOkWithUpdatedBody() throws Exception {
        //         SchoolDTO request = new SchoolDTO("ap.be", "AP Campus Noord", "Ellermanstraat 33", 2030);
        //         when(schoolService.updateSchool(eq("ap.be"), any(SchoolDTO.class))).thenReturn(request);

        //         mockMvc.perform(put("/schools/ap.be")
        //                         .contentType(MediaType.APPLICATION_JSON)
        //                         .content(objectMapper.writeValueAsString(request)))
        //                         .andExpect(status().isOk())
        //                         .andExpect(jsonPath("$.domainName").value("ap.be"))
        //                         .andExpect(jsonPath("$.name").value("AP Campus Noord"));
        // }

        @Test
        void givenInvalidBody_whenUpdate_thenReturnBadRequest() throws Exception {
                String invalidRequestJson = """
                                {
                                  "domainName": "ap.be",
                                  "name": "",
                                  "address": "",
                                  "postalCode": 0
                                }
                                """;

                mockMvc.perform(put("/schools/ap.be")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidRequestJson))
                                .andExpect(status().isBadRequest());
        }
}
