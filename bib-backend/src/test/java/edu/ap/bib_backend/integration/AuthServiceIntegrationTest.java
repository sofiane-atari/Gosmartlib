package edu.ap.bib_backend.integration;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import edu.ap.bib_backend.dto.JwtTokenDTO;
import edu.ap.bib_backend.jpa.repository.SchoolRepository;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import edu.ap.bib_backend.service.ActiveSchoolContext;
import edu.ap.bib_backend.service.AuthService;
import edu.ap.bib_backend.service.JwtService;
import edu.ap.bib_backend.service.TokenStorageService;
import edu.ap.bib_backend.service.smartschool.SmartschoolProfileService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceIntegrationTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private TokenStorageService tokenStorageService;

    @Mock
    private OAuth2AuthorizedClientService authorizedClientService;

    @Mock
    private SmartschoolProfileService smartschoolProfileService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SchoolRepository schoolRepository;

    @Mock
    private ActiveSchoolContext activeSchoolContext;

    private AuthService authService;

    private HttpServer fakeSmartschoolServer;
    private String smartschoolTokenUri;

    @BeforeEach
    void setUp() throws IOException {
        fakeSmartschoolServer = HttpServer.create(new InetSocketAddress(0), 0);
        fakeSmartschoolServer.start();

        int port = fakeSmartschoolServer.getAddress().getPort();
        smartschoolTokenUri = "http://localhost:" + port + "/oauth/token";

        authService = new AuthService(
                jwtService,
                tokenStorageService,
                authorizedClientService,
                smartschoolProfileService,
                userRepository,
                schoolRepository,
                activeSchoolContext);

        ReflectionTestUtils.setField(authService, "smartschoolClientId", "test-client-id");
        ReflectionTestUtils.setField(authService, "smartschoolClientSecret", "test-client-secret");
        ReflectionTestUtils.setField(authService, "smartschoolTokenUri", smartschoolTokenUri);
    }

    @AfterEach
    void tearDown() {
        if (fakeSmartschoolServer != null) {
            fakeSmartschoolServer.stop(0);
        }
    }

    @Test
    void givenValidJwtAndSmartschoolRefreshToken_whenRefreshJwtToken_thenRefreshSmartschoolTokenAndReturnNewJwt() {
        String oldJwt = "old.jwt.token";
        String userId = "user-1";
        String role = "ROLE_STUDENT";
        String oldSmartschoolRefreshToken = "old-smartschool-refresh-token";

        String newSmartschoolAccessToken = "new-smartschool-access-token";
        String newSmartschoolRefreshToken = "new-smartschool-refresh-token";

        String newJwt = "new.jwt.token";
        Instant newJwtExpiresAt = Instant.parse("2026-04-30T12:00:00Z");

        AtomicReference<String> receivedRequestBody = new AtomicReference<>();

        fakeSmartschoolServer.createContext("/oauth/token", exchange -> {
            receivedRequestBody.set(readBody(exchange));

            String responseJson = """
                    {
                      "access_token": "new-smartschool-access-token",
                      "refresh_token": "new-smartschool-refresh-token",
                      "token_type": "Bearer",
                      "expires_in": 3600
                    }
                    """;

            sendJson(exchange, 200, responseJson);
        });

        when(jwtService.isValid(oldJwt)).thenReturn(true);
        when(jwtService.extractUsername(oldJwt)).thenReturn(userId);
        when(jwtService.extractClaim(oldJwt, "roles")).thenReturn(role);

        when(tokenStorageService.isCurrentToken(userId, oldJwt)).thenReturn(true);
        when(tokenStorageService.getSmartschoolRefreshToken(userId)).thenReturn(oldSmartschoolRefreshToken);

        when(jwtService.generateToken(userId, role)).thenReturn(newJwt);
        when(jwtService.extractExpirationInstant(newJwt)).thenReturn(newJwtExpiresAt);

        JwtTokenDTO result = authService.refreshJwtToken(oldJwt);

        assertThat(result.token()).isEqualTo(newJwt);
        assertThat(result.expiresAt()).isEqualTo(newJwtExpiresAt);

        assertThat(receivedRequestBody.get()).contains("grant_type=refresh_token");
        assertThat(receivedRequestBody.get()).contains("refresh_token=old-smartschool-refresh-token");
        assertThat(receivedRequestBody.get()).contains("client_id=test-client-id");
        assertThat(receivedRequestBody.get()).contains("client_secret=test-client-secret");

        verify(tokenStorageService).replaceToken(
                userId,
                newJwt,
                role,
                newJwtExpiresAt);

        verify(tokenStorageService).storeSmartschoolTokens(
                userId,
                newSmartschoolAccessToken,
                newSmartschoolRefreshToken);
    }

    @Test
    void givenValidJwtButNoSmartschoolRefreshToken_whenRefreshJwtToken_thenThrowUnauthorized() {
        String oldJwt = "old.jwt.token";
        String userId = "user-1";
        String role = "ROLE_STUDENT";

        when(jwtService.isValid(oldJwt)).thenReturn(true);
        when(jwtService.extractUsername(oldJwt)).thenReturn(userId);
        when(jwtService.extractClaim(oldJwt, "roles")).thenReturn(role);
        when(tokenStorageService.isCurrentToken(userId, oldJwt)).thenReturn(true);
        when(tokenStorageService.getSmartschoolRefreshToken(userId)).thenReturn(null);

        assertThatThrownBy(() -> authService.refreshJwtToken(oldJwt))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No Smartschool refresh token available");
    }

    @Test
    void givenSmartschoolDoesNotReturnRefreshToken_whenRefreshJwtToken_thenKeepOldRefreshToken() {
        String oldJwt = "old.jwt.token";
        String userId = "user-1";
        String role = "ROLE_STUDENT";
        String oldSmartschoolRefreshToken = "old-smartschool-refresh-token";

        String newSmartschoolAccessToken = "new-smartschool-access-token";
        String newJwt = "new.jwt.token";
        Instant newJwtExpiresAt = Instant.parse("2026-04-30T12:00:00Z");

        fakeSmartschoolServer.createContext("/oauth/token", exchange -> {
            String responseJson = """
                    {
                      "access_token": "new-smartschool-access-token",
                      "token_type": "Bearer",
                      "expires_in": 3600
                    }
                    """;

            sendJson(exchange, 200, responseJson);
        });

        when(jwtService.isValid(oldJwt)).thenReturn(true);
        when(jwtService.extractUsername(oldJwt)).thenReturn(userId);
        when(jwtService.extractClaim(oldJwt, "roles")).thenReturn(role);

        when(tokenStorageService.isCurrentToken(userId, oldJwt)).thenReturn(true);
        when(tokenStorageService.getSmartschoolRefreshToken(userId)).thenReturn(oldSmartschoolRefreshToken);

        when(jwtService.generateToken(userId, role)).thenReturn(newJwt);
        when(jwtService.extractExpirationInstant(newJwt)).thenReturn(newJwtExpiresAt);

        JwtTokenDTO result = authService.refreshJwtToken(oldJwt);

        assertThat(result.token()).isEqualTo(newJwt);
        assertThat(result.expiresAt()).isEqualTo(newJwtExpiresAt);

        verify(tokenStorageService).storeSmartschoolTokens(
                userId,
                newSmartschoolAccessToken,
                oldSmartschoolRefreshToken);
    }

    @Test
    void givenSmartschoolRefreshFails_whenRefreshJwtToken_thenThrowUnauthorized() {
        String oldJwt = "old.jwt.token";
        String userId = "user-1";
        String role = "ROLE_STUDENT";
        String smartschoolRefreshToken = "smartschool-refresh-token";

        fakeSmartschoolServer.createContext("/oauth/token", exchange -> {
            String responseJson = """
                    {
                      "error": "invalid_grant"
                    }
                    """;

            sendJson(exchange, 401, responseJson);
        });

        when(jwtService.isValid(oldJwt)).thenReturn(true);
        when(jwtService.extractUsername(oldJwt)).thenReturn(userId);
        when(jwtService.extractClaim(oldJwt, "roles")).thenReturn(role);

        when(tokenStorageService.isCurrentToken(userId, oldJwt)).thenReturn(true);
        when(tokenStorageService.getSmartschoolRefreshToken(userId)).thenReturn(smartschoolRefreshToken);

        assertThatThrownBy(() -> authService.refreshJwtToken(oldJwt))
                .isInstanceOf(ResponseStatusException.class);
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    private static void sendJson(HttpExchange exchange, int statusCode, String responseJson) throws IOException {
        byte[] responseBytes = responseJson.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);

        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(responseBytes);
        }
    }
}