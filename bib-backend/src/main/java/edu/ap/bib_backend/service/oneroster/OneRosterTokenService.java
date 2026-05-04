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

import edu.ap.bib_backend.jpa.entity.OneRosterCredential;
import edu.ap.bib_backend.jpa.entity.OneRosterToken;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.repository.OneRosterCredentialRepository;
import edu.ap.bib_backend.jpa.repository.OneRosterTokenRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;

class OneRosterTokenService {
    private final RestTemplate restTemplate;
    private final String tokenUrl;
    private final TokenAuthMethod tokenAuthMethod;
    private final TokenEndpointMode tokenEndpointMode;
    private final String tokenScope;
    private final OneRosterCredentialRepository credentialRepository;
    private final OneRosterTokenRepository tokenRepository;

    OneRosterTokenService(
            RestTemplate restTemplate,
            String tokenUrl,
            String tokenAuthMethod,
            String tokenEndpointMode,
            String tokenScope,
            OneRosterCredentialRepository credentialRepository,
            OneRosterTokenRepository tokenRepository) {
        this.restTemplate = restTemplate;
        this.tokenUrl = tokenUrl;
        this.tokenAuthMethod = TokenAuthMethod.fromProperty(tokenAuthMethod);
        this.tokenEndpointMode = TokenEndpointMode.fromProperty(tokenEndpointMode);
        this.tokenScope = tokenScope == null ? "" : tokenScope.trim();
        this.credentialRepository = credentialRepository;
        this.tokenRepository = tokenRepository;
    }

    String fetchAccessToken(School school, String schoolHost) {
        OneRosterToken cached = tokenRepository.findBySchoolId(school.getId())
                .filter(this::isTokenValid)
                .orElse(null);
        if (cached != null) {
            return cached.getAccessToken();
        }

        OneRosterCredential credentials = credentialRepository.findBySchoolId(school.getId())
                .orElseThrow(() -> new ResponseStatusException(BAD_GATEWAY,
                        "OneRoster credentials ontbreken voor school: " + school.getDomainName()));

        List<String> endpointCandidates = resolveTokenEndpoints(schoolHost);
        List<String> errors = new ArrayList<>();

        for (String endpoint : endpointCandidates) {
            TokenAttemptResult basicAttempt = fetchAccessTokenWithBasicAuth(endpoint, credentials);
            if (basicAttempt.token() != null) {
                return storeToken(school, basicAttempt);
            }

            TokenAttemptResult postAttempt = TokenAttemptResult.notAttempted();
            if (tokenAuthMethod == TokenAuthMethod.POST || tokenAuthMethod == TokenAuthMethod.AUTO) {
                // client_secret_post is optioneel en uitgeschakeld by default om secret-leaks
                // in request-body logs te vermijden.
                postAttempt = fetchAccessTokenWithClientSecretPost(endpoint, credentials);
                if (postAttempt.token() != null) {
                    return storeToken(school, postAttempt);
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

    void invalidateToken(School school) {
        tokenRepository.findBySchoolId(school.getId())
                .ifPresent(tokenRepository::delete);
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

    private TokenAttemptResult fetchAccessTokenWithClientSecretPost(String endpointUrl,
            OneRosterCredential credentials) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        body.add("client_id", credentials.getClientId());
        body.add("client_secret", credentials.getClientSecret());
        if (StringUtils.hasText(tokenScope)) {
            body.add("scope", tokenScope);
        }

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        return executeTokenRequest(endpointUrl, request, "post");
    }

    private TokenAttemptResult fetchAccessTokenWithBasicAuth(String endpointUrl,
            OneRosterCredential credentials) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(credentials.getClientId(), credentials.getClientSecret());
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
            Object expiresIn = payload == null ? null : payload.get("expires_in");
            if (token instanceof String tokenValue && !tokenValue.isBlank()) {
                Instant expiresAt = parseExpiresAt(expiresIn);
                return TokenAttemptResult.success(tokenValue, expiresAt);
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

    private String storeToken(School school, TokenAttemptResult attempt) {
        OneRosterToken token = tokenRepository.findBySchoolId(school.getId())
                .orElseGet(OneRosterToken::new);
        token.setSchool(school);
        token.setAccessToken(attempt.token());
        token.setExpiresAt(requireExpiresAt(attempt.expiresAt()));
        tokenRepository.save(token);
        return attempt.token();
    }

    private Instant parseExpiresAt(Object expiresIn) {
        if (expiresIn instanceof Number number) {
            return Instant.now().plusSeconds(number.longValue());
        }
        if (expiresIn instanceof String text) {
            try {
                return Instant.now().plusSeconds(Long.parseLong(text.trim()));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private Instant requireExpiresAt(Instant expiresAt) {
        if (expiresAt == null) {
            return Instant.now().plusSeconds(3600);
        }
        return expiresAt;
    }

    private boolean isTokenValid(OneRosterToken token) {
        if (token.getAccessToken() == null || token.getAccessToken().isBlank()) {
            return false;
        }
        Instant expiresAt = token.getExpiresAt();
        if (expiresAt == null) {
            return false;
        }
        return expiresAt.isAfter(Instant.now().plusSeconds(60));
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

    private record TokenAttemptResult(String token, Instant expiresAt, String error) {
        private static TokenAttemptResult success(String token, Instant expiresAt) {
            return new TokenAttemptResult(token, expiresAt, null);
        }

        private static TokenAttemptResult failed(String error) {
            return new TokenAttemptResult(null, null, error);
        }

        private static TokenAttemptResult notAttempted() {
            return new TokenAttemptResult(null, null, "not_attempted");
        }
    }
}
