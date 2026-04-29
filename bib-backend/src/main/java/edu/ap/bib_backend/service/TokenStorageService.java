package edu.ap.bib_backend.service;

import edu.ap.bib_backend.jpa.entity.AuthSession;
import edu.ap.bib_backend.jpa.repository.AuthSessionRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
public class TokenStorageService {

    private static final long SMARTSCHOOL_ACCESS_TOKEN_EXPIRY_MINUTES = 55;

    private final AuthSessionRepository authSessionRepository;

    public TokenStorageService(AuthSessionRepository authSessionRepository) {
        this.authSessionRepository = authSessionRepository;
    }

    public void storeToken(String userId, String jwtToken, String role, Instant jwtExpiresAt) {
        AuthSession session = authSessionRepository.findById(userId)
                .orElseGet(() -> new AuthSession(userId, jwtToken, role, jwtExpiresAt));

        session.setJwtToken(jwtToken);
        session.setRole(role);
        session.setJwtExpiresAt(jwtExpiresAt);

        authSessionRepository.save(session);
    }

    public void replaceToken(String userId, String jwtToken, String role, Instant jwtExpiresAt) {
        storeToken(userId, jwtToken, role, jwtExpiresAt);
    }

    public Optional<AuthSession> getSession(String userId) {
        return authSessionRepository.findById(userId);
    }

    public String getToken(String userId) {
        return authSessionRepository.findById(userId)
                .filter(session -> session.getJwtExpiresAt() != null)
                .filter(session -> session.getJwtExpiresAt().isAfter(Instant.now()))
                .map(AuthSession::getJwtToken)
                .orElse(null);
    }

    public boolean hasActiveToken(String userId) {
        return authSessionRepository.findById(userId)
                .filter(session -> session.getJwtExpiresAt() != null)
                .filter(session -> session.getJwtExpiresAt().isAfter(Instant.now()))
                .isPresent();
    }

    public boolean isCurrentToken(String userId, String jwtToken) {
        String storedToken = getToken(userId);
        return storedToken != null && storedToken.equals(jwtToken);
    }

    public boolean hasActiveToken(String userId, String jwtToken) {
        return isCurrentToken(userId, jwtToken);
    }

    public void revokeToken(String userId) {
        authSessionRepository.deleteById(userId);
    }

    public void storeUserMetadata(String userId, String role) {
        authSessionRepository.findById(userId).ifPresent(session -> {
            session.setRole(role);
            authSessionRepository.save(session);
        });
    }

    public void removeUserMetadata(String userId) {
        authSessionRepository.findById(userId).ifPresent(session -> {
            session.setRole("");
            authSessionRepository.save(session);
        });
    }

    public String getUserRole(String userId) {
        return authSessionRepository.findById(userId)
                .map(AuthSession::getRole)
                .orElse(null);
    }

    public void storeSmartschoolAccessToken(String userId, String accessToken) {
        AuthSession session = authSessionRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("No auth session found for user " + userId));

        session.setSmartschoolAccessToken(accessToken);
        session.setSmartschoolAccessTokenExpiresAt(
                Instant.now().plusSeconds(SMARTSCHOOL_ACCESS_TOKEN_EXPIRY_MINUTES * 60));

        authSessionRepository.save(session);
    }

    public String getSmartschoolAccessToken(String userId) {
        return authSessionRepository.findById(userId)
                .filter(session -> session.getSmartschoolAccessToken() != null)
                .filter(session -> session.getSmartschoolAccessTokenExpiresAt() != null)
                .filter(session -> session.getSmartschoolAccessTokenExpiresAt().isAfter(Instant.now()))
                .map(AuthSession::getSmartschoolAccessToken)
                .orElse(null);
    }

    public void removeSmartschoolAccessToken(String userId) {
        authSessionRepository.findById(userId).ifPresent(session -> {
            session.setSmartschoolAccessToken(null);
            session.setSmartschoolAccessTokenExpiresAt(null);
            authSessionRepository.save(session);
        });
    }

    public void storeAdminOverride(String userId, long ttlMinutes) {
        AuthSession session = authSessionRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("No auth session found for user " + userId));

        session.setAdminOverride(true);
        session.setAdminOverrideExpiresAt(Instant.now().plusSeconds(ttlMinutes * 60));

        authSessionRepository.save(session);
    }

    public boolean hasAdminOverride(String userId) {
        return authSessionRepository.findById(userId)
                .filter(AuthSession::isAdminOverride)
                .filter(session -> session.getAdminOverrideExpiresAt() != null)
                .filter(session -> session.getAdminOverrideExpiresAt().isAfter(Instant.now()))
                .isPresent();
    }

    public void removeAdminOverride(String userId) {
        authSessionRepository.findById(userId).ifPresent(session -> {
            session.setAdminOverride(false);
            session.setAdminOverrideExpiresAt(null);
            authSessionRepository.save(session);
        });
    }

    public Long getAdminOverrideTtlSeconds(String userId) {
        return authSessionRepository.findById(userId)
                .filter(AuthSession::isAdminOverride)
                .map(AuthSession::getAdminOverrideExpiresAt)
                .filter(expiresAt -> expiresAt.isAfter(Instant.now()))
                .map(expiresAt -> Duration.between(Instant.now(), expiresAt).getSeconds())
                .orElse(null);
    }
}