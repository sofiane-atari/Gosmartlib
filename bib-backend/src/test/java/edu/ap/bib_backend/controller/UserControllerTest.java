package edu.ap.bib_backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import edu.ap.bib_backend.TestSecurityConfig;
import edu.ap.bib_backend.dto.AuthUserDto;
import edu.ap.bib_backend.jpa.enums.Role;
import edu.ap.bib_backend.options.UserGetOptions;
import edu.ap.bib_backend.service.AdminOverrideService;
import edu.ap.bib_backend.service.JwtService;
import edu.ap.bib_backend.service.TokenStorageService;
import edu.ap.bib_backend.service.user.CurrentUserService;
import edu.ap.bib_backend.service.user.UserService;

@WebMvcTest(UserController.class)
@Import(TestSecurityConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private CurrentUserService currentUserService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private TokenStorageService tokenStorageService;

    @MockitoBean
    private AdminOverrideService adminOverrideService;

    @Test
    void GivenUsersExist_WhenGetUsers_ThenReturn200WithUsers() throws Exception {
        // Arrange
        AuthUserDto firstUser = new AuthUserDto("s001", null, null, null, Role.STUDENT, null, null, null, null, null, false);
        AuthUserDto secondUser = new AuthUserDto("t001", null, null, null, Role.TEACHER, null, null, null, null, null, true);
        List<AuthUserDto> response = List.of(firstUser, secondUser);
        given(userService.getUsers(any(UserGetOptions.class))).willReturn(response);

        // Act
        ResultActions result = mockMvc.perform(get("/users"));

        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("s001"))
                .andExpect(jsonPath("$[0].role").value("STUDENT"))
                .andExpect(jsonPath("$[0].reviewBlocked").value(false))
                .andExpect(jsonPath("$[1].id").value("t001"))
                .andExpect(jsonPath("$[1].role").value("TEACHER"))
                .andExpect(jsonPath("$[1].reviewBlocked").value(true));
    }

    @Test
    void GivenNoUsersExist_WhenGetUsers_ThenReturn200WithEmptyList() throws Exception {
        // Arrange
        given(userService.getUsers(any(UserGetOptions.class))).willReturn(List.of());

        // Act
        ResultActions result = mockMvc.perform(get("/users"));

        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void GivenIdFilterInQuery_WhenGetUsers_ThenPassOptionsToService() throws Exception {
        // Arrange
        given(userService.getUsers(any(UserGetOptions.class))).willReturn(List.of());

        // Act
        mockMvc.perform(get("/users").param("id", "abc"));

        // Assert
        then(userService).should(times(1)).getUsers(argThat(options -> "abc".equals(options.getId())));
    }

    @Test
    void GivenModerator_WhenPatchReviewAccess_ThenReturnUpdatedUser() throws Exception {
        // Arrange
        AuthUserDto updated = new AuthUserDto("s001", null, null, null, Role.STUDENT, null, null, null, null, null, true);

        given(currentUserService.isModerator(any())).willReturn(true);
        given(userService.setReviewBlocked("s001", true)).willReturn(updated);

        // Act
        ResultActions result = mockMvc.perform(
                patch("/users/{userId}/review-access", "s001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"blocked\":true}")
        );

        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("s001"))
                .andExpect(jsonPath("$.reviewBlocked").value(true));

        then(userService).should(times(1)).setReviewBlocked(eq("s001"), eq(true));
    }
}