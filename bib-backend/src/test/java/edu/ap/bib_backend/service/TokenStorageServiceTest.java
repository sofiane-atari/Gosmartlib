package edu.ap.bib_backend.service;

import edu.ap.bib_backend.jpa.entity.AuthSession;
import edu.ap.bib_backend.jpa.repository.AuthSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenStorageServiceTest {

    @Mock
    private AuthSessionRepository authSessionRepository;

    @InjectMocks
    private TokenStorageService tokenStorageService;

    private final String userId = "user123";
    private final String jwtToken = "valid.jwt.token";
    private final String role = "ROLE_STUDENT";
    private final Instant expiry = Instant.now().plusSeconds(3600);

    @Test
    void Given_NewUser_When_StoreToken_Then_SessionIsCreatedAndSaved() {
        // Arrange
        when(authSessionRepository.findById(userId)).thenReturn(Optional.empty());

        // Act
        tokenStorageService.storeToken(userId, jwtToken, role, expiry);

        // Assert
        verify(authSessionRepository).save(any(AuthSession.class));
    }

    @Test
    void Given_ExistingUser_When_StoreToken_Then_SessionIsUpdatedAndSaved() {
        // Arrange
        AuthSession existingSession = new AuthSession(userId, "old.token", "OLD_ROLE", Instant.now());
        when(authSessionRepository.findById(userId)).thenReturn(Optional.of(existingSession));

        // Act
        tokenStorageService.storeToken(userId, jwtToken, role, expiry);

        // Assert
        assertEquals(jwtToken, existingSession.getJwtToken());
        assertEquals(role, existingSession.getRole());
        assertEquals(expiry, existingSession.getJwtExpiresAt());
        verify(authSessionRepository).save(existingSession);
    }

    @Test
    void Given_ValidActiveToken_When_GetToken_Then_TokenIsReturned() {
        // Arrange
        AuthSession session = new AuthSession(userId, jwtToken, role, expiry);
        when(authSessionRepository.findById(userId)).thenReturn(Optional.of(session));

        // Act
        String result = tokenStorageService.getToken(userId);

        // Assert
        assertEquals(jwtToken, result);
    }

    @Test
    void Given_ExpiredToken_When_GetToken_Then_NullIsReturned() {
        // Arrange
        AuthSession session = new AuthSession(userId, jwtToken, role, Instant.now().minusSeconds(10));
        when(authSessionRepository.findById(userId)).thenReturn(Optional.of(session));

        // Act
        String result = tokenStorageService.getToken(userId);

        // Assert
        assertNull(result);
    }

    @Test
    void Given_ValidToken_When_HasActiveToken_Then_ReturnsTrue() {
        // Arrange
        AuthSession session = new AuthSession(userId, jwtToken, role, expiry);
        when(authSessionRepository.findById(userId)).thenReturn(Optional.of(session));

        // Act
        boolean result = tokenStorageService.hasActiveToken(userId);

        // Assert
        assertTrue(result);
    }

    @Test
    void Given_CorrectToken_When_IsCurrentToken_Then_ReturnsTrue() {
        // Arrange
        AuthSession session = new AuthSession(userId, jwtToken, role, expiry);
        when(authSessionRepository.findById(userId)).thenReturn(Optional.of(session));

        // Act
        boolean result = tokenStorageService.isCurrentToken(userId, jwtToken);

        // Assert
        assertTrue(result);
    }

    @Test
    void Given_UserId_When_RevokeToken_Then_RepositoryDeleteIsCalled() {
        // Act
        tokenStorageService.revokeToken(userId);

        // Assert
        verify(authSessionRepository).deleteById(userId);
    }

    @Test
    void Given_SmartschoolTokens_When_StoreSmartschoolTokens_Then_SessionIsUpdated() {
        // Arrange
        AuthSession session = new AuthSession(userId, jwtToken, role, expiry);
        when(authSessionRepository.findById(userId)).thenReturn(Optional.of(session));
        String accessToken = "ss-access";
        String refreshToken = "ss-refresh";

        // Act
        tokenStorageService.storeSmartschoolTokens(userId, accessToken, refreshToken);

        // Assert
        assertEquals(accessToken, session.getSmartschoolAccessToken());
        assertEquals(refreshToken, session.getSmartschoolRefreshToken());
        assertNotNull(session.getSmartschoolAccessTokenExpiresAt());
        assertNotNull(session.getSmartschoolRefreshTokenExpiresAt());
        verify(authSessionRepository).save(session);
    }

    @Test
    void Given_NoSession_When_StoreSmartschoolTokens_Then_ThrowsIllegalStateException() {
        // Arrange
        when(authSessionRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> 
            tokenStorageService.storeSmartschoolTokens(userId, "access", "refresh"));
    }

    @Test
    void Given_AdminOverride_When_StoreAdminOverride_Then_SessionIsUpdated() {
        // Arrange
        AuthSession session = new AuthSession(userId, jwtToken, role, expiry);
        when(authSessionRepository.findById(userId)).thenReturn(Optional.of(session));

        // Act
        tokenStorageService.storeAdminOverride(userId, 30);

        // Assert
        assertTrue(session.isAdminOverride());
        assertNotNull(session.getAdminOverrideExpiresAt());
        verify(authSessionRepository).save(session);
    }

    @Test
    void Given_ActiveAdminOverride_When_HasAdminOverride_Then_ReturnsTrue() {
        // Arrange
        AuthSession session = new AuthSession(userId, jwtToken, role, expiry);
        session.setAdminOverride(true);
        session.setAdminOverrideExpiresAt(Instant.now().plusSeconds(600));
        when(authSessionRepository.findById(userId)).thenReturn(Optional.of(session));

        // Act
        boolean result = tokenStorageService.hasAdminOverride(userId);

        // Assert
        assertTrue(result);
    }
}