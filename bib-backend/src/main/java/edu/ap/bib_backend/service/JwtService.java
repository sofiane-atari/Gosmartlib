package edu.ap.bib_backend.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;

import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long expiration;

    public JwtService(@Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expiration) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.expiration = expiration;
    }

    // Call this in /api/auth/callback after Smartschool login
    public String generateToken(String username, String role) {
        return generateToken(username, role, Map.of());
    }

    public String generateToken(String username, String role, Map<String, Object> extraClaims) {
        return Jwts.builder()
                .subject(String.valueOf(username))
                .claim("roles", role)
                .claims(extraClaims)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(secretKey)
                .compact();
    }

    // Extract the username (sub) from a token
    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    // Extract any claim from the token
    public Object extractClaim(String token, String claimKey) {
        return parseClaims(token).get(claimKey);
    }

    public boolean isValid(String token) {
        try {
            parseClaims(token); // throws if expired or tampered
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Date extractExpiration(String token) {
        return parseClaims(token).getExpiration();
    }

    public Instant extractExpirationInstant(String token) {
        return extractExpiration(token).toInstant();
    }
}