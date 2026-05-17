package edu.ap.bib_backend.interceptor;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.*;
import org.springframework.util.StreamUtils;
import java.io.IOException;
import java.io.ByteArrayInputStream;

public class LoggingInterceptor implements ClientHttpRequestInterceptor {
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
            ClientHttpRequestExecution execution) throws IOException {

        ClientHttpResponse response = execution.execute(request, body);
        byte[] responseBody = StreamUtils.copyToByteArray(response.getBody());

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