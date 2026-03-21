package edu.ap.bib_backend.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String SECRET = "this-is-a-very-long-test-secret-key-1234567890";

    @Test
    void givenUsernameAndRole_WhenGenerateToken_ThenReturnNonEmptyToken() {
        // Arrange
        JwtService jwtService = new JwtService(SECRET, 60_000L);
        String username = "user-123";
        String role = "ROLE_STUDENT";

        // Act
        String token = jwtService.generateToken(username, role);

        // Assert
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void givenGeneratedToken_WhenExtractUsername_ThenReturnOriginalUsername() {
        // Arrange
        JwtService jwtService = new JwtService(SECRET, 60_000L);
        String username = "user-abc";
        String role = "ROLE_ADMIN";
        String token = jwtService.generateToken(username, role);

        // Act
        String extractedUsername = jwtService.extractUsername(token);

        // Assert
        assertEquals(username, extractedUsername);
    }

    @Test
    void givenGeneratedToken_WhenExtractRolesClaim_ThenReturnOriginalRole() {
        // Arrange
        JwtService jwtService = new JwtService(SECRET, 60_000L);
        String username = "user-role";
        String role = "ROLE_TEACHER";
        String token = jwtService.generateToken(username, role);

        // Act
        Object extractedRole = jwtService.extractClaim(token, "roles");

        // Assert
        assertEquals(role, extractedRole);
    }

    @Test
    void givenFreshGeneratedToken_WhenIsValid_ThenReturnTrue() {
        // Arrange
        JwtService jwtService = new JwtService(SECRET, 60_000L);
        String token = jwtService.generateToken("valid-user", "ROLE_LIBRARIAN");

        // Act
        boolean result = jwtService.isValid(token);

        // Assert
        assertTrue(result);
    }

    @Test
    void givenTamperedToken_WhenIsValid_ThenReturnFalse() {
        // Arrange
        JwtService jwtService = new JwtService(SECRET, 60_000L);
        String token = jwtService.generateToken("tamper-user", "ROLE_STUDENT");
        String tamperedToken = token + "tampered";

        // Act
        boolean result = jwtService.isValid(tamperedToken);

        // Assert
        assertFalse(result);
    }

    @Test
    void givenExpiredToken_WhenIsValid_ThenReturnFalse() throws InterruptedException {
        // Arrange
        JwtService jwtService = new JwtService(SECRET, 1L);
        String token = jwtService.generateToken("expired-user", "ROLE_STUDENT");
        Thread.sleep(25L);

        // Act
        boolean result = jwtService.isValid(token);

        // Assert
        assertFalse(result);
    }
}