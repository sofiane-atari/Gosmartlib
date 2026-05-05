package edu.ap.bib_backend.controller;

import edu.ap.bib_backend.dto.AuthUserDto;
import edu.ap.bib_backend.dto.JwtTokenDTO;
import edu.ap.bib_backend.jpa.enums.Role;
import edu.ap.bib_backend.service.AuthService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AuthControllerTest {

    private MockMvc mockMvc;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);

        AuthController controller = new AuthController(authService);

        ReflectionTestUtils.setField(
                controller,
                "frontendRedirectUrl",
                "http://localhost:4200/login");

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new TestExceptionHandler())
                .build();
    }

    @Test
    void givenUnauthenticated_whenMeCalled_thenReturn401() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void givenUnauthenticated_whenLogoutCalled_thenReturn401() throws Exception {
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void givenAuthenticatedUser_whenMeCalled_thenReturnUserInfo() throws Exception {
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_STUDENT"));

        Authentication authentication = new UsernamePasswordAuthenticationToken("1", null, authorities);

        AuthUserDto userDto = new AuthUserDto(
                "1",
                "Enes",
                "Taşci",
                "Taşci Enes",
                Role.STUDENT,
                "enes.tasci@student.ap.be",
                "1",
                "https://aphogeschool.smartschool.be",
                "2ITSOF1",
                List.of("Leerlingen"),
                false,
                1L,
                "Test School",
                100L,
                "Test Campus");

        when(authService.getUserDetails(any(Authentication.class))).thenReturn(userDto);

        mockMvc.perform(get("/auth/me").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.firstName").value("Enes"))
                .andExpect(jsonPath("$.lastName").value("Taşci"))
                .andExpect(jsonPath("$.fullName").value("Taşci Enes"))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.email").value("enes.tasci@student.ap.be"))
                .andExpect(jsonPath("$.primaryGroupName").value("2ITSOF1"))
                .andExpect(jsonPath("$.reviewBlocked").value(false));

        verify(authService).getUserDetails(any(Authentication.class));
    }

    @Test
    void givenAuthenticatedUserWithoutDetails_whenMeCalled_thenReturnEmptyOk() throws Exception {
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_STUDENT"));

        Authentication authentication = new UsernamePasswordAuthenticationToken("1", null, authorities);

        when(authService.getUserDetails(any(Authentication.class))).thenReturn(null);

        mockMvc.perform(get("/auth/me").principal(authentication))
                .andExpect(status().isOk());

        verify(authService).getUserDetails(any(Authentication.class));
    }

    @Test
    void givenOAuth2User_whenCallbackCalled_thenRedirectWithToken() throws Exception {
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_STUDENT"));

        OAuth2User principal = new DefaultOAuth2User(
                authorities,
                Map.of("sub", "1"),
                "sub");

        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(principal, authorities, "smartschool");

        when(authService.processCallbackAndGenerateJwt(any(OAuth2AuthenticationToken.class)))
                .thenReturn("jwt-token");

        mockMvc.perform(get("/auth/callback").principal(authentication))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(
                        "Location",
                        containsString("token=jwt-token")));

        verify(authService).processCallbackAndGenerateJwt(any(OAuth2AuthenticationToken.class));
    }

    @Test
    void givenNonOAuthAuthentication_whenCallbackCalled_thenRedirectWithError() throws Exception {
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_STUDENT"));

        Authentication authentication = new UsernamePasswordAuthenticationToken("1", null, authorities);

        mockMvc.perform(get("/auth/callback").principal(authentication))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(
                        "Location",
                        containsString("error=authentication_failed")));
    }

    @Test
    void givenAuthenticatedUser_whenLogoutCalled_thenLogoutAndReturnMessage() throws Exception {
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_STUDENT"));

        Authentication authentication = new UsernamePasswordAuthenticationToken("1", null, authorities);

        mockMvc.perform(post("/auth/logout").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));

        verify(authService).logout("1");
    }

    @Test
    void givenValidBearerToken_whenRefreshCalled_thenReturnNewJwtToken() throws Exception {
        JwtTokenDTO tokenDto = new JwtTokenDTO(
                "new-jwt-token",
                Instant.parse("2026-04-30T12:00:00Z"));

        when(authService.refreshJwtToken("old-jwt-token")).thenReturn(tokenDto);

        mockMvc.perform(post("/auth/refresh")
                .header("Authorization", "Bearer old-jwt-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("new-jwt-token"))
                .andExpect(jsonPath("$.expiresAt").exists());

        verify(authService).refreshJwtToken("old-jwt-token");
    }

    @Test
    void givenMissingBearerToken_whenRefreshCalled_thenReturn401() throws Exception {
        mockMvc.perform(post("/auth/refresh"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void givenInvalidAuthorizationHeader_whenRefreshCalled_thenReturn401() throws Exception {
        mockMvc.perform(post("/auth/refresh")
                .header("Authorization", "Basic invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void givenOAuthErrorWithMessage_whenOauthErrorCalled_thenReturn401WithMessage() throws Exception {
        mockMvc.perform(get("/auth/error")
                .param("message", "Smartschool login failed"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Smartschool login failed"));
    }
}