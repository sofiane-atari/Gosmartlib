package edu.ap.bib_backend.config;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.AuthenticationException;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RestAuthenticationEntryPointTest {

    @Test
    void Given_UnauthorizedRequest_When_Commence_Then_ReturnsUnauthorizedJson()
            throws ServletException, IOException {
        // Arrange
        RestAuthenticationEntryPoint entryPoint = new RestAuthenticationEntryPoint();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthenticationException exception = new AuthenticationException("unauthorized") {};

        // Act
        entryPoint.commence(request, response, exception);

        // Assert
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("TOKEN_INVALID"));
    }
}
