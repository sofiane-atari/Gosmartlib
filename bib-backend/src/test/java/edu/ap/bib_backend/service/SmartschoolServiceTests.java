package edu.ap.bib_backend.service;

import edu.ap.bib_backend.dto.SmartschoolTokenResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmartschoolServiceTests {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private TokenStorageService tokenStorageService;

    @Test
    void Given_ValidData_When_SendMessage_Then_RestTemplateIsCalled() {
        // Arrange
        SmartschoolMessageService service = new SmartschoolMessageService("https://test.be/Api/V1/fulluserinfo");
        ReflectionTestUtils.setField(service, "restTemplate", restTemplate);
        
        ResponseEntity<Map<String, Object>> response = ResponseEntity.ok(Map.of("success", true));
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(), any(org.springframework.core.ParameterizedTypeReference.class)))
                .thenReturn(response);

        // Act
        service.sendMessage("token", "Title", "Body");

        // Assert
        verify(restTemplate).exchange(any(URI.class), eq(HttpMethod.GET), any(), any(org.springframework.core.ParameterizedTypeReference.class));
    }

    @Test
    void Given_ExistingToken_When_GetValidSmartschoolAccessToken_Then_ReturnsTokenWithoutRefresh() {
        // Arrange
        SmartschoolTokenRefreshService service = new SmartschoolTokenRefreshService(tokenStorageService, "id", "secret", "uri", "token-uri");
        when(tokenStorageService.getSmartschoolAccessToken("user123")).thenReturn("existing-token");

        // Act
        String result = service.getValidSmartschoolAccessToken("user123");

        // Assert
        assertEquals("existing-token", result);
        verify(tokenStorageService, never()).getSmartschoolRefreshToken(anyString());
    }
}
