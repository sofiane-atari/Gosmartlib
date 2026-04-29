package edu.ap.bib_backend.jpa.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "auth_sessions")
@Getter
@Setter
public class AuthSession {

    @Id
    @Column(nullable = false)
    private String userId;

    @Column(nullable = false, length = 2048)
    private String jwtToken;

    @Column(nullable = false)
    private String role;

    @Column(nullable = false)
    private Instant jwtExpiresAt;

    @Column(length = 2048)
    private String smartschoolAccessToken;

    private Instant smartschoolAccessTokenExpiresAt;

    private boolean adminOverride;

    private Instant adminOverrideExpiresAt;

    public AuthSession() {
    }

    public AuthSession(String userId, String jwtToken, String role, Instant jwtExpiresAt) {
        this.userId = userId;
        this.jwtToken = jwtToken;
        this.role = role;
        this.jwtExpiresAt = jwtExpiresAt;
    }
}