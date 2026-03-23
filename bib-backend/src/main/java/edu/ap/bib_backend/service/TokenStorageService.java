package edu.ap.bib_backend.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;

@Service
public class TokenStorageService {

    private final RedisTemplate<String, String> redisTemplate;
    private final int JWT_EXPIRY_HOURS = 24;
    private static final long SMARTSCHOOL_ACCESS_TOKEN_EXPIRY_MINUTES = 55;

    public TokenStorageService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Store JWT token mapped to user ID (not username/personal data)
     * Key format: "token:userId:{userId}"
     * Value: JWT token
     */

    public void storeToken(String userId, String jwtToken) {
        redisTemplate.opsForValue().set(
            buildJwtKey(userId),
            jwtToken,
            JWT_EXPIRY_HOURS,
            TimeUnit.HOURS
        );
    }

    /**
     * Retrieve JWT token by user ID
     */
    public String getToken(String userId) {
        return redisTemplate.opsForValue().get(buildJwtKey(userId));
    }

    /**
     * Check if user has an active token session
     */

    public boolean hasActiveToken(String userId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(buildJwtKey(userId)));
    }

    /**
     * Revoke token by user ID (logout)
     */

    public void revokeToken(String userId) {
        redisTemplate.delete(buildJwtKey(userId));
        redisTemplate.delete(buildSmartschoolAccessTokenKey(userId));
    }

    /**
     * Store additional user metadata (e.g., role) without PII
     * Key format: "user:userId:{userId}"
     * Value: serialized role info
     */

    public void storeUserMetadata(String userId, String role) {
        redisTemplate.opsForValue().set(
            buildRoleKey(userId),
            role,
            JWT_EXPIRY_HOURS,
            TimeUnit.HOURS
        );
    }

    public void removeUserMetadata(String userId) {
        redisTemplate.delete(buildRoleKey(userId));
    }

    /**
     * Retrieve user role by user ID
     */

    public String getUserRole(String userId) {
        return redisTemplate.opsForValue().get(buildRoleKey(userId));
    }

    public void storeSmartschoolAccessToken(String userId, String accessToken) {
        redisTemplate.opsForValue().set(
            buildSmartschoolAccessTokenKey(userId),
            accessToken,
            SMARTSCHOOL_ACCESS_TOKEN_EXPIRY_MINUTES,
            TimeUnit.MINUTES
        );
    }

    public String getSmartschoolAccessToken(String userId) {
        return redisTemplate.opsForValue().get(buildSmartschoolAccessTokenKey(userId));
    }

    public void removeSmartschoolAccessToken(String userId) {
        redisTemplate.delete(buildSmartschoolAccessTokenKey(userId));
    }

    private String buildJwtKey(String userId) {
        return "auth:jwt:user:" + userId;
    }

    private String buildRoleKey(String userId) {
        return "auth:role:user:" + userId;
    }

    private String buildSmartschoolAccessTokenKey(String userId) {
        return "smartschool:access:user:" + userId;
    }
}