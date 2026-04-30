package edu.ap.bib_backend.service.oneroster;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import org.springframework.http.client.SimpleClientHttpRequestFactory;

@Service
public class OneRosterClient {
    private static final String DEFAULT_ONE_ROSTER_PATH = "/ims/oneroster/v1p1";

    public record AudiencePayload(List<String> classes, List<String> years, List<String> schools) {
    }

    private final OneRosterHostValidator hostValidator;
    private final OneRosterTokenService tokenService;
    private final OneRosterDataService dataService;

    public OneRosterClient(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${oneroster.client-id}") String clientId,
            @Value("${oneroster.client-secret}") String clientSecret,
            @Value("${oneroster.token-url:https://oauth.smartschool.be/OAuth/index/token}") String tokenUrl,
            @Value("${oneroster.api-path:" + DEFAULT_ONE_ROSTER_PATH + "}") String apiPath,
            @Value("${oneroster.allowed-host-suffixes:.smartschool.be}") String allowedHostSuffixes,
            @Value("${oneroster.connect-timeout-ms:5000}") int connectTimeoutMs,
            @Value("${oneroster.read-timeout-ms:7000}") int readTimeoutMs,
            @Value("${oneroster.token-auth-method:basic}") String tokenAuthMethod,
            @Value("${oneroster.token-endpoint-mode:auto}") String tokenEndpointMode,
            @Value("${oneroster.scope:}") String tokenScope) {
        RestTemplate template = restTemplateBuilder.build();
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMs);
        requestFactory.setReadTimeout(readTimeoutMs);
        template.setRequestFactory(requestFactory);

        this.hostValidator = new OneRosterHostValidator(allowedHostSuffixes);
        this.tokenService = new OneRosterTokenService(
                template,
                clientId,
                clientSecret,
                tokenUrl,
                tokenAuthMethod,
                tokenEndpointMode,
                tokenScope);
        this.dataService = new OneRosterDataService(
                template,
                normalizeApiPath(apiPath),
                new OneRosterLabelFilter());
    }

    public AudiencePayload fetchAudiencePayload(String schoolCode) {
        String host = hostValidator.normalizeAndValidateSchoolHost(schoolCode);
        String accessToken = tokenService.fetchAccessToken(host);
        return dataService.fetchAudiencePayloadWithResolvedHost(host, accessToken);
    }

    public List<Map<String, Object>> fetchUsers(String schoolCode) {
        String host = hostValidator.normalizeAndValidateSchoolHost(schoolCode);
        String accessToken = tokenService.fetchAccessToken(host);
        return dataService.fetchUsersWithResolvedHost(host, accessToken);
    }

    public AudiencePayload fetchAudiencePayloadWithBearer(String schoolCode, String bearerToken) {
        String host = hostValidator.normalizeAndValidateSchoolHost(schoolCode);
        if (!StringUtils.hasText(bearerToken)) {
            return fetchAudiencePayload(schoolCode);
        }

        try {
            return dataService.fetchAudiencePayloadWithResolvedHost(host, bearerToken.trim());
        } catch (ResponseStatusException ex) {
            // Fallback naar client-credentials flow wanneer delegated token niet werkt.
            return fetchAudiencePayload(schoolCode);
        }
    }

    public List<Map<String, Object>> fetchUsersWithBearer(String schoolCode, String bearerToken) {
        String host = hostValidator.normalizeAndValidateSchoolHost(schoolCode);
        if (!StringUtils.hasText(bearerToken)) {
            return fetchUsers(schoolCode);
        }

        try {
            return dataService.fetchUsersWithResolvedHost(host, bearerToken.trim());
        } catch (ResponseStatusException ex) {
            // Fallback naar client-credentials flow wanneer delegated token niet werkt.
            return fetchUsers(schoolCode);
        }
    }

    private String normalizeApiPath(String rawPath) {
        String path = (rawPath == null || rawPath.isBlank()) ? DEFAULT_ONE_ROSTER_PATH : rawPath.trim();
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }
}
