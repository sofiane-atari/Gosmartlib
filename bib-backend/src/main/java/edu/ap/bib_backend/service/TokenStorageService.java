package edu.ap.bib_backend.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;

@Service
public class TokenStorageService {

    private final RedisTemplate<String, String> redisTemplate;
    private final int TOKEN_EXPIRY_HOURS = 24;

    public TokenStorageService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Store JWT token mapped to user ID (not username/personal data)
     * Key format: "token:userId:{userId}"
     * Value: JWT token
     */
    public void storeToken(String userId, String jwtToken) {
        String key = "token:userId:" + userId;
        redisTemplate.opsForValue().set(key, jwtToken, TOKEN_EXPIRY_HOURS, TimeUnit.HOURS);
    }

    /**
     * Retrieve JWT token by user ID
     */
    public String getToken(String userId) {
        String key = "token:userId:" + userId;
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * Check if user has an active token session
     */
    public boolean hasActiveToken(String userId) {
        String key = "token:userId:" + userId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * Revoke token by user ID (logout)
     */
    public void revokeToken(String userId) {
        String key = "token:userId:" + userId;
        redisTemplate.delete(key);
    }

    /**
     * Store additional user metadata (e.g., role) without PII
     * Key format: "user:userId:{userId}"
     * Value: serialized role info
     */
    public void storeUserMetadata(String userId, String role) {
        String key = "user:metadata:" + userId;
        redisTemplate.opsForValue().set(key, role, TOKEN_EXPIRY_HOURS, TimeUnit.HOURS);
    }

    /**
     * Retrieve user role by user ID
     */
    public String getUserRole(String userId) {
        String key = "user:metadata:" + userId;
        return redisTemplate.opsForValue().get(key);
    }
}