package edu.ap.bib_backend.service;

import edu.ap.bib_backend.jpa.entity.AuthSession;
import edu.ap.bib_backend.jpa.repository.AuthSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenStorageServiceTest {

    @Mock
    private AuthSessionRepository authSessionRepository;

    private TokenStorageService tokenStorageService;

    @BeforeEach
    void setUp() {
        tokenStorageService = new TokenStorageService(authSessionRepository);
    }

    @Test
    void givenNewSession_whenStoreToken_thenCreateAndSaveSession() {
        String userId = "user-1";
        String jwtToken = "jwt-token";
        String role = "ROLE_STUDENT";
        Instant expiresAt = Instant.now().plusSeconds(3600);

        when(authSessionRepository.findById(userId)).thenReturn(Optional.empty());

        tokenStorageService.storeToken(userId, jwtToken, role, expiresAt);

        ArgumentCaptor<AuthSession> captor = ArgumentCaptor.forClass(AuthSession.class);
        verify(authSessionRepository).save(captor.capture());

        AuthSession saved = captor.getValue();

        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getJwtToken()).isEqualTo(jwtToken);
        assertThat(saved.getRole()).isEqualTo(role);
        assertThat(saved.getJwtExpiresAt()).isEqualTo(expiresAt);
    }

    @Test
    void givenExistingSession_whenStoreToken_thenUpdateAndSaveSession() {
        String userId = "user-1";

        AuthSession existing = new AuthSession(
                userId,
                "old-token",
                "ROLE_STUDENT",
                Instant.now().minusSeconds(60));

        Instant newExpiresAt = Instant.now().plusSeconds(3600);

        when(authSessionRepository.findById(userId)).thenReturn(Optional.of(existing));

        tokenStorageService.storeToken(userId, "new-token", "ROLE_TEACHER", newExpiresAt);

        verify(authSessionRepository).save(existing);

        assertThat(existing.getJwtToken()).isEqualTo("new-token");
        assertThat(existing.getRole()).isEqualTo("ROLE_TEACHER");
        assertThat(existing.getJwtExpiresAt()).isEqualTo(newExpiresAt);
    }

    @Test
    void givenReplaceToken_whenCalled_thenStoresToken() {
        String userId = "user-1";
        Instant expiresAt = Instant.now().plusSeconds(3600);

        when(authSessionRepository.findById(userId)).thenReturn(Optional.empty());

        tokenStorageService.replaceToken(userId, "jwt-token", "ROLE_STUDENT", expiresAt);

        ArgumentCaptor<AuthSession> captor = ArgumentCaptor.forClass(AuthSession.class);
        verify(authSessionRepository).save(captor.capture());

        assertThat(captor.getValue().getJwtToken()).isEqualTo("jwt-token");
        assertThat(captor.getValue().getRole()).isEqualTo("ROLE_STUDENT");
        assertThat(captor.getValue().getJwtExpiresAt()).isEqualTo(expiresAt);
    }

    @Test
    void givenExistingSession_whenGetSession_thenReturnSession() {
        AuthSession session = new AuthSession(
                "user-1",
                "jwt-token",
                "ROLE_STUDENT",
                Instant.now().plusSeconds(3600));

        when(authSessionRepository.findById("user-1")).thenReturn(Optional.of(session));

        Optional<AuthSession> result = tokenStorageService.getSession("user-1");

        assertThat(result).contains(session);
    }

    @Test
    void givenActiveToken_whenGetToken_thenReturnJwtToken() {
        AuthSession session = new AuthSession(
                "user-1",
                "jwt-token",
                "ROLE_STUDENT",
                Instant.now().plusSeconds(3600));

        when(authSessionRepository.findById("user-1")).thenReturn(Optional.of(session));

        String result = tokenStorageService.getToken("user-1");

        assertThat(result).isEqualTo("jwt-token");
    }

    @Test
    void givenExpiredToken_whenGetToken_thenReturnNull() {
        AuthSession session = new AuthSession(
                "user-1",
                "expired-token",
                "ROLE_STUDENT",
                Instant.now().minusSeconds(1));

        when(authSessionRepository.findById("user-1")).thenReturn(Optional.of(session));

        String result = tokenStorageService.getToken("user-1");

        assertThat(result).isNull();
    }

    @Test
    void givenSessionWithoutJwtExpiry_whenGetToken_thenReturnNull() {
        AuthSession session = new AuthSession(
                "user-1",
                "jwt-token",
                "ROLE_STUDENT",
                null);

        when(authSessionRepository.findById("user-1")).thenReturn(Optional.of(session));

        String result = tokenStorageService.getToken("user-1");

        assertThat(result).isNull();
    }

    @Test
    void givenActiveToken_whenHasActiveToken_thenReturnTrue() {
        AuthSession session = new AuthSession(
                "user-1",
                "jwt-token",
                "ROLE_STUDENT",
                Instant.now().plusSeconds(3600));

        when(authSessionRepository.findById("user-1")).thenReturn(Optional.of(session));

        boolean result = tokenStorageService.hasActiveToken("user-1");

        assertThat(result).isTrue();
    }

    @Test
    void givenExpiredToken_whenHasActiveToken_thenReturnFalse() {
        AuthSession session = new AuthSession(
                "user-1",
                "jwt-token",
                "ROLE_STUDENT",
                Instant.now().minusSeconds(1));

        when(authSessionRepository.findById("user-1")).thenReturn(Optional.of(session));

        boolean result = tokenStorageService.hasActiveToken("user-1");

        assertThat(result).isFalse();
    }

    @Test
    void givenCurrentToken_whenIsCurrentToken_thenReturnTrue() {
        AuthSession session = new AuthSession(
                "user-1",
                "jwt-token",
                "ROLE_STUDENT",
                Instant.now().plusSeconds(3600));

        when(authSessionRepository.findById("user-1")).thenReturn(Optional.of(session));

        boolean result = tokenStorageService.isCurrentToken("user-1", "jwt-token");

        assertThat(result).isTrue();
    }

    @Test
    void givenDifferentToken_whenIsCurrentToken_thenReturnFalse() {
        AuthSession session = new AuthSession(
                "user-1",
                "jwt-token",
                "ROLE_STUDENT",
                Instant.now().plusSeconds(3600));

        when(authSessionRepository.findById("user-1")).thenReturn(Optional.of(session));

        boolean result = tokenStorageService.isCurrentToken("user-1", "other-token");

        assertThat(result).isFalse();
    }

    @Test
    void givenCurrentToken_whenHasActiveTokenWithJwt_thenReturnTrue() {
        AuthSession session = new AuthSession(
                "user-1",
                "jwt-token",
                "ROLE_STUDENT",
                Instant.now().plusSeconds(3600));

        when(authSessionRepository.findById("user-1")).thenReturn(Optional.of(session));

        boolean result = tokenStorageService.hasActiveToken("user-1", "jwt-token");

        assertThat(result).isTrue();
    }

    @Test
    void givenUserId_whenRevokeToken_thenDeleteSession() {
        tokenStorageService.revokeToken("user-1");

        verify(authSessionRepository).deleteById("user-1");
    }

    @Test
    void givenExistingSession_whenStoreUserMetadata_thenUpdateRole() {
        AuthSession session = new AuthSession(
                "user-1",
                "jwt-token",
                "ROLE_STUDENT",
                Instant.now().plusSeconds(3600));

        when(authSessionRepository.findById("user-1")).thenReturn(Optional.of(session));

        tokenStorageService.storeUserMetadata("user-1", "ROLE_ADMIN");

        assertThat(session.getRole()).isEqualTo("ROLE_ADMIN");
        verify(authSessionRepository).save(session);
    }

    @Test
    void givenExistingSession_whenRemoveUserMetadata_thenClearRole() {
        AuthSession session = new AuthSession(
                "user-1",
                "jwt-token",
                "ROLE_STUDENT",
                Instant.now().plusSeconds(3600));

        when(authSessionRepository.findById("user-1")).thenReturn(Optional.of(session));

        tokenStorageService.removeUserMetadata("user-1");

        assertThat(session.getRole()).isEqualTo("");
        verify(authSessionRepository).save(session);
    }

    @Test
    void givenExistingSession_whenGetUserRole_thenReturnRole() {
        AuthSession session = new AuthSession(
                "user-1",
                "jwt-token",
                "ROLE_STUDENT",
                Instant.now().plusSeconds(3600));

        when(authSessionRepository.findById("user-1")).thenReturn(Optional.of(session));

        String result = tokenStorageService.getUserRole("user-1");

        assertThat(result).isEqualTo("ROLE_STUDENT");
    }

    @Test
    void givenMissingSession_whenGetUserRole_thenReturnNull() {
        when(authSessionRepository.findById("user-1")).thenReturn(Optional.empty());

        String result = tokenStorageService.getUserRole("user-1");

        assertThat(result).isNull();
    }

    @Test
    void givenExistingSession_whenStoreSmartschoolTokens_thenSaveAccessAndRefreshTokens() {
        AuthSession session = new AuthSession(
                "user-1",
                "jwt-token",
                "ROLE_STUDENT",
                Instant.now().plusSeconds(3600));

        when(authSessionRepository.findById("user-1")).thenReturn(Optional.of(session));

        tokenStorageService.storeSmartschoolTokens(
                "user-1",
                "access-token",
                "refresh-token");

        assertThat(session.getSmartschoolAccessToken()).isEqualTo("access-token");
        assertThat(session.getSmartschoolAccessTokenExpiresAt()).isAfter(Instant.now());
        assertThat(session.getSmartschoolRefreshToken()).isEqualTo("refresh-token");
        assertThat(session.getSmartschoolRefreshTokenExpiresAt()).isAfter(Instant.now());

        verify(authSessionRepository).save(session);
    }

    @Test
    void givenExistingSessionAndBlankRefreshToken_whenStoreSmartschoolTokens_thenOnlySaveAccessToken() {
        AuthSession session = new AuthSession(
                "user-1",
                "jwt-token",
                "ROLE_STUDENT",
                Instant.now().plusSeconds(3600));
        session.setSmartschoolRefreshToken("existing-refresh-token");

        when(authSessionRepository.findById("user-1")).thenReturn(Optional.of(session));

        tokenStorageService.storeSmartschoolTokens(
                "user-1",
                "new-access-token",
                "");

        assertThat(session.getSmartschoolAccessToken()).isEqualTo("new-access-token");
        assertThat(session.getSmartschoolRefreshToken()).isEqualTo("existing-refresh-token");

        verify(authSessionRepository).save(session);
    }

    @Test
    void givenMissingSession_whenStoreSmartschoolTokens_thenThrowException() {
        when(authSessionRepository.findById("user-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tokenStorageService.storeSmartschoolTokens(
                "user-1",
                "access-token",
                "refresh-token"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No auth session found for user user-1");
    }

    @Test
    void givenValidRefreshToken_whenGetSmartschoolRefreshToken_thenReturnToken() {
        AuthSession session = new AuthSession(
                "user-1",
                "jwt-token",
                "ROLE_STUDENT",
                Instant.now().plusSeconds(3600));

        session.setSmartschoolRefreshToken("refresh-token");
        session.setSmartschoolRefreshTokenExpiresAt(Instant.now().plusSeconds(3600));

        when(authSessionRepository.findById("user-1")).thenReturn(Optional.of(session));

        String result = tokenStorageService.getSmartschoolRefreshToken("user-1");

        assertThat(result).isEqualTo("refresh-token");
    }

    @Test
    void givenExpiredRefreshToken_whenGetSmartschoolRefreshToken_thenReturnNull() {
        AuthSession session = new AuthSession(
                "user-1",
                "jwt-token",
                "ROLE_STUDENT",
                Instant.now().plusSeconds(3600));

        session.setSmartschoolRefreshToken("refresh-token");
        session.setSmartschoolRefreshTokenExpiresAt(Instant.now().minusSeconds(1));

        when(authSessionRepository.findById("user-1")).thenReturn(Optional.of(session));

        String result = tokenStorageService.getSmartschoolRefreshToken("user-1");

        assertThat(result).isNull();
    }

    @Test
    void givenRefreshTokenWithoutExpiry_whenGetSmartschoolRefreshToken_thenReturnToken() {
        AuthSession session = new AuthSession(
                "user-1",
                "jwt-token",
                "ROLE_STUDENT",
                Instant.now().plusSeconds(3600));

        session.setSmartschoolRefreshToken("refresh-token");
        session.setSmartschoolRefreshTokenExpiresAt(null);

        when(authSessionRepository.findById("user-1")).thenReturn(Optional.of(session));

        String result = tokenStorageService.getSmartschoolRefreshToken("user-1");

        assertThat(result).isEqualTo("refresh-token");
    }

    @Test
    void givenValidAccessToken_whenGetSmartschoolAccessToken_thenReturnToken() {
        AuthSession session = new AuthSession(
                "user-1",
                "jwt-token",
                "ROLE_STUDENT",
                Instant.now().plusSeconds(3600));

        session.setSmartschoolAccessToken("access-token");
        session.setSmartschoolAccessTokenExpiresAt(Instant.now().plusSeconds(3600));

        when(authSessionRepository.findById("user-1")).thenReturn(Optional.of(session));

        String result = tokenStorageService.getSmartschoolAccessToken("user-1");

        assertThat(result).isEqualTo("access-token");
    }

    @Test
    void givenExpiredAccessToken_whenGetSmartschoolAccessToken_thenReturnNull() {
        AuthSession session = new AuthSession(
                "user-1",
                "jwt-token",
                "ROLE_STUDENT",
                Instant.now().plusSeconds(3600));

        session.setSmartschoolAccessToken("access-token");
        session.setSmartschoolAccessTokenExpiresAt(Instant.now().minusSeconds(1));

        when(authSessionRepository.findById("user-1")).thenReturn(Optional.of(session));

        String result = tokenStorageService.getSmartschoolAccessToken("user-1");

        assertThat(result).isNull();
    }

    @Test
    void givenExistingSession_whenRemoveSmartschoolAccessToken_thenClearAccessToken() {
        AuthSession session = new AuthSession(
                "user-1",
                "jwt-token",
                "ROLE_STUDENT",
                Instant.now().plusSeconds(3600));

        session.setSmartschoolAccessToken("access-token");
        session.setSmartschoolAccessTokenExpiresAt(Instant.now().plusSeconds(3600));

        when(authSessionRepository.findById("user-1")).thenReturn(Optional.of(session));

        tokenStorageService.removeSmartschoolAccessToken("user-1");

        assertThat(session.getSmartschoolAccessToken()).isNull();
        assertThat(session.getSmartschoolAccessTokenExpiresAt()).isNull();

        verify(authSessionRepository).save(session);
    }

    @Test
    void givenExistingSession_whenStoreAdminOverride_thenActivateOverride() {
        AuthSession session = new AuthSession(
                "user-1",
                "jwt-token",
                "ROLE_STUDENT",
                Instant.now().plusSeconds(3600));

        when(authSessionRepository.findById("user-1")).thenReturn(Optional.of(session));

        tokenStorageService.storeAdminOverride("user-1", 30);

        assertThat(session.isAdminOverride()).isTrue();
        assertThat(session.getAdminOverrideExpiresAt()).isAfter(Instant.now());

        verify(authSessionRepository).save(session);
    }

    @Test
    void givenMissingSession_whenStoreAdminOverride_thenThrowException() {
        when(authSessionRepository.findById("user-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tokenStorageService.storeAdminOverride("user-1", 30))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No auth session found for user user-1");
    }

    @Test
    void givenActiveAdminOverride_whenHasAdminOverride_thenReturnTrue() {
        AuthSession session = new AuthSession(
                "user-1",
                "jwt-token",
                "ROLE_STUDENT",
                Instant.now().plusSeconds(3600));

        session.setAdminOverride(true);
        session.setAdminOverrideExpiresAt(Instant.now().plusSeconds(1800));

        when(authSessionRepository.findById("user-1")).thenReturn(Optional.of(session));

        boolean result = tokenStorageService.hasAdminOverride("user-1");

        assertThat(result).isTrue();
    }

    @Test
    void givenExpiredAdminOverride_whenHasAdminOverride_thenReturnFalse() {
        AuthSession session = new AuthSession(
                "user-1",
                "jwt-token",
                "ROLE_STUDENT",
                Instant.now().plusSeconds(3600));

        session.setAdminOverride(true);
        session.setAdminOverrideExpiresAt(Instant.now().minusSeconds(1));

        when(authSessionRepository.findById("user-1")).thenReturn(Optional.of(session));

        boolean result = tokenStorageService.hasAdminOverride("user-1");

        assertThat(result).isFalse();
    }

    @Test
    void givenExistingSession_whenRemoveAdminOverride_thenDeactivateOverride() {
        AuthSession session = new AuthSession(
                "user-1",
                "jwt-token",
                "ROLE_STUDENT",
                Instant.now().plusSeconds(3600));

        session.setAdminOverride(true);
        session.setAdminOverrideExpiresAt(Instant.now().plusSeconds(1800));

        when(authSessionRepository.findById("user-1")).thenReturn(Optional.of(session));

        tokenStorageService.removeAdminOverride("user-1");

        assertThat(session.isAdminOverride()).isFalse();
        assertThat(session.getAdminOverrideExpiresAt()).isNull();

        verify(authSessionRepository).save(session);
    }

    @Test
    void givenActiveAdminOverride_whenGetAdminOverrideTtlSeconds_thenReturnPositiveTtl() {
        AuthSession session = new AuthSession(
                "user-1",
                "jwt-token",
                "ROLE_STUDENT",
                Instant.now().plusSeconds(3600));

        session.setAdminOverride(true);
        session.setAdminOverrideExpiresAt(Instant.now().plusSeconds(1800));

        when(authSessionRepository.findById("user-1")).thenReturn(Optional.of(session));

        Long result = tokenStorageService.getAdminOverrideTtlSeconds("user-1");

        assertThat(result).isNotNull();
        assertThat(result).isBetween(1700L, 1800L);
    }

    @Test
    void givenNoAdminOverride_whenGetAdminOverrideTtlSeconds_thenReturnNull() {
        AuthSession session = new AuthSession(
                "user-1",
                "jwt-token",
                "ROLE_STUDENT",
                Instant.now().plusSeconds(3600));

        session.setAdminOverride(false);
        session.setAdminOverrideExpiresAt(null);

        when(authSessionRepository.findById("user-1")).thenReturn(Optional.of(session));

        Long result = tokenStorageService.getAdminOverrideTtlSeconds("user-1");

        assertThat(result).isNull();
    }
}