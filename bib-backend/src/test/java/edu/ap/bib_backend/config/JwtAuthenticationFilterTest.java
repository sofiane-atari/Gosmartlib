package edu.ap.bib_backend.config;

import edu.ap.bib_backend.service.AdminOverrideService;
import edu.ap.bib_backend.service.JwtService;
import edu.ap.bib_backend.service.TokenStorageService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
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
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

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
    void GivenNoAuthHeader_WhenFiltered_ThenContinueWithoutAuth() throws ServletException, IOException {
        // Arrange
        given(request.getHeader("Authorization")).willReturn(null);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        then(filterChain).should().doFilter(request, response);
        then(jwtService).shouldHaveNoInteractions();
    }

    @Test
    void GivenInvalidAuthPrefix_WhenFiltered_ThenContinueWithoutAuth() throws ServletException, IOException {
        // Arrange
        given(request.getHeader("Authorization")).willReturn("Basic dXNlcm5hbWU6cGFzc3dvcmQ=");

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        then(filterChain).should().doFilter(request, response);
        then(jwtService).shouldHaveNoInteractions();
    }

    @Test
    void GivenInvalidToken_WhenFiltered_ThenContinueWithoutAuth() throws ServletException, IOException {
        // Arrange
        String invalidToken = "invalid.jwt.token";
        given(request.getHeader("Authorization")).willReturn("Bearer " + invalidToken);
        given(jwtService.isValid(invalidToken)).willReturn(false);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        then(filterChain).should().doFilter(request, response);
    }

    @Test
    void GivenValidToken_WhenFiltered_ThenSetAuthentication() throws ServletException, IOException {
        // Arrange
        String validToken = "valid.jwt.token";
        String username = "testleerkracht";
        String role = "ROLE_TEACHER";

        given(request.getHeader("Authorization")).willReturn("Bearer " + validToken);
        given(jwtService.isValid(validToken)).willReturn(true);
        given(jwtService.extractUsername(validToken)).willReturn(username);
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
    void GivenValidTokenAndActiveOverride_WhenFiltered_ThenSetAdminAuthority() throws ServletException, IOException {
        // Arrange
        String validToken = "valid.jwt.token";
        String username = "student-1";

        given(request.getHeader("Authorization")).willReturn("Bearer " + validToken);
        given(jwtService.isValid(validToken)).willReturn(true);
        given(jwtService.extractUsername(validToken)).willReturn(username);
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
    void GivenValidJwtButNotCurrentToken_WhenFiltered_ThenDoNotSetAuthentication()
            throws ServletException, IOException {
        // Arrange
        String validToken = "old.jwt.token";
        String username = "student-1";

        given(request.getHeader("Authorization")).willReturn("Bearer " + validToken);
        given(jwtService.isValid(validToken)).willReturn(true);
        given(jwtService.extractUsername(validToken)).willReturn(username);
        given(tokenStorageService.isCurrentToken(username, validToken)).willReturn(false);

        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Assert
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        assertNull(authentication, "Authentication moet null blijven bij oude of ingetrokken token");
        then(filterChain).should().doFilter(request, response);
        then(jwtService).should(never()).extractClaim(validToken, "roles");
    }
}