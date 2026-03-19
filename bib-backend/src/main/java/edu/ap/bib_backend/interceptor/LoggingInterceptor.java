package edu.ap.bib_backend.interceptor;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.*;
import org.springframework.util.StreamUtils;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

public class LoggingInterceptor implements ClientHttpRequestInterceptor {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LoggingInterceptor.class);

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
            ClientHttpRequestExecution execution) throws IOException {

        log.debug("=== TOKEN REQUEST ===");
        log.debug("URI: {}", request.getURI());
        log.debug("Headers: {}", request.getHeaders());
        log.debug("Body: {}", new String(body, StandardCharsets.UTF_8));

        ClientHttpResponse response = execution.execute(request, body);
        byte[] responseBody = StreamUtils.copyToByteArray(response.getBody());

        log.debug("=== TOKEN RESPONSE ===");
        log.debug("Status: {}", response.getStatusCode());
        log.debug("Content-Type: {}", response.getHeaders().getContentType());
        log.debug("Body: {}", new String(responseBody, StandardCharsets.UTF_8));

        // Return a re-readable response without spring-test
        return new ClientHttpResponse() {
            @Override
            public org.springframework.http.HttpHeaders getHeaders() {
                return response.getHeaders();
            }

            @Override
            public java.io.InputStream getBody() {
                return new ByteArrayInputStream(responseBody);
            }

            @Override
            public org.springframework.http.HttpStatusCode getStatusCode() throws IOException {
                return response.getStatusCode();
            }

            @Override
            public String getStatusText() throws IOException {
                return response.getStatusText();
            }

            @Override
            public void close() {
                response.close();
            }
        };
    }
}