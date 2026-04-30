package edu.ap.bib_backend.service.oneroster;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;

class OneRosterTokenService {
    private final RestTemplate restTemplate;
    private final String clientId;
    private final String clientSecret;
    private final String tokenUrl;
    private final TokenAuthMethod tokenAuthMethod;
    private final TokenEndpointMode tokenEndpointMode;
    private final String tokenScope;

    OneRosterTokenService(
            RestTemplate restTemplate,
            String clientId,
            String clientSecret,
            String tokenUrl,
            String tokenAuthMethod,
            String tokenEndpointMode,
            String tokenScope) {
        this.restTemplate = restTemplate;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.tokenUrl = tokenUrl;
        this.tokenAuthMethod = TokenAuthMethod.fromProperty(tokenAuthMethod);
        this.tokenEndpointMode = TokenEndpointMode.fromProperty(tokenEndpointMode);
        this.tokenScope = tokenScope == null ? "" : tokenScope.trim();
    }

    String fetchAccessToken(String schoolHost) {
        List<String> endpointCandidates = resolveTokenEndpoints(schoolHost);
        List<String> errors = new ArrayList<>();

        for (String endpoint : endpointCandidates) {
            TokenAttemptResult basicAttempt = fetchAccessTokenWithBasicAuth(endpoint);
            if (basicAttempt.token() != null) {
                return basicAttempt.token();
            }

            TokenAttemptResult postAttempt = TokenAttemptResult.notAttempted();
            if (tokenAuthMethod == TokenAuthMethod.POST || tokenAuthMethod == TokenAuthMethod.AUTO) {
                // client_secret_post is optioneel en uitgeschakeld by default om secret-leaks
                // in request-body logs te vermijden.
                postAttempt = fetchAccessTokenWithClientSecretPost(endpoint);
                if (postAttempt.token() != null) {
                    return postAttempt.token();
                }
            }

            String reason = switch (tokenAuthMethod) {
                case BASIC -> "basic=" + basicAttempt.error();
                case POST -> "post=" + postAttempt.error();
                case AUTO -> "basic=" + basicAttempt.error() + "; post=" + postAttempt.error();
            };
            errors.add(endpoint + " -> " + reason);
        }

        throw new ResponseStatusException(BAD_GATEWAY,
                "OneRoster token ophalen mislukt. Upstream details: " + String.join(" | ", errors));
    }

    private List<String> resolveTokenEndpoints(String schoolHost) {
        List<String> candidates = new ArrayList<>();
        String globalEndpoint = tokenUrl.trim();
        String schoolEndpoint = "https://" + schoolHost + "/ims/oneroster/token";

        switch (tokenEndpointMode) {
            case GLOBAL -> {
                if (!globalEndpoint.isEmpty()) {
                    candidates.add(globalEndpoint);
                }
            }
            case SCHOOL -> candidates.add(schoolEndpoint);
            case AUTO -> {
                if (!globalEndpoint.isEmpty()) {
                    candidates.add(globalEndpoint);
                }
                if (!globalEndpoint.equalsIgnoreCase(schoolEndpoint)) {
                    candidates.add(schoolEndpoint);
                }
            }
        }

        return candidates;
    }

    private TokenAttemptResult fetchAccessTokenWithClientSecretPost(String endpointUrl) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        if (StringUtils.hasText(tokenScope)) {
            body.add("scope", tokenScope);
        }

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        return executeTokenRequest(endpointUrl, request, "post");
    }

    private TokenAttemptResult fetchAccessTokenWithBasicAuth(String endpointUrl) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(clientId, clientSecret);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        if (StringUtils.hasText(tokenScope)) {
            body.add("scope", tokenScope);
        }

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        return executeTokenRequest(endpointUrl, request, "basic");
    }

    private TokenAttemptResult executeTokenRequest(
            String endpointUrl,
            HttpEntity<MultiValueMap<String, String>> request,
            String methodName) {
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    endpointUrl,
                    HttpMethod.POST,
                    request,
                    new ParameterizedTypeReference<>() {
                    });

            Map<String, Object> payload = response.getBody();
            Object token = payload == null ? null : payload.get("access_token");
            if (token instanceof String tokenValue && !tokenValue.isBlank()) {
                return TokenAttemptResult.success(tokenValue);
            }

            return TokenAttemptResult.failed(methodName + " response bevat geen access_token");
        } catch (HttpStatusCodeException exception) {
            String responseBody = sanitizeBody(exception.getResponseBodyAsString());
            String error = methodName + " status=" + exception.getStatusCode().value();
            if (responseBody != null) {
                error += " body=" + responseBody;
            }
            return TokenAttemptResult.failed(error);
        } catch (RestClientException exception) {
            return TokenAttemptResult.failed(methodName + " transport=" + exception.getClass().getSimpleName());
        }
    }

    private String sanitizeBody(String rawBody) {
        if (!StringUtils.hasText(rawBody)) {
            return null;
        }

        String compact = rawBody.replaceAll("\\s+", " ").trim();
        if (compact.length() > 240) {
            return compact.substring(0, 240) + "...";
        }
        return compact;
    }

    private enum TokenAuthMethod {
        BASIC,
        POST,
        AUTO;

        private static TokenAuthMethod fromProperty(String rawValue) {
            if (rawValue == null || rawValue.isBlank()) {
                return BASIC;
            }

            return switch (rawValue.trim().toLowerCase(Locale.ROOT)) {
                case "basic" -> BASIC;
                case "post", "client_secret_post" -> POST;
                case "auto" -> AUTO;
                default -> BASIC;
            };
        }
    }

    private enum TokenEndpointMode {
        GLOBAL,
        SCHOOL,
        AUTO;

        private static TokenEndpointMode fromProperty(String rawValue) {
            if (rawValue == null || rawValue.isBlank()) {
                return AUTO;
            }

            return switch (rawValue.trim().toLowerCase(Locale.ROOT)) {
                case "global" -> GLOBAL;
                case "school" -> SCHOOL;
                case "auto" -> AUTO;
                default -> AUTO;
            };
        }
    }

    private record TokenAttemptResult(String token, String error) {
        private static TokenAttemptResult success(String token) {
            return new TokenAttemptResult(token, null);
        }

        private static TokenAttemptResult failed(String error) {
            return new TokenAttemptResult(null, error);
        }

        private static TokenAttemptResult notAttempted() {
            return new TokenAttemptResult(null, "not_attempted");
        }
    }
}
