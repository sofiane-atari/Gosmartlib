package edu.ap.bib_backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.ap.bib_backend.dto.AuthUserDto;
import edu.ap.bib_backend.service.ActiveSchoolContext;
import edu.ap.bib_backend.service.AdminOverrideService;
import edu.ap.bib_backend.service.JwtService;
import edu.ap.bib_backend.service.RoleService;
import edu.ap.bib_backend.service.TokenStorageService;
import edu.ap.bib_backend.jpa.repository.SchoolRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminPromotionController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "app.dev-mode-enabled=true")
class AdminPromotionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RoleService roleService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private TokenStorageService tokenStorageService;

    @MockitoBean
    private AdminOverrideService adminOverrideService;

    @MockitoBean
    private ActiveSchoolContext activeSchoolContext;

    @MockitoBean
    private SchoolRepository schoolRepository;

    @Test
    @DisplayName("given dev mode enabled when assign-admin then returns promoted user")
    void givenDevModeEnabled_whenAssignAdmin_thenReturnsPromotedUser() throws Exception {
        AuthUserDto response = AuthUserDto.builder().id("admin-1").build();
        when(roleService.assignAdmin(any(), anyString(), anyString())).thenReturn(response);

        mockMvc.perform(post("/roles/assign-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"admin-1\",\"schoolCode\":\"test.smartschool.be\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("admin-1"));

        verify(roleService).assignAdmin(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("given dev mode enabled when revoke-admin then returns demoted user")
    void givenDevModeEnabled_whenRevokeAdmin_thenReturnsDemotedUser() throws Exception {
        AuthUserDto response = AuthUserDto.builder().id("admin-2").build();
        when(roleService.revokeAdmin(any(), anyString(), anyString())).thenReturn(response);

        mockMvc.perform(post("/roles/revoke-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"admin-2\",\"schoolCode\":\"test.smartschool.be\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("admin-2"));

        verify(roleService).revokeAdmin(any(), anyString(), anyString());
    }
}
