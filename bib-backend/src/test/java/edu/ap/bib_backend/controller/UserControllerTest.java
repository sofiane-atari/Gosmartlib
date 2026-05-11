package edu.ap.bib_backend.controller;

import edu.ap.bib_backend.dto.AuthUserDto;
import edu.ap.bib_backend.dto.UserReviewAccessUpdateDto;
import edu.ap.bib_backend.service.user.UserService;
import edu.ap.bib_backend.service.user.CurrentUserService;
import edu.ap.bib_backend.service.TokenStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

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

    @Test
    void Given_NoOptions_When_GetUsers_Then_ReturnsOk() throws Exception {
        // Arrange
        when(userService.getUsers(any())).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void Given_UserId_When_HasRefreshToken_Then_ReturnsBoolean() throws Exception {
        // Arrange
        when(tokenStorageService.hasValidSmartschoolRefreshToken("user123")).thenReturn(true);

        // Act & Assert
        mockMvc.perform(get("/users/has-refresh-token").param("userId", "user123"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void Given_Moderator_When_UpdateReviewAccess_Then_ReturnsUpdatedUser() throws Exception {
        // Arrange
        UserReviewAccessUpdateDto payload = new UserReviewAccessUpdateDto(true);
        AuthUserDto updatedUser = AuthUserDto.builder().id("user123").reviewBlocked(true).build();
        
        when(currentUserService.isModerator(any())).thenReturn(true);
        when(userService.setReviewBlocked(eq("user123"), eq(true))).thenReturn(updatedUser);

        // Act & Assert
        mockMvc.perform(patch("/users/user123/review-access")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"blocked\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewBlocked").value(true));
    }
}
