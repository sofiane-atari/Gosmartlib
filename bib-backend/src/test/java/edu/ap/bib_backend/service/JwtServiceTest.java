package edu.ap.bib_backend.service;

import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private final String secret = "my-secret-key-that-is-at-least-32-characters-long";
    private final long expiration = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        // Arrange
        jwtService = new JwtService(secret, expiration);
    }

    @Test
    void Given_UsernameAndRole_When_GenerateToken_Then_TokenIsGenerated() {
        // Arrange
        String username = "testuser";
        String role = "ROLE_STUDENT";

        // Act
        String token = jwtService.generateToken(username, role);

        // Assert
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void Given_ValidToken_When_ExtractUsername_Then_CorrectUsernameIsReturned() {
        // Arrange
        String username = "testuser";
        String role = "ROLE_STUDENT";
        String token = jwtService.generateToken(username, role);

        // Act
        String extractedUsername = jwtService.extractUsername(token);

        // Assert
        assertEquals(username, extractedUsername);
    }

    @Test
    void Given_ValidToken_When_ExtractClaim_Then_CorrectClaimIsReturned() {
        // Arrange
        String username = "testuser";
        String role = "ROLE_STUDENT";
        String token = jwtService.generateToken(username, role);

        // Act
        Object extractedRole = jwtService.extractClaim(token, "roles");

        // Assert
        assertEquals(role, extractedRole);
    }

    @Test
    void Given_ValidToken_When_IsValid_Then_ReturnsTrue() {
        // Arrange
        String username = "testuser";
        String role = "ROLE_STUDENT";
        String token = jwtService.generateToken(username, role);

        // Act
        boolean isValid = jwtService.isValid(token);

        // Assert
        assertTrue(isValid);
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid-token", "", "header.payload.signature_but_invalid"})
    void Given_InvalidToken_When_IsValid_Then_ReturnsFalse(String invalidToken) {
        // Act
        boolean isValid = jwtService.isValid(invalidToken);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void Given_ValidToken_When_ExtractExpiration_Then_ExpirationDateIsNotNull() {
        // Arrange
        String username = "testuser";
        String role = "ROLE_STUDENT";
        String token = jwtService.generateToken(username, role);

        // Act
        Date expirationDate = jwtService.extractExpiration(token);

        // Assert
        assertNotNull(expirationDate);
        assertTrue(expirationDate.after(new Date()));
    }

    @Test
    void Given_ValidToken_When_ExtractExpirationInstant_Then_InstantIsCorrect() {
        // Arrange
        String username = "testuser";
        String role = "ROLE_STUDENT";
        String token = jwtService.generateToken(username, role);

        // Act
        Instant expirationInstant = jwtService.extractExpirationInstant(token);

        // Assert
        assertNotNull(expirationInstant);
        assertTrue(expirationInstant.isAfter(Instant.now()));
    }

    @Test
    void Given_ExtraClaims_When_GenerateToken_Then_ClaimsAreIncluded() {
        // Arrange
        String username = "testuser";
        String role = "ROLE_STUDENT";
        Map<String, Object> extraClaims = Map.of("schoolCode", "AP");

        // Act
        String token = jwtService.generateToken(username, role, extraClaims);
        String extractedSchoolCode = (String) jwtService.extractClaim(token, "schoolCode");

        // Assert
        assertEquals("AP", extractedSchoolCode);
    }
}