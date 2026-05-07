package edu.ap.bib_backend.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class JwtServiceIntegrationTest {

    @Autowired
    private JwtService jwtService;

    @Test
    void Given_ServiceIsLoaded_When_GenerateToken_Then_TokenIsSuccessfullyGenerated() {
        // Arrange
        String username = "integration-user";
        String role = "ROLE_STUDENT";

        // Act
        String token = jwtService.generateToken(username, role);

        // Assert
        assertNotNull(token);
        assertTrue(jwtService.isValid(token));
    }
}
