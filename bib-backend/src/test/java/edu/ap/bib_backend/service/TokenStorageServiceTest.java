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

    @Test
    void Given_ExpiredAdminOverride_When_HasAdminOverride_Then_ReturnsFalse() {
        // Arrange
        AuthSession session = new AuthSession(userId, jwtToken, role, expiry);
        session.setAdminOverride(true);
        session.setAdminOverrideExpiresAt(Instant.now().minusSeconds(10));
        when(authSessionRepository.findById(userId)).thenReturn(Optional.of(session));

        // Act
        boolean result = tokenStorageService.hasAdminOverride(userId);

        // Assert
        assertFalse(result);
    }

    @Test
    void Given_NoSession_When_HasAdminOverride_Then_ReturnsFalse() {
        // Arrange
        when(authSessionRepository.findById(userId)).thenReturn(Optional.empty());

        // Act
        boolean result = tokenStorageService.hasAdminOverride(userId);

        // Assert
        assertFalse(result);
    }

    @Test
    void Given_ExistingSession_When_ReplaceToken_Then_TokenIsUpdated() {
        // Arrange
        AuthSession existingSession = new AuthSession(userId, "old.token", "OLD_ROLE", Instant.now());
        when(authSessionRepository.findById(userId)).thenReturn(Optional.of(existingSession));

        // Act
        tokenStorageService.replaceToken(userId, jwtToken, role, expiry);

        // Assert
        assertEquals(jwtToken, existingSession.getJwtToken());
        verify(authSessionRepository).save(existingSession);
    }

    @Test
    void Given_SessionExists_When_GetSession_Then_ReturnsSession() {
        // Arrange
        AuthSession session = new AuthSession(userId, jwtToken, role, expiry);
        when(authSessionRepository.findById(userId)).thenReturn(Optional.of(session));

        // Act
        Optional<AuthSession> result = tokenStorageService.getSession(userId);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(userId, result.get().getUserId());
    }

    @Test
    void Given_NoSession_When_GetSession_Then_ReturnsEmpty() {
        // Arrange
        when(authSessionRepository.findById(userId)).thenReturn(Optional.empty());

        // Act
        Optional<AuthSession> result = tokenStorageService.getSession(userId);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void Given_DifferentToken_When_IsCurrentToken_Then_ReturnsFalse() {
        // Arrange
        AuthSession session = new AuthSession(userId, jwtToken, role, expiry);
        when(authSessionRepository.findById(userId)).thenReturn(Optional.of(session));

        // Act
        boolean result = tokenStorageService.isCurrentToken(userId, "different.token");

        // Assert
        assertFalse(result);
    }

    @Test
    void Given_NullToken_When_IsCurrentToken_Then_ReturnsFalse() {
        // Arrange
        AuthSession session = new AuthSession(userId, jwtToken, role, expiry);
        when(authSessionRepository.findById(userId)).thenReturn(Optional.of(session));

        // Act
        boolean result = tokenStorageService.isCurrentToken(userId, null);

        // Assert
        assertFalse(result);
    }

    @Test
    void Given_ExpiredToken_When_HasActiveTokenWithToken_Then_ReturnsFalse() {
        // Arrange
        AuthSession session = new AuthSession(userId, jwtToken, role, Instant.now().minusSeconds(10));
        when(authSessionRepository.findById(userId)).thenReturn(Optional.of(session));

        // Act
        boolean result = tokenStorageService.hasActiveToken(userId, jwtToken);

        // Assert
        assertFalse(result);
    }

    @Test
    void Given_ValidToken_When_HasActiveTokenWithToken_Then_ReturnsTrue() {
        // Arrange
        AuthSession session = new AuthSession(userId, jwtToken, role, expiry);
        when(authSessionRepository.findById(userId)).thenReturn(Optional.of(session));

        // Act
        boolean result = tokenStorageService.hasActiveToken(userId, jwtToken);

        // Assert
        assertTrue(result);
    }

    @Test
    void Given_SessionWithRole_When_StoreUserMetadata_Then_RoleIsUpdated() {
        // Arrange
        AuthSession session = new AuthSession(userId, jwtToken, role, expiry);
        when(authSessionRepository.findById(userId)).thenReturn(Optional.of(session));

        // Act
        tokenStorageService.storeUserMetadata(userId, "ROLE_ADMIN");

        // Assert
        assertEquals("ROLE_ADMIN", session.getRole());
        verify(authSessionRepository).save(session);
    }

    @Test
    void Given_NoSession_When_StoreUserMetadata_Then_DoesNothing() {
        // Arrange
        when(authSessionRepository.findById(userId)).thenReturn(Optional.empty());

        // Act
        tokenStorageService.storeUserMetadata(userId, "ROLE_ADMIN");

        // Assert
        verify(authSessionRepository, never()).save(any());
    }

    @Test
    void Given_SessionWithRole_When_RemoveUserMetadata_Then_RoleIsCleared() {
        // Arrange
        AuthSession session = new AuthSession(userId, jwtToken, role, expiry);
        when(authSessionRepository.findById(userId)).thenReturn(Optional.of(session));

        // Act
        tokenStorageService.removeUserMetadata(userId);

        // Assert
        assertEquals("", session.getRole());
        verify(authSessionRepository).save(session);
    }

    @Test
    void Given_SessionWithRole_When_GetUserRole_Then_ReturnsRole() {
        // Arrange
        AuthSession session = new AuthSession(userId, jwtToken, role, expiry);
        when(authSessionRepository.findById(userId)).thenReturn(Optional.of(session));

        // Act
        String result = tokenStorageService.getUserRole(userId);

        // Assert
        assertEquals(role, result);
    }

    @Test
    void Given_NoSession_When_GetUserRole_Then_ReturnsNull() {
        // Arrange
        when(authSessionRepository.findById(userId)).thenReturn(Optional.empty());

        // Act
        String result = tokenStorageService.getUserRole(userId);

        // Assert
        assertNull(result);
    }

    @Test
    void Given_NullExpiresAt_When_GetToken_Then_ReturnsNull() {
        // Arrange
        AuthSession session = new AuthSession(userId, jwtToken, role, null);
        when(authSessionRepository.findById(userId)).thenReturn(Optional.of(session));

        // Act
        String result = tokenStorageService.getToken(userId);

        // Assert
        assertNull(result);
    }

    @Test
    void Given_ExpiredSmartschoolRefreshToken_When_GetSmartschoolRefreshToken_Then_ReturnsNull() {
        // Arrange
        AuthSession session = new AuthSession(userId, jwtToken, role, expiry);
        session.setSmartschoolRefreshToken("refresh");
        session.setSmartschoolRefreshTokenExpiresAt(Instant.now().minusSeconds(10));
        when(authSessionRepository.findById(userId)).thenReturn(Optional.of(session));

        // Act
        String result = tokenStorageService.getSmartschoolRefreshToken(userId);

        // Assert
        assertNull(result);
    }

    @Test
    void Given_ValidSmartschoolRefreshToken_When_GetSmartschoolRefreshToken_Then_ReturnsToken() {
        // Arrange
        AuthSession session = new AuthSession(userId, jwtToken, role, expiry);
        session.setSmartschoolRefreshToken("refresh-token");
        session.setSmartschoolRefreshTokenExpiresAt(Instant.now().plusSeconds(3600));
        when(authSessionRepository.findById(userId)).thenReturn(Optional.of(session));

        // Act
        String result = tokenStorageService.getSmartschoolRefreshToken(userId);

        // Assert
        assertEquals("refresh-token", result);
    }

    @Test
    void Given_NullRefreshToken_When_HasValidSmartschoolRefreshToken_Then_ReturnsFalse() {
        // Arrange
        AuthSession session = new AuthSession(userId, jwtToken, role, expiry);
        session.setSmartschoolRefreshToken(null);
        when(authSessionRepository.findById(userId)).thenReturn(Optional.of(session));

        // Act
        boolean result = tokenStorageService.hasValidSmartschoolRefreshToken(userId);

        // Assert
        assertFalse(result);
    }

    @Test
    void Given_ValidRefreshToken_When_HasValidSmartschoolRefreshToken_Then_ReturnsTrue() {
        // Arrange
        AuthSession session = new AuthSession(userId, jwtToken, role, expiry);
        session.setSmartschoolRefreshToken("refresh-token");
        session.setSmartschoolRefreshTokenExpiresAt(Instant.now().plusSeconds(3600));
        when(authSessionRepository.findById(userId)).thenReturn(Optional.of(session));

        // Act
        boolean result = tokenStorageService.hasValidSmartschoolRefreshToken(userId);

        // Assert
        assertTrue(result);
    }

    @Test
    void Given_NullAccessTokenExpiresAt_When_GetSmartschoolAccessToken_Then_ReturnsNull() {
        // Arrange
        AuthSession session = new AuthSession(userId, jwtToken, role, expiry);
        session.setSmartschoolAccessToken("access");
        session.setSmartschoolAccessTokenExpiresAt(null);
        when(authSessionRepository.findById(userId)).thenReturn(Optional.of(session));

        // Act
        String result = tokenStorageService.getSmartschoolAccessToken(userId);

        // Assert
        assertNull(result);
    }

    @Test
    void Given_ExpiredSmartschoolAccessToken_When_GetSmartschoolAccessToken_Then_ReturnsNull() {
        // Arrange
        AuthSession session = new AuthSession(userId, jwtToken, role, expiry);
        session.setSmartschoolAccessToken("access");
        session.setSmartschoolAccessTokenExpiresAt(Instant.now().minusSeconds(10));
        when(authSessionRepository.findById(userId)).thenReturn(Optional.of(session));

        // Act
        String result = tokenStorageService.getSmartschoolAccessToken(userId);

        // Assert
        assertNull(result);
    }

    @Test
    void Given_ValidSmartschoolAccessToken_When_GetSmartschoolAccessToken_Then_ReturnsToken() {
        // Arrange
        AuthSession session = new AuthSession(userId, jwtToken, role, expiry);
        session.setSmartschoolAccessToken("access-token");
        session.setSmartschoolAccessTokenExpiresAt(Instant.now().plusSeconds(3600));
        when(authSessionRepository.findById(userId)).thenReturn(Optional.of(session));

        // Act
        String result = tokenStorageService.getSmartschoolAccessToken(userId);

        // Assert
        assertEquals("access-token", result);
    }

    @Test
    void Given_SessionWithAccessToken_When_RemoveSmartschoolAccessToken_Then_ClearsToken() {
        // Arrange
        AuthSession session = new AuthSession(userId, jwtToken, role, expiry);
        session.setSmartschoolAccessToken("access");
        session.setSmartschoolAccessTokenExpiresAt(Instant.now().plusSeconds(3600));
        when(authSessionRepository.findById(userId)).thenReturn(Optional.of(session));

        // Act
        tokenStorageService.removeSmartschoolAccessToken(userId);

        // Assert
        assertNull(session.getSmartschoolAccessToken());
        assertNull(session.getSmartschoolAccessTokenExpiresAt());
        verify(authSessionRepository).save(session);
    }

    @Test
    void Given_NoSession_When_RemoveAdminOverride_Then_DoesNothing() {
        // Arrange
        when(authSessionRepository.findById(userId)).thenReturn(Optional.empty());

        // Act
        tokenStorageService.removeAdminOverride(userId);

        // Assert
        verify(authSessionRepository, never()).save(any());
    }

    @Test
    void Given_SessionWithAdminOverride_When_RemoveAdminOverride_Then_ClearsOverride() {
        // Arrange
        AuthSession session = new AuthSession(userId, jwtToken, role, expiry);
        session.setAdminOverride(true);
        session.setAdminOverrideExpiresAt(Instant.now().plusSeconds(600));
        when(authSessionRepository.findById(userId)).thenReturn(Optional.of(session));

        // Act
        tokenStorageService.removeAdminOverride(userId);

        // Assert
        assertFalse(session.isAdminOverride());
        assertNull(session.getAdminOverrideExpiresAt());
        verify(authSessionRepository).save(session);
    }

    @Test
    void Given_ExpiredAdminOverride_When_GetAdminOverrideTtlSeconds_Then_ReturnsNull() {
        // Arrange
        AuthSession session = new AuthSession(userId, jwtToken, role, expiry);
        session.setAdminOverride(true);
        session.setAdminOverrideExpiresAt(Instant.now().minusSeconds(10));
        when(authSessionRepository.findById(userId)).thenReturn(Optional.of(session));

        // Act
        Long result = tokenStorageService.getAdminOverrideTtlSeconds(userId);

        // Assert
        assertNull(result);
    }

    @Test
    void Given_ActiveAdminOverride_When_GetAdminOverrideTtlSeconds_Then_ReturnsPositiveValue() {
        // Arrange
        AuthSession session = new AuthSession(userId, jwtToken, role, expiry);
        session.setAdminOverride(true);
        session.setAdminOverrideExpiresAt(Instant.now().plusSeconds(600));
        when(authSessionRepository.findById(userId)).thenReturn(Optional.of(session));

        // Act
        Long result = tokenStorageService.getAdminOverrideTtlSeconds(userId);

        // Assert
        assertNotNull(result);
        assertTrue(result > 0);
        assertTrue(result <= 600);
    }

    @Test
    void Given_NullAdminOverride_When_GetAdminOverrideTtlSeconds_Then_ReturnsNull() {
        // Arrange
        AuthSession session = new AuthSession(userId, jwtToken, role, expiry);
        session.setAdminOverride(false);
        when(authSessionRepository.findById(userId)).thenReturn(Optional.of(session));

        // Act
        Long result = tokenStorageService.getAdminOverrideTtlSeconds(userId);

        // Assert
        assertNull(result);
    }

    @Test
    void Given_NullRefreshToken_When_StoreSmartschoolTokens_Then_NullRefreshTokenIsStored() {
        // Arrange
        AuthSession session = new AuthSession(userId, jwtToken, role, expiry);
        when(authSessionRepository.findById(userId)).thenReturn(Optional.of(session));
        String accessToken = "ss-access";

        // Act
        tokenStorageService.storeSmartschoolTokens(userId, accessToken, null);

        // Assert
        assertEquals(accessToken, session.getSmartschoolAccessToken());
        assertNull(session.getSmartschoolRefreshToken());
        assertNull(session.getSmartschoolRefreshTokenExpiresAt());
    }

    @Test
    void Given_NoAdminOverrideSession_When_StoreAdminOverride_Then_ThrowsException() {
        // Arrange
        when(authSessionRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalStateException.class, () ->
            tokenStorageService.storeAdminOverride(userId, 30));
    }
}