package edu.ap.bib_backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class SmartschoolMessageServiceIntegrationTest {

    @Autowired
    private SmartschoolMessageService smartschoolMessageService;

    @MockitoBean
    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(smartschoolMessageService, "restTemplate", restTemplate);
    }

    @SuppressWarnings("unchecked")
@Test
    void Given_ValidPayload_When_SendMessage_Then_ApiCallSucceeds() {
        ReflectionTestUtils.setField(smartschoolMessageService, "smartschoolBaseUrl", "https://smartschool.be");
        when(restTemplate.exchange(
                any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), any(org.springframework.core.ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(Map.of("success", true)));

        assertDoesNotThrow(() -> smartschoolMessageService.sendMessage("test-token", "Test Title", "Test Body"));

        verify(restTemplate).exchange(
                contains("/Api/V1/sendmsg"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(org.springframework.core.ParameterizedTypeReference.class));
    }

    @SuppressWarnings("unchecked")
@Test
    void Given_ApiError_When_SendMessage_Then_ThrowsIllegalStateException() {
        ReflectionTestUtils.setField(smartschoolMessageService, "smartschoolBaseUrl", "https://smartschool.be");
        when(restTemplate.exchange(
                any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), any(org.springframework.core.ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.status(500).body(Map.of()));

        assertThrows(IllegalStateException.class,
                () -> smartschoolMessageService.sendMessage("test-token", "Title", "Body"));
    }

    @SuppressWarnings("unchecked")
@Test
    void Given_ApiReturnsFailure_When_SendMessage_Then_ThrowsIllegalStateException() {
        ReflectionTestUtils.setField(smartschoolMessageService, "smartschoolBaseUrl", "https://smartschool.be");
        when(restTemplate.exchange(
                any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), any(org.springframework.core.ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(Map.of("success", false)));

        assertThrows(IllegalStateException.class,
                () -> smartschoolMessageService.sendMessage("test-token", "Title", "Body"));
    }

    @SuppressWarnings("unchecked")
@Test
    void Given_NetworkError_When_SendMessage_Then_ThrowsIllegalStateException() {
        ReflectionTestUtils.setField(smartschoolMessageService, "smartschoolBaseUrl", "https://smartschool.be");
        when(restTemplate.exchange(
                any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), any(org.springframework.core.ParameterizedTypeReference.class)))
                .thenThrow(new RestClientException("Connection refused"));

        assertThrows(IllegalStateException.class,
                () -> smartschoolMessageService.sendMessage("test-token", "Title", "Body"));
    }
}
