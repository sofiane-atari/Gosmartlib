package edu.ap.bib_backend.service;

import edu.ap.bib_backend.jpa.entity.AuthSession;
import edu.ap.bib_backend.jpa.repository.AuthSessionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TokenStorageServiceIntegrationTest {

    @Autowired
    private TokenStorageService tokenStorageService;

    @Autowired
    private AuthSessionRepository authSessionRepository;

    @Test
    void Given_SessionData_When_StoreToken_Then_DataIsPersistedInDatabase() {
        // Arrange
        String userId = "int-user-1";
        String token = "int.jwt.token";
        String role = "ROLE_LIBRARIAN";
        Instant expiry = Instant.now().plusSeconds(3600);

        // Act
        tokenStorageService.storeToken(userId, token, role, expiry);

        // Assert
        AuthSession saved = authSessionRepository.findById(userId).orElse(null);
        assertNotNull(saved);
        assertEquals(token, saved.getJwtToken());
        assertEquals(role, saved.getRole());
    }

    @Test
    void Given_PersistedSession_When_RevokeToken_Then_DataIsRemovedFromDatabase() {
        // Arrange
        String userId = "int-user-2";
        authSessionRepository.save(new AuthSession(userId, "token", "ROLE", Instant.now().plusSeconds(60)));

        // Act
        tokenStorageService.revokeToken(userId);

        // Assert
        assertFalse(authSessionRepository.existsById(userId));
    }
}
