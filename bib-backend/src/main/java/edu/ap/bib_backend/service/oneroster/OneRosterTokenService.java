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
    private final String tokenPath;
    private final TokenAuthMethod tokenAuthMethod;
    private final String tokenScope;
    private final OneRosterCredentialRepository credentialRepository;
    private final OneRosterTokenRepository tokenRepository;

    OneRosterTokenService(
            RestTemplate restTemplate,
            String tokenPath,
            String tokenAuthMethod,
            String tokenScope,
            OneRosterCredentialRepository credentialRepository,
            OneRosterTokenRepository tokenRepository) {
        this.restTemplate = restTemplate;
        this.tokenPath = tokenPath;
        this.tokenAuthMethod = TokenAuthMethod.fromProperty(tokenAuthMethod);
        this.tokenScope = tokenScope == null ? "" : tokenScope.trim();
        this.credentialRepository = credentialRepository;
        this.tokenRepository = tokenRepository;
    }

    /**
     * @param school     school entity (used for credential lookup and caching)
     * @param schoolHost validated school hostname (used to resolve token endpoints)
     * @return a valid OAuth2 access token
     */
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

        TokenAttemptResult result = attemptTokenFetch(credentials, schoolHost);
        if (result.token() == null) {
            throw new ResponseStatusException(BAD_GATEWAY,
                    "OneRoster token ophalen mislukt. Upstream details: " + result.error());
        }
        return storeToken(school, result);
    }

    /**
     * Validates OneRoster credentials by attempting to fetch an access token.
     *
     * @param clientId     the OAuth2 client ID
     * @param clientSecret the OAuth2 client secret
     * @param schoolHost   validated school hostname
     * @return the access token if credentials are valid
     * @throws ResponseStatusException if credentials are invalid
     */
    String validateCredentials(String clientId, String clientSecret, String schoolHost) {
        OneRosterCredential tempCredential = new OneRosterCredential();
        tempCredential.setClientId(clientId);
        tempCredential.setClientSecret(clientSecret);

        TokenAttemptResult result = attemptTokenFetch(tempCredential, schoolHost);
        if (result.token() == null) {
            throw new ResponseStatusException(BAD_GATEWAY,
                    "OneRoster credentials ongeldig. Details: " + result.error());
        }
        return result.token();
    }

    void invalidateToken(School school) {
        tokenRepository.findBySchoolId(school.getId())
                .ifPresent(tokenRepository::delete);
    }

    private TokenAttemptResult attemptTokenFetch(OneRosterCredential credentials, String schoolHost) {
        List<String> endpointCandidates = resolveTokenEndpoints(schoolHost);
        List<String> errors = new ArrayList<>();

        for (String endpoint : endpointCandidates) {
            TokenAttemptResult basicAttempt = fetchAccessTokenWithBasicAuth(endpoint, credentials);
            if (basicAttempt.token() != null) {
                return basicAttempt;
            }

            TokenAttemptResult postAttempt = TokenAttemptResult.notAttempted();
            if (tokenAuthMethod == TokenAuthMethod.POST || tokenAuthMethod == TokenAuthMethod.AUTO) {
                postAttempt = fetchAccessTokenWithClientSecretPost(endpoint, credentials);
                if (postAttempt.token() != null) {
                    return postAttempt;
                }
            }

            String reason = switch (tokenAuthMethod) {
                case BASIC -> "basic=" + basicAttempt.error();
                case POST -> "post=" + postAttempt.error();
                case AUTO -> "basic=" + basicAttempt.error() + "; post=" + postAttempt.error();
            };
            errors.add(endpoint + " -> " + reason);
        }

        return TokenAttemptResult.failed(String.join(" | ", errors));
    }

    private List<String> resolveTokenEndpoints(String schoolHost) {
        return List.of("https://" + schoolHost + tokenPath);
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