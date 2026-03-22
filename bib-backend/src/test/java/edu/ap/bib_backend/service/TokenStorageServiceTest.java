package edu.ap.bib_backend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenStorageServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Test
    void givenUserIdAndJwtToken_WhenStoreToken_ThenStoreTokenWithExpectedKeyAndExpiry() {
        // Arrange
        TokenStorageService tokenStorageService = new TokenStorageService(redisTemplate);
        String userId = "user-1";
        String token = "jwt-token-value";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // Act
        tokenStorageService.storeToken(userId, token);

        // Assert
        verify(redisTemplate, times(1)).opsForValue();
        verify(valueOperations, times(1)).set("token:userId:user-1", token, 24, TimeUnit.HOURS);
    }

    @Test
    void givenExistingStoredToken_WhenGetToken_ThenReturnTokenValue() {
        // Arrange
        TokenStorageService tokenStorageService = new TokenStorageService(redisTemplate);
        String userId = "user-2";
        String expectedToken = "stored-token";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("token:userId:user-2")).thenReturn(expectedToken);

        // Act
        String result = tokenStorageService.getToken(userId);

        // Assert
        assertEquals(expectedToken, result);
        verify(redisTemplate, times(1)).opsForValue();
        verify(valueOperations, times(1)).get("token:userId:user-2");
    }

    @Test
    void givenExistingTokenKey_WhenHasActiveToken_ThenReturnTrue() {
        // Arrange
        TokenStorageService tokenStorageService = new TokenStorageService(redisTemplate);
        String userId = "user-3";
        when(redisTemplate.hasKey("token:userId:user-3")).thenReturn(true);

        // Act
        boolean result = tokenStorageService.hasActiveToken(userId);

        // Assert
        assertTrue(result);
        verify(redisTemplate, times(1)).hasKey("token:userId:user-3");
    }

    @Test
    void givenMissingTokenKey_WhenHasActiveToken_ThenReturnFalse() {
        // Arrange
        TokenStorageService tokenStorageService = new TokenStorageService(redisTemplate);
        String userId = "user-4";
        when(redisTemplate.hasKey("token:userId:user-4")).thenReturn(null);

        // Act
        boolean result = tokenStorageService.hasActiveToken(userId);

        // Assert
        assertFalse(result);
        verify(redisTemplate, times(1)).hasKey("token:userId:user-4");
    }

    @Test
    void givenUserId_WhenRevokeToken_ThenDeleteTokenKey() {
        // Arrange
        TokenStorageService tokenStorageService = new TokenStorageService(redisTemplate);
        String userId = "user-5";

        // Act
        tokenStorageService.revokeToken(userId);

        // Assert
        verify(redisTemplate, times(1)).delete("token:userId:user-5");
    }

    @Test
    void givenUserIdAndRole_WhenStoreUserMetadata_ThenStoreRoleWithExpectedKeyAndExpiry() {
        // Arrange
        TokenStorageService tokenStorageService = new TokenStorageService(redisTemplate);
        String userId = "user-6";
        String role = "ROLE_STUDENT";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // Act
        tokenStorageService.storeUserMetadata(userId, role);

        // Assert
        verify(redisTemplate, times(1)).opsForValue();
        verify(valueOperations, times(1)).set("user:metadata:user-6", role, 24, TimeUnit.HOURS);
    }

    @Test
    void givenStoredUserMetadata_WhenGetUserRole_ThenReturnStoredRole() {
        // Arrange
        TokenStorageService tokenStorageService = new TokenStorageService(redisTemplate);
        String userId = "user-7";
        String expectedRole = "ROLE_LIBRARIAN";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("user:metadata:user-7")).thenReturn(expectedRole);

        // Act
        String result = tokenStorageService.getUserRole(userId);

        // Assert
        assertEquals(expectedRole, result);
        verify(redisTemplate, times(1)).opsForValue();
        verify(valueOperations, times(1)).get("user:metadata:user-7");
    }
}