package edu.ap.bib_backend.controller;

import edu.ap.bib_backend.dto.AuthUserDto;
import edu.ap.bib_backend.dto.PageDTO;
import edu.ap.bib_backend.dto.PaginatedMeta;
import edu.ap.bib_backend.dto.TeacherStatsDTO;
import edu.ap.bib_backend.jpa.repository.SchoolRepository;
import edu.ap.bib_backend.service.AdminOverrideService;
import edu.ap.bib_backend.service.ActiveSchoolContext;
import edu.ap.bib_backend.service.JwtService;
import edu.ap.bib_backend.service.user.UserService;
import edu.ap.bib_backend.service.user.CurrentUserService;
import edu.ap.bib_backend.service.TokenStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for simplicity in this turn
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private CurrentUserService currentUserService;

    @MockitoBean
    private TokenStorageService tokenStorageService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private AdminOverrideService adminOverrideService;

    @MockitoBean
    private ActiveSchoolContext activeSchoolContext;

    @MockitoBean
    private SchoolRepository schoolRepository;

    @Test
    void Given_NoOptions_When_GetUsers_Then_ReturnsOk() throws Exception {
        // Arrange
        when(userService.getUsers(any())).thenReturn(Collections.emptyList());

        // Act
        var result = mockMvc.perform(get("/users"));

        // Assert
        result.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void Given_Moderator_When_UpdateReviewAccess_Then_ReturnsUpdatedUser() throws Exception {
        // Arrange
        AuthUserDto updatedUser = AuthUserDto.builder().id("user123").reviewBlocked(true).build();

        when(currentUserService.isModerator(any())).thenReturn(true);
        when(userService.setReviewBlocked(eq("user123"), eq(true))).thenReturn(updatedUser);

        // Act
        var result = mockMvc.perform(patch("/users/user123/review-access")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"blocked\": true}"));

        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewBlocked").value(true));
    }

    @Test
    void Given_Authentication_When_GetTeacherStats_Then_ReturnsStats() throws Exception {
        // Arrange
        when(userService.getTeacherStats(any(Authentication.class)))
                .thenReturn(new TeacherStatsDTO(5, 4, 1, 0));

        // Act
        var result = mockMvc.perform(get("/users/teachers/stats")
                .principal(new UsernamePasswordAuthenticationToken("user", null)));

        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTeachers").value(5))
                .andExpect(jsonPath("$.activeTeachers").value(4))
                .andExpect(jsonPath("$.librarians").value(1));
    }

    @Test
    void Given_QueryParams_When_GetTeachers_Then_ReturnsPaginatedTeachers() throws Exception {
        // Arrange
        AuthUserDto teacher = AuthUserDto.builder().id("t1").fullName("Alice").build();
        PageDTO<AuthUserDto> page = new PageDTO<>(List.of(teacher), new PaginatedMeta(0, 20, 1, 1));
        when(userService.getTeachers(any(Authentication.class), eq(true), eq(true), eq(0), eq(20), eq("alice")))
                .thenReturn(page);

        // Act
        var result = mockMvc.perform(get("/users/teachers")
                .param("excludeLibrarians", "true")
                .param("excludeAdmins", "true")
                .param("page", "0")
                .param("pageSize", "20")
                .param("search", "alice")
                .principal(new UsernamePasswordAuthenticationToken("user", null)));

        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("t1"))
                .andExpect(jsonPath("$.meta.totalItems").value(1));
    }

    @Test
    void Given_QueryParams_When_SearchExternalUsers_Then_ReturnsUsers() throws Exception {
        // Arrange
        AuthUserDto teacher = AuthUserDto.builder().id("t1").fullName("Alice").build();
        when(userService.searchExternalUsers(any(Authentication.class), eq(2L), eq("Alice"), eq("Teacher")))
                .thenReturn(List.of(teacher));

        // Act
        var result = mockMvc.perform(get("/users/search-external")
                .param("schoolId", "2")
                .param("firstName", "Alice")
                .param("lastName", "Teacher")
                .principal(new UsernamePasswordAuthenticationToken("user", null)));

        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("t1"));
    }

    @Test
    void Given_UserWithRefreshToken_When_GetNotificationStatus_Then_ReturnsStatus() throws Exception {
        // Arrange
        when(tokenStorageService.hasValidSmartschoolRefreshToken("user123")).thenReturn(true);

        // Act
        var result = mockMvc.perform(get("/users/user123/notification-status"));

        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.hasValidRefreshToken").value(true));
    }
}
