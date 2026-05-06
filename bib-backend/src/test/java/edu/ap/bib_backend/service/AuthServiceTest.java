package edu.ap.bib_backend.service;

import edu.ap.bib_backend.dto.AuthUserDto;
import edu.ap.bib_backend.dto.SmartschoolGroupDto;
import edu.ap.bib_backend.dto.SmartschoolProfileDto;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
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

        SmartschoolProfileDto profile = new SmartschoolProfileDto("First", "Last", "Full", "email@test.com", "ssId", "platform");
        when(smartschoolProfileService.fetchProfile("ss-token")).thenReturn(profile);
        
        SmartschoolGroupDto groups = new SmartschoolGroupDto("Primary", List.of("Parent"));
        when(smartschoolProfileService.fetchGroups("ss-token")).thenReturn(groups);

        // Act
        AuthUserDto result = authService.getUserDetails(auth);

        // Assert
        assertNotNull(result);
        assertEquals(userId, result.userId());
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
}
