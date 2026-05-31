package edu.ap.bib_backend.controller;

import edu.ap.bib_backend.dto.AssignLibrarianRequestDto;
import edu.ap.bib_backend.dto.AuthUserDto;
import edu.ap.bib_backend.jpa.repository.SchoolRepository;
import edu.ap.bib_backend.service.ActiveSchoolContext;
import edu.ap.bib_backend.service.AdminOverrideService;
import edu.ap.bib_backend.service.JwtService;
import edu.ap.bib_backend.service.RoleService;
import edu.ap.bib_backend.service.TokenStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RoleController.class)
@AutoConfigureMockMvc(addFilters = false)
class RoleControllerTest {

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
    void Given_AssignLibrarianRequest_When_AssignLibrarian_Then_ReturnsUser() throws Exception {
        // Arrange
        AuthUserDto response = AuthUserDto.builder().id("user-1").build();
        when(roleService.assignLibrarian(any(), any(), any())).thenReturn(response);

        // Act
        mockMvc.perform(post("/roles/assign-librarian")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"user-1\"}"))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("user-1"));
    }

    @Test
    void Given_RevokeLibrarianRequest_When_RevokeLibrarian_Then_ReturnsUser() throws Exception {
        // Arrange
        AuthUserDto response = AuthUserDto.builder().id("user-2").build();
        when(roleService.revokeLibrarian(any(), any(), any())).thenReturn(response);

        // Act
        mockMvc.perform(post("/roles/revoke-librarian")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"user-2\"}"))
                // Assert
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("user-2"));
    }
}
