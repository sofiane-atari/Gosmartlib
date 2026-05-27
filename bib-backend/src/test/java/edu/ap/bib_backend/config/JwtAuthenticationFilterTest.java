package edu.ap.bib_backend.config;

import edu.ap.bib_backend.service.AdminOverrideService;
import edu.ap.bib_backend.service.JwtService;
import edu.ap.bib_backend.service.TokenStorageService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private AdminOverrideService adminOverrideService;

    @Mock
    private FilterChain filterChain;

    @Mock
    private TokenStorageService tokenStorageService;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void Given_NoAuthHeader_When_Filtered_Then_ContinueWithoutAuth() throws ServletException, IOException {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        then(filterChain).should().doFilter(request, response);
        then(jwtService).shouldHaveNoInteractions();
    }

    @Test
    void Given_InvalidAuthPrefix_When_Filtered_Then_ContinueWithoutAuth() throws ServletException, IOException {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic dXNlcm5hbWU6cGFzc3dvcmQ=");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        then(filterChain).should().doFilter(request, response);
        then(jwtService).shouldHaveNoInteractions();
    }

    @Test
    void Given_InvalidToken_When_Filtered_Then_ReturnsUnauthorizedResponse() throws ServletException, IOException {
        // Arrange
        String invalidToken = "invalid.jwt.token";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + invalidToken);
        MockHttpServletResponse response = new MockHttpServletResponse();
        given(jwtService.isValid(invalidToken)).willReturn(false);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        assertTrue(response.getContentAsString().contains("TOKEN_INVALID"));
        then(filterChain).shouldHaveNoInteractions();
    }

    @Test
    void Given_ValidToken_When_Filtered_Then_SetsAuthentication() throws ServletException, IOException {
        // Arrange
        String validToken = "valid.jwt.token";
        String username = "testleerkracht";
        String role = "ROLE_TEACHER";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + validToken);
        MockHttpServletResponse response = new MockHttpServletResponse();

        given(jwtService.isValid(validToken)).willReturn(true);
        given(jwtService.extractUsername(validToken)).willReturn(username);
        given(jwtService.extractClaim(validToken, "type")).willReturn(null);
        given(tokenStorageService.isCurrentToken(username, validToken)).willReturn(true);
        given(jwtService.extractClaim(validToken, "roles")).willReturn(role);
        given(adminOverrideService.isAdminOverrideActive(username)).willReturn(false);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        assertNotNull(authentication, "Authentication mag niet null zijn");
        assertEquals(username, authentication.getName(), "Username moet overeenkomen");
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(role)), "Gebruiker moet de juiste rol hebben");

        then(filterChain).should().doFilter(request, response);
    }

    @Test
    void Given_ValidTokenAndActiveOverride_When_Filtered_Then_SetsAdminAuthority()
            throws ServletException, IOException {
        // Arrange
        String validToken = "valid.jwt.token";
        String username = "student-1";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + validToken);
        MockHttpServletResponse response = new MockHttpServletResponse();

        given(jwtService.isValid(validToken)).willReturn(true);
        given(jwtService.extractUsername(validToken)).willReturn(username);
        given(jwtService.extractClaim(validToken, "type")).willReturn(null);
        given(tokenStorageService.isCurrentToken(username, validToken)).willReturn(true);
        given(jwtService.extractClaim(validToken, "roles")).willReturn("ROLE_STUDENT");
        given(adminOverrideService.isAdminOverrideActive(username)).willReturn(true);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        assertNotNull(authentication, "Authentication mag niet null zijn");
        assertEquals(username, authentication.getName(), "Username moet overeenkomen");
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")),
                "Gebruiker moet tijdelijk admin rechten krijgen");

        then(filterChain).should().doFilter(request, response);
    }

    @Test
    void Given_ValidJwtButNotCurrentToken_When_Filtered_Then_ReturnsUnauthorizedResponse()
            throws ServletException, IOException {
        // Arrange
        String validToken = "old.jwt.token";
        String username = "student-1";

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + validToken);
        MockHttpServletResponse response = new MockHttpServletResponse();

        given(jwtService.isValid(validToken)).willReturn(true);
        given(jwtService.extractUsername(validToken)).willReturn(username);
        given(jwtService.extractClaim(validToken, "type")).willReturn(null);
        given(tokenStorageService.isCurrentToken(username, validToken)).willReturn(false);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        assertNull(authentication, "Authentication moet null blijven bij oude of ingetrokken token");
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        assertTrue(response.getContentAsString().contains("TOKEN_INVALID"));
        then(filterChain).shouldHaveNoInteractions();
        then(jwtService).should(never()).extractClaim(validToken, "roles");
    }
}
