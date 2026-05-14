package edu.ap.bib_backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmartschoolMessageServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Captor
    private ArgumentCaptor<HttpEntity<MultiValueMap<String, String>>> requestCaptor;

    private SmartschoolMessageService service;

    @BeforeEach
    void setUp() {
        service = new SmartschoolMessageService("https://smartschool.be/Api/V1/fulluserinfo");
        ReflectionTestUtils.setField(service, "restTemplate", restTemplate);
    }

    @Test
    void Given_ValidTokenTitleAndBody_When_SendMessage_Then_PostsToSmartschoolApi() {
        ResponseEntity<Map<String, Object>> response = ResponseEntity.ok(Map.of("success", true));
        when(restTemplate.exchange(
                any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), any(org.springframework.core.ParameterizedTypeReference.class)))
                .thenReturn(response);

        service.sendMessage("valid-token", "Test Title", "Test Body");

        verify(restTemplate).exchange(
                eq("https://smartschool.be/Api/V1/sendmsg"),
                eq(HttpMethod.POST),
                requestCaptor.capture(),
                any(org.springframework.core.ParameterizedTypeReference.class));
        MultiValueMap<String, String> body = requestCaptor.getValue().getBody();
        assertNotNull(body);
        assertEquals("valid-token", body.getFirst("access_token"));
        assertEquals("Test Title", body.getFirst("messageTitle"));
        assertEquals("Test Body", body.getFirst("messageBody"));
    }

    @Test
    void Given_ApiReturnsNon2xxStatus_When_SendMessage_Then_ThrowsIllegalStateException() {
        ResponseEntity<Map<String, Object>> response = ResponseEntity.status(500).body(Map.of());
        when(restTemplate.exchange(
                any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), any(org.springframework.core.ParameterizedTypeReference.class)))
                .thenReturn(response);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> service.sendMessage("token", "Title", "Body"));

        assertTrue(exception.getMessage().contains("Failed to send Smartschool message"));
    }

    @Test
    void Given_ApiReturnsSuccessFalse_When_SendMessage_Then_ThrowsIllegalStateException() {
        ResponseEntity<Map<String, Object>> response = ResponseEntity.ok(Map.of("success", false));
        when(restTemplate.exchange(
                any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), any(org.springframework.core.ParameterizedTypeReference.class)))
                .thenReturn(response);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> service.sendMessage("token", "Title", "Body"));

        assertTrue(exception.getMessage().contains("Smartschool API returned failure"));
    }

    @Test
    void Given_ApiReturnsNullBody_When_SendMessage_Then_CompletesSuccessfully() {
        ResponseEntity<Map<String, Object>> response = ResponseEntity.ok(null);
        when(restTemplate.exchange(
                any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), any(org.springframework.core.ParameterizedTypeReference.class)))
                .thenReturn(response);

        assertDoesNotThrow(() -> service.sendMessage("token", "Title", "Body"));
    }

    @Test
    void Given_RestClientExceptionThrown_When_SendMessage_Then_ThrowsIllegalStateException() {
        when(restTemplate.exchange(
                any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), any(org.springframework.core.ParameterizedTypeReference.class)))
                .thenThrow(new RestClientException("Connection refused"));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> service.sendMessage("token", "Title", "Body"));

        assertTrue(exception.getMessage().contains("Failed to send Smartschool message"));
        assertNotNull(exception.getCause());
        assertInstanceOf(RestClientException.class, exception.getCause());
    }

    @Test
    void Given_NullBody_When_SendMessage_Then_StillSendsWithNullBody() {
        ResponseEntity<Map<String, Object>> response = ResponseEntity.ok(Map.of("success", true));
        when(restTemplate.exchange(
                any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), any(org.springframework.core.ParameterizedTypeReference.class)))
                .thenReturn(response);

        assertDoesNotThrow(() -> service.sendMessage("token", "Title", null));

        verify(restTemplate).exchange(
                any(String.class), eq(HttpMethod.POST), requestCaptor.capture(), any(org.springframework.core.ParameterizedTypeReference.class));
        assertNull(requestCaptor.getValue().getBody().getFirst("messageBody"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "Short"})
    void Given_VariousBodyLengths_When_SendMessage_Then_HandlesCorrectly(String body) {
        ResponseEntity<Map<String, Object>> response = ResponseEntity.ok(Map.of("success", true));
        when(restTemplate.exchange(
                any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), any(org.springframework.core.ParameterizedTypeReference.class)))
                .thenReturn(response);

        assertDoesNotThrow(() -> service.sendMessage("token", "Title", body));

        verify(restTemplate).exchange(
                any(String.class), eq(HttpMethod.POST), requestCaptor.capture(), any(org.springframework.core.ParameterizedTypeReference.class));
        assertEquals(body, requestCaptor.getValue().getBody().getFirst("messageBody"));
    }

    @Test
    void Given_SuccessIsNotBoolean_When_SendMessage_Then_IgnoresAndCompletes() {
        ResponseEntity<Map<String, Object>> response = ResponseEntity.ok(Map.of("success", "true"));
        when(restTemplate.exchange(
                any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), any(org.springframework.core.ParameterizedTypeReference.class)))
                .thenReturn(response);

        assertDoesNotThrow(() -> service.sendMessage("token", "Title", "Body"));
    }
}
