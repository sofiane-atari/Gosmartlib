package edu.ap.bib_backend.service;

import edu.ap.bib_backend.dto.AuthUserDto;
import edu.ap.bib_backend.dto.JwtTokenDTO;
import edu.ap.bib_backend.dto.SmartschoolGroupDto;
import edu.ap.bib_backend.dto.SmartschoolProfileDto;
import edu.ap.bib_backend.dto.SmartschoolRefreshResult;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.enums.Role;
import edu.ap.bib_backend.jpa.repository.SchoolRepository;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private JwtService jwtService;
    @Mock
    private TokenStorageService tokenStorageService;
    @Mock
    private OAuth2AuthorizedClientService authorizedClientService;
    @Mock
    private SmartschoolProfileService smartschoolProfileService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SchoolRepository schoolRepository;

    @InjectMocks
    private AuthService authService;

    @Test
    void Given_AuthenticatedUserWithSmartschoolToken_When_GetUserDetails_Then_ReturnsFullDetails() {
        // Arrange
        String userId = "user123";
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(userId);
        when(auth.getAuthorities()).thenAnswer(i -> Collections.singleton(new SimpleGrantedAuthority("ROLE_STUDENT")));
        
        when(tokenStorageService.getSmartschoolAccessToken(userId)).thenReturn("ss-token");
        
        User user = new User();
        user.setReviewBlocked(false);
        School school = new School();
        school.setName("AP");
        user.setSchool(school);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        SmartschoolProfileDto profile = new SmartschoolProfileDto("ssId", "First", "Last", "Full", "email@test.com", "platform");
        when(smartschoolProfileService.fetchProfile("ss-token")).thenReturn(profile);
        
        SmartschoolGroupDto groups = new SmartschoolGroupDto("Primary", List.of("Parent"));
        when(smartschoolProfileService.fetchGroups("ss-token")).thenReturn(groups);

        // Act
        AuthUserDto result = authService.getUserDetails(auth);

        // Assert
        assertNotNull(result);
        assertEquals(userId, result.id());
        assertEquals("Full", result.fullName());
        assertEquals(Role.STUDENT, result.role());
        assertEquals("Primary", result.primaryGroupName());
        verify(smartschoolProfileService).fetchProfile("ss-token");
        verify(smartschoolProfileService).fetchGroups("ss-token");
    }

    @Test
    void Given_AuthenticatedUser_When_Logout_Then_RevokesToken() {
        // Arrange
        String userId = "user123";

        // Act
        authService.logout(userId);

        // Assert
        verify(tokenStorageService).revokeToken(userId);
    }

    @Test
    void Given_InvalidRole_When_GetUserDetails_Then_ThrowsIllegalStateException() {
        // Arrange
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("user");
        when(auth.getAuthorities()).thenAnswer(i -> Collections.emptyList());

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> authService.getUserDetails(auth));
    }

    @Test
    void Given_InvalidOldToken_When_RefreshJwtToken_Then_ThrowsUnauthorized() {
        // Arrange
        String oldToken = "invalid.token";
        when(jwtService.isValid(oldToken)).thenReturn(false);

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
            authService.refreshJwtToken(oldToken));
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    void Given_TokenNotActive_When_RefreshJwtToken_Then_ThrowsUnauthorized() {
        // Arrange
        String oldToken = "old.jwt.token";
        String userId = "user123";
        String role = "ROLE_STUDENT";

        when(jwtService.isValid(oldToken)).thenReturn(true);
        when(jwtService.extractUsername(oldToken)).thenReturn(userId);
        when(jwtService.extractClaim(oldToken, "roles")).thenReturn(role);
        when(tokenStorageService.isCurrentToken(userId, oldToken)).thenReturn(false);

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
            authService.refreshJwtToken(oldToken));
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    void Given_NoSmartschoolRefreshToken_When_RefreshJwtToken_Then_ThrowsUnauthorized() {
        // Arrange
        String oldToken = "old.jwt.token";
        String userId = "user123";
        String role = "ROLE_STUDENT";

        when(jwtService.isValid(oldToken)).thenReturn(true);
        when(jwtService.extractUsername(oldToken)).thenReturn(userId);
        when(jwtService.extractClaim(oldToken, "roles")).thenReturn(role);
        when(tokenStorageService.isCurrentToken(userId, oldToken)).thenReturn(true);
        when(tokenStorageService.getSmartschoolRefreshToken(userId)).thenReturn(null);

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
            authService.refreshJwtToken(oldToken));
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    void Given_BlankSmartschoolRefreshToken_When_RefreshJwtToken_Then_ThrowsUnauthorized() {
        // Arrange
        String oldToken = "old.jwt.token";
        String userId = "user123";
        String role = "ROLE_STUDENT";

        when(jwtService.isValid(oldToken)).thenReturn(true);
        when(jwtService.extractUsername(oldToken)).thenReturn(userId);
        when(jwtService.extractClaim(oldToken, "roles")).thenReturn(role);
        when(tokenStorageService.isCurrentToken(userId, oldToken)).thenReturn(true);
        when(tokenStorageService.getSmartschoolRefreshToken(userId)).thenReturn("   ");

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
            authService.refreshJwtToken(oldToken));
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    void Given_UserWithSchool_When_GetUserDetails_Then_ReturnsSchool() {
        // Arrange
        String userId = "user123";
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(userId);
        when(auth.getAuthorities()).thenAnswer(i -> Collections.singleton(new SimpleGrantedAuthority("ROLE_STUDENT")));
        when(tokenStorageService.getSmartschoolAccessToken(userId)).thenReturn(null);

        School school = new School();
        school.setName("AP School");
        school.setDefaultLoanDurationDays(30);

        User user = new User();
        user.setReviewBlocked(false);
        user.setSchool(school);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act
        AuthUserDto result = authService.getUserDetails(auth);

        // Assert
        assertNotNull(result);
        assertNotNull(result.school());
    }

    @Test
    void Given_SmartschoolTokenThrowsException_When_GetUserDetails_Then_FallsBackToOAuth() {
        // Arrange
        String userId = "user123";
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(userId);
        when(auth.getAuthorities()).thenAnswer(i -> Collections.singleton(new SimpleGrantedAuthority("ROLE_STUDENT")));
        when(tokenStorageService.getSmartschoolAccessToken(userId)).thenReturn("ss-token");

        when(smartschoolProfileService.fetchProfile("ss-token")).thenThrow(new RuntimeException("API Error"));

        User user = new User();
        user.setReviewBlocked(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act
        AuthUserDto result = authService.getUserDetails(auth);

        // Assert - should not throw, fallback to OAuth
        assertNotNull(result);
    }
}
