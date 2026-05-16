package edu.ap.bib_backend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmartschoolTokenRefreshServiceTest {

    @Mock
    private TokenStorageService tokenStorageService;

    @InjectMocks
    private SmartschoolTokenRefreshService service;

    @Test
    void Given_ValidAccessTokenExists_When_GetValidSmartschoolAccessToken_Then_ReturnsTokenWithoutRefresh() {
        when(tokenStorageService.getSmartschoolAccessToken("user123")).thenReturn("existing-token");

        String result = service.getValidSmartschoolAccessToken("user123");

        assertEquals("existing-token", result);
        verify(tokenStorageService, never()).getSmartschoolRefreshToken(anyString());
    }

    @Test
    void Given_RefreshTokenIsNull_When_RefreshTokens_Then_ThrowsIllegalStateException() {
        when(tokenStorageService.getSmartschoolRefreshToken("user123")).thenReturn(null);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> service.refreshTokens("user123"));

        assertTrue(exception.getMessage().contains("No valid refresh token for user"));
    }
}
