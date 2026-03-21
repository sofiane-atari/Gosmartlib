package edu.ap.bib_backend.interceptor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoggingInterceptorTest {

    @Mock
    private HttpRequest request;

    @Mock
    private ClientHttpRequestExecution execution;

    @Mock
    private ClientHttpResponse originalResponse;

    @Test
    void givenExecutionResponse_WhenIntercept_ThenReturnResponseWithReadableBody() throws IOException {
        // Arrange
        LoggingInterceptor loggingInterceptor = new LoggingInterceptor();
        byte[] requestBody = "grant_type=authorization_code".getBytes(StandardCharsets.UTF_8);
        byte[] responseBody = "{\"access_token\":\"abc\"}".getBytes(StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        when(request.getURI()).thenReturn(URI.create("https://smartschool.example.com/token"));
        when(request.getHeaders()).thenReturn(new HttpHeaders());
        when(execution.execute(request, requestBody)).thenReturn(originalResponse);
        when(originalResponse.getBody()).thenReturn(new ByteArrayInputStream(responseBody));
        when(originalResponse.getHeaders()).thenReturn(headers);
        when(originalResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        when(originalResponse.getStatusText()).thenReturn("OK");

        // Act
        ClientHttpResponse interceptedResponse = loggingInterceptor.intercept(request, requestBody, execution);
        byte[] firstRead = interceptedResponse.getBody().readAllBytes();
        byte[] secondRead = interceptedResponse.getBody().readAllBytes();

        // Assert
        assertNotNull(interceptedResponse);
        assertEquals(HttpStatus.OK, interceptedResponse.getStatusCode());
        assertEquals("OK", interceptedResponse.getStatusText());
        assertEquals("{\"access_token\":\"abc\"}", new String(firstRead, StandardCharsets.UTF_8));
        assertEquals("{\"access_token\":\"abc\"}", new String(secondRead, StandardCharsets.UTF_8));
        verify(execution, times(1)).execute(request, requestBody);
    }

    @Test
    void givenInterceptedResponse_WhenCloseCalled_ThenDelegateCloseToOriginalResponse() throws IOException {
        // Arrange
        LoggingInterceptor loggingInterceptor = new LoggingInterceptor();
        byte[] requestBody = "request-body".getBytes(StandardCharsets.UTF_8);

        when(request.getURI()).thenReturn(URI.create("https://smartschool.example.com/token"));
        when(request.getHeaders()).thenReturn(new HttpHeaders());
        when(execution.execute(request, requestBody)).thenReturn(originalResponse);
        when(originalResponse.getBody()).thenReturn(new ByteArrayInputStream("response".getBytes(StandardCharsets.UTF_8)));
        when(originalResponse.getHeaders()).thenReturn(new HttpHeaders());
        when(originalResponse.getStatusCode()).thenReturn(HttpStatus.OK);

        ClientHttpResponse interceptedResponse = loggingInterceptor.intercept(request, requestBody, execution);

        // Act
        interceptedResponse.close();

        // Assert
        verify(originalResponse, times(1)).close();
    }
}