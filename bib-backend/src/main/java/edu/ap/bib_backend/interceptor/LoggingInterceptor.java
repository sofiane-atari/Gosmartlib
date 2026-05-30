package edu.ap.bib_backend.interceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.*;
import org.springframework.util.StreamUtils;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

public class LoggingInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(LoggingInterceptor.class);

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
            ClientHttpRequestExecution execution) throws IOException {

        log.info("=== OAuth2 HTTP {} {} ===", request.getMethod(), request.getURI());

        ClientHttpResponse response = execution.execute(request, body);
        byte[] responseBody = StreamUtils.copyToByteArray(response.getBody());

        String contentType = response.getHeaders().getContentType() != null
                ? response.getHeaders().getContentType().toString()
                : "unknown";
        log.info("Response status: {}, content-type: {}", response.getStatusCode(), contentType);

        String bodyStr = new String(responseBody, StandardCharsets.UTF_8);
        if (responseBody.length > 0) {
            log.info("Response body ({} bytes):\n{}", responseBody.length,
                    bodyStr.substring(0, Math.min(bodyStr.length(), 2000)));
        }

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