package edu.ap.bib_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ap.bib_backend.config.JwtAuthenticationFilter;
import edu.ap.bib_backend.config.SecurityConfig;
import edu.ap.bib_backend.dto.AdminOverrideStatusDto;
import edu.ap.bib_backend.service.AdminOverrideService;
import edu.ap.bib_backend.service.JwtService;
import edu.ap.bib_backend.service.SmartschoolOAuth2UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminOverrideController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
@ActiveProfiles("dev")
@TestPropertySource(properties = {
        "FRONTEND_REDIRECT_URL=http://localhost:4200/login",
        "admin.override.enabled=true"
})
class AdminOverrideControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AdminOverrideService adminOverrideService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private SmartschoolOAuth2UserService smartschoolOAuth2UserService;

    @Test
    @WithMockUser(username = "student-1", authorities = {"ROLE_STUDENT"})
    void givenAuthenticatedUser_whenGetStatus_thenReturnCurrentStatus() throws Exception {
        when(adminOverrideService.getStatus("student-1"))
                .thenReturn(new AdminOverrideStatusDto(true, 120L));

        mockMvc.perform(get("/auth/admin-override/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.expiresInSeconds").value(120));
    }

    @Test
    @WithMockUser(username = "student-2", authorities = {"ROLE_STUDENT"})
    void givenAuthenticatedUser_whenToggleOverride_thenReturnUpdatedStatus() throws Exception {
        when(adminOverrideService.setOverride(eq("student-2"), eq(true), startsWith("127.")))
                .thenReturn(new AdminOverrideStatusDto(true, 1800L));

        mockMvc.perform(post("/auth/admin-override")
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TogglePayload(true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.expiresInSeconds").value(1800));

        verify(adminOverrideService).setOverride(eq("student-2"), eq(true), startsWith("127."));
    }

    private record TogglePayload(boolean enabled) {
    }
}

