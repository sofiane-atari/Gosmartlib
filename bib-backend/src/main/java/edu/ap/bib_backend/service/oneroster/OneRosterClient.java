package edu.ap.bib_backend.service.oneroster;

import edu.ap.bib_backend.dto.oneroster.OneRosterClassDTO;
import edu.ap.bib_backend.dto.oneroster.OneRosterEnrollmentDTO;
import edu.ap.bib_backend.dto.oneroster.OneRosterUserDTO;
import edu.ap.bib_backend.mapper.OneRosterMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
public class OneRosterClient {

    private static final String DEFAULT_ONE_ROSTER_PATH = "/ims/oneroster/v1p1";

    private final RestTemplate restTemplate;
    private final OneRosterMapper oneRosterMapper;
    private final String clientId;
    private final String clientSecret;
    private final String tokenUrl;
    private final String apiPath;
    private final List<String> allowedHostSuffixes;
    private final TokenAuthMethod tokenAuthMethod;
    private final TokenEndpointMode tokenEndpointMode;
    private final String tokenScope;

    public OneRosterClient(
            RestTemplateBuilder restTemplateBuilder,
            OneRosterMapper oneRosterMapper,
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

        this.restTemplate = template;
        this.oneRosterMapper = oneRosterMapper;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.tokenUrl = tokenUrl;
        this.apiPath = normalizeApiPath(apiPath);
        this.allowedHostSuffixes = parseSuffixes(allowedHostSuffixes);
        this.tokenAuthMethod = TokenAuthMethod.fromProperty(tokenAuthMethod);
        this.tokenEndpointMode = TokenEndpointMode.fromProperty(tokenEndpointMode);
        this.tokenScope = tokenScope == null ? "" : tokenScope.trim();
    }

    public AudiencePayload fetchAudiencePayload(String schoolCode) {
        String host = normalizeAndValidateSchoolHost(schoolCode);
        String accessToken = fetchAccessToken(host);

        return fetchAudiencePayloadWithResolvedHost(host, accessToken);
    }

    public AudiencePayload fetchAudiencePayloadWithBearer(String schoolCode, String bearerToken) {
        String host = normalizeAndValidateSchoolHost(schoolCode);

        if (!StringUtils.hasText(bearerToken)) {
            return fetchAudiencePayload(schoolCode);
        }

        try {
            return fetchAudiencePayloadWithResolvedHost(host, bearerToken.trim());
        } catch (ResponseStatusException ex) {
            return fetchAudiencePayload(schoolCode);
        }
    }

    public Map<String, String> fetchUserDisplayNames(String schoolCode, Set<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }

        String host = normalizeAndValidateSchoolHost(schoolCode);
        String accessToken = fetchAccessToken(host);

        URI usersUri = buildApiUri(host, "/users");
        Map<String, Object> payload = getJsonWithBearer(usersUri, accessToken);

        List<Map<String, Object>> users = extractRecordCollection(payload, "users");

        return users.stream()
                .filter(user -> {
                    String sourcedId = firstNonBlank(user, "sourcedId", "identifier", "userId", "username");
                    return sourcedId != null && userIds.contains(sourcedId);
                })
                .collect(java.util.stream.Collectors.toMap(
                        user -> firstNonBlank(user, "sourcedId", "identifier", "userId", "username"),
                        oneRosterMapper::buildDisplayName,
                        (left, right) -> left));
    }

    public String findSourcedIdByLegacyIdentifier(String schoolCode, String legacyIdentifier) {
        if (legacyIdentifier == null || legacyIdentifier.isBlank()) {
            return null;
        }

        String host = normalizeAndValidateSchoolHost(schoolCode);
        String accessToken = fetchAccessToken(host);

        URI usersUri = buildApiUri(host, "/users");
        Map<String, Object> payload = getJsonWithBearer(usersUri, accessToken);

        List<Map<String, Object>> users = extractRecordCollection(payload, "users");

        return users.stream()
                .filter(user -> legacyIdentifier.equals(readMetadataString(user, "smsc.legacyIdentifier")))
                .map(user -> firstNonBlank(user, "sourcedId"))
                .filter(id -> id != null && !id.isBlank())
                .findFirst()
                .orElse(null);
    }

    public List<OneRosterClassDTO> fetchClassesWithBearer(String schoolCode, String bearerToken) {
        String host = normalizeAndValidateSchoolHost(schoolCode);

        if (StringUtils.hasText(bearerToken)) {
            try {
                URI classUri = buildApiUri(host, "/classes");
                Map<String, Object> payload = getJsonWithBearer(classUri, bearerToken.trim());

                return extractRecordCollection(payload, "classes").stream()
                        .map(oneRosterMapper::toOneRosterClass)
                        .filter(item -> item.sourcedId() != null && !item.sourcedId().isBlank())
                        .sorted(Comparator.comparing(OneRosterClassDTO::title, String.CASE_INSENSITIVE_ORDER))
                        .toList();

            } catch (ResponseStatusException ex) {
                return fetchClasses(schoolCode);
            }
        }

        return fetchClasses(schoolCode);
    }

    public List<OneRosterUserDTO> fetchStudentsForClassWithBearer(
            String schoolCode,
            String bearerToken,
            String classSourcedId) {
        String host = normalizeAndValidateSchoolHost(schoolCode);

        if (StringUtils.hasText(bearerToken)) {
            try {
                return fetchStudentsForClassWithResolvedHost(host, bearerToken.trim(), classSourcedId);
            } catch (ResponseStatusException ex) {
                return fetchStudentsForClass(schoolCode, classSourcedId);
            }
        }

        return fetchStudentsForClass(schoolCode, classSourcedId);
    }

    public List<OneRosterEnrollmentDTO> fetchEnrollmentsWithBearer(String schoolCode, String bearerToken) {
        String host = normalizeAndValidateSchoolHost(schoolCode);

        if (StringUtils.hasText(bearerToken)) {
            try {
                URI enrollmentsUri = buildApiUri(host, "/enrollments");
                Map<String, Object> payload = getJsonWithBearer(enrollmentsUri, bearerToken.trim());

                return extractRecordCollection(payload, "enrollments").stream()
                        .map(oneRosterMapper::toOneRosterEnrollment)
                        .filter(enrollment -> enrollment.userSourcedId() != null
                                && !enrollment.userSourcedId().isBlank())
                        .filter(enrollment -> enrollment.classSourcedId() != null
                                && !enrollment.classSourcedId().isBlank())
                        .toList();

            } catch (ResponseStatusException ex) {
                return fetchEnrollments(schoolCode);
            }
        }

        return fetchEnrollments(schoolCode);
    }

    public List<OneRosterUserDTO> fetchUsersWithBearer(String schoolCode, String bearerToken) {
        String host = normalizeAndValidateSchoolHost(schoolCode);

        if (StringUtils.hasText(bearerToken)) {
            try {
                URI usersUri = buildApiUri(host, "/users");
                Map<String, Object> payload = getJsonWithBearer(usersUri, bearerToken.trim());

                return extractRecordCollection(payload, "users").stream()
                        .map(oneRosterMapper::toOneRosterUser)
                        .filter(user -> user.sourcedId() != null && !user.sourcedId().isBlank())
                        .sorted(Comparator.comparing(OneRosterUserDTO::displayName, String.CASE_INSENSITIVE_ORDER))
                        .toList();

            } catch (ResponseStatusException ex) {
                return fetchUsers(schoolCode);
            }
        }

        return fetchUsers(schoolCode);
    }

    private AudiencePayload fetchAudiencePayloadWithResolvedHost(String host, String accessToken) {
        URI classUri = buildApiUri(host, "/classes");
        URI yearUri = buildApiUri(host, "/academicSessions");
        URI schoolUri = buildApiUri(host, "/schools");
        URI orgUri = buildApiUri(host, "/orgs");

        Map<String, Object> classPayload = getJsonWithBearer(classUri, accessToken);
        Map<String, Object> yearPayload = getJsonWithBearer(yearUri, accessToken);

        Map<String, Object> schoolPayload;

        try {
            schoolPayload = getJsonWithBearer(schoolUri, accessToken);
        } catch (ResponseStatusException ex) {
            schoolPayload = getJsonWithBearer(orgUri, accessToken);
        }

        List<String> classes = filterUmbrellaClassLabels(extractDistinctSortedLabels(
                classPayload,
                "classes",
                "classCode",
                "identifier",
                "code",
                "sourcedId",
                "title",
                "name"));

        List<String> years = extractDistinctSortedLabels(
                yearPayload,
                "academicSessions",
                "title",
                "schoolYear",
                "identifier",
                "sourcedId");

        List<String> schools = extractDistinctSortedLabels(
                schoolPayload,
                "schools",
                "name",
                "title",
                "identifier",
                "sourcedId");

        if (schools.isEmpty()) {
            schools = extractDistinctSortedLabels(
                    schoolPayload,
                    "orgs",
                    "name",
                    "title",
                    "identifier",
                    "sourcedId");
        }

        return new AudiencePayload(classes, years, schools);
    }

    private List<OneRosterClassDTO> fetchClasses(String schoolCode) {
        String host = normalizeAndValidateSchoolHost(schoolCode);
        String accessToken = fetchAccessToken(host);

        URI classUri = buildApiUri(host, "/classes");
        Map<String, Object> payload = getJsonWithBearer(classUri, accessToken);

        return extractRecordCollection(payload, "classes").stream()
                .map(oneRosterMapper::toOneRosterClass)
                .filter(item -> item.sourcedId() != null && !item.sourcedId().isBlank())
                .sorted(Comparator.comparing(OneRosterClassDTO::title, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private List<OneRosterUserDTO> fetchStudentsForClass(String schoolCode, String classSourcedId) {
        String host = normalizeAndValidateSchoolHost(schoolCode);
        String accessToken = fetchAccessToken(host);

        return fetchStudentsForClassWithResolvedHost(host, accessToken, classSourcedId);
    }

    private List<OneRosterUserDTO> fetchStudentsForClassWithResolvedHost(
            String host,
            String accessToken,
            String classSourcedId) {
        if (!StringUtils.hasText(classSourcedId)) {
            throw new ResponseStatusException(BAD_REQUEST, "Geen geldige klas geselecteerd");
        }

        URI studentsUri = buildApiUri(host, "/classes/" + classSourcedId + "/students");

        try {
            Map<String, Object> payload = getJsonWithBearer(studentsUri, accessToken);

            List<Map<String, Object>> students = extractRecordCollection(payload, "users");

            if (students.isEmpty()) {
                students = extractRecordCollection(payload, "students");
            }

            return students.stream()
                    .map(oneRosterMapper::toOneRosterUser)
                    .filter(user -> user.sourcedId() != null && !user.sourcedId().isBlank())
                    .sorted(Comparator.comparing(OneRosterUserDTO::displayName, String.CASE_INSENSITIVE_ORDER))
                    .toList();

        } catch (ResponseStatusException ex) {
            System.out.println("Kon leerlingen niet ophalen via " + studentsUri);
            System.out.println("Fallback nodig voor classSourcedId = " + classSourcedId);

            return List.of();
        }
    }

    private List<OneRosterEnrollmentDTO> fetchEnrollments(String schoolCode) {
        String host = normalizeAndValidateSchoolHost(schoolCode);
        String accessToken = fetchAccessToken(host);

        URI enrollmentsUri = buildApiUri(host, "/enrollments");
        Map<String, Object> payload = getJsonWithBearer(enrollmentsUri, accessToken);

        return extractRecordCollection(payload, "enrollments").stream()
                .map(oneRosterMapper::toOneRosterEnrollment)
                .filter(enrollment -> enrollment.userSourcedId() != null && !enrollment.userSourcedId().isBlank())
                .filter(enrollment -> enrollment.classSourcedId() != null && !enrollment.classSourcedId().isBlank())
                .toList();
    }

    private List<OneRosterUserDTO> fetchUsers(String schoolCode) {
        String host = normalizeAndValidateSchoolHost(schoolCode);
        String accessToken = fetchAccessToken(host);

        URI usersUri = buildApiUri(host, "/users");
        Map<String, Object> payload = getJsonWithBearer(usersUri, accessToken);

        return extractRecordCollection(payload, "users").stream()
                .map(oneRosterMapper::toOneRosterUser)
                .filter(user -> user.sourcedId() != null && !user.sourcedId().isBlank())
                .sorted(Comparator.comparing(OneRosterUserDTO::displayName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private String fetchAccessToken(String schoolHost) {
        List<String> endpointCandidates = resolveTokenEndpoints(schoolHost);
        List<String> errors = new ArrayList<>();

        for (String endpoint : endpointCandidates) {
            TokenAttemptResult basicAttempt = fetchAccessTokenWithBasicAuth(endpoint);

            if (basicAttempt.token() != null) {
                return basicAttempt.token();
            }

            TokenAttemptResult postAttempt = TokenAttemptResult.notAttempted();

            if (tokenAuthMethod == TokenAuthMethod.POST || tokenAuthMethod == TokenAuthMethod.AUTO) {
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

        throw new ResponseStatusException(
                BAD_GATEWAY,
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

    private Map<String, Object> getJsonWithBearer(URI uri, String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    request,
                    new ParameterizedTypeReference<>() {
                    });

            return response.getBody() == null ? Map.of() : response.getBody();

        } catch (HttpStatusCodeException exception) {
            String sanitizedBody = sanitizeBody(exception.getResponseBodyAsString());

            throw new ResponseStatusException(
                    BAD_GATEWAY,
                    "OneRoster data ophalen mislukt: "
                            + exception.getStatusCode()
                            + (sanitizedBody == null ? "" : " " + sanitizedBody),
                    exception);

        } catch (RestClientException exception) {
            throw new ResponseStatusException(
                    BAD_GATEWAY,
                    "OneRoster data ophalen mislukt: " + exception.getClass().getSimpleName(),
                    exception);
        }
    }

    private List<String> extractDistinctSortedLabels(
            Map<String, Object> payload,
            String collectionKey,
            String... labelKeys) {
        List<Map<String, Object>> records = extractRecordCollection(payload, collectionKey);
        Set<String> labels = new LinkedHashSet<>();

        for (Map<String, Object> record : records) {
            String label = firstNonBlank(record, labelKeys);

            if (label != null) {
                labels.add(label);
            }
        }

        return labels.stream()
                .sorted(Comparator.comparing(String::toLowerCase))
                .toList();
    }

    private List<Map<String, Object>> extractRecordCollection(Map<String, Object> payload, String collectionKey) {
        Object root = payload.get(collectionKey);

        if (!(root instanceof List<?> items)) {
            return List.of();
        }

        List<Map<String, Object>> records = new ArrayList<>();

        for (Object item : items) {
            if (item instanceof Map<?, ?> mapItem) {
                records.add(mapItem.entrySet()
                        .stream()
                        .filter(entry -> entry.getKey() instanceof String)
                        .collect(java.util.stream.Collectors.toMap(
                                entry -> (String) entry.getKey(),
                                Map.Entry::getValue,
                                (left, right) -> left)));
            }
        }

        return records;
    }

    private String firstNonBlank(Map<String, Object> source, String... keys) {
        for (String key : keys) {
            Object value = source.get(key);

            if (value instanceof String text && !text.isBlank()) {
                return text.trim();
            }
        }

        return null;
    }

    private URI buildApiUri(String host, String endpoint) {
        return URI.create("https://" + host + apiPath + endpoint);
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

    private String normalizeAndValidateSchoolHost(String schoolCode) {
        String candidate = normalizeSchoolCodeToHost(schoolCode);
        validateHost(candidate);

        return candidate;
    }

    private String normalizeSchoolCodeToHost(String schoolCode) {
        if (schoolCode == null || schoolCode.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Geen geldige schoolcontext gevonden");
        }

        String trimmed = schoolCode.trim();

        try {
            URI uri = URI.create(trimmed);

            if (uri.getHost() != null) {
                return uri.getHost().toLowerCase(Locale.ROOT);
            }
        } catch (IllegalArgumentException ignored) {
            // Plain host without scheme.
        }

        if (trimmed.contains("/") || trimmed.contains("\\") || trimmed.contains("@") || trimmed.contains(":")) {
            throw new ResponseStatusException(BAD_REQUEST, "School host is ongeldig geformatteerd");
        }

        return trimmed.toLowerCase(Locale.ROOT);
    }

    private void validateHost(String host) {
        if (!host.matches("^[a-z0-9.-]+$")) {
            throw new ResponseStatusException(BAD_REQUEST, "School host bevat ongeldige tekens");
        }

        if (host.startsWith(".") || host.endsWith(".") || host.contains("..")) {
            throw new ResponseStatusException(BAD_REQUEST, "School host is ongeldig");
        }

        if (host.equals("localhost") || host.endsWith(".localhost") || isIpLiteral(host)) {
            throw new ResponseStatusException(BAD_REQUEST, "School host is niet toegestaan");
        }

        boolean matchesAllowedSuffix = allowedHostSuffixes.stream()
                .anyMatch(host::endsWith);

        if (!matchesAllowedSuffix) {
            throw new ResponseStatusException(BAD_REQUEST, "School host valt buiten toegestane domeinen");
        }

        try {
            InetAddress[] resolved = InetAddress.getAllByName(host);

            for (InetAddress address : resolved) {
                if (address.isAnyLocalAddress()
                        || address.isLoopbackAddress()
                        || address.isLinkLocalAddress()
                        || address.isSiteLocalAddress()
                        || address.isMulticastAddress()) {
                    throw new ResponseStatusException(BAD_REQUEST, "School host resolveert naar een privaat adres");
                }
            }

        } catch (UnknownHostException ex) {
            throw new ResponseStatusException(BAD_REQUEST, "School host kon niet gevalideerd worden", ex);
        }
    }

    private boolean isIpLiteral(String host) {
        if (host.matches("^(\\d{1,3}\\.){3}\\d{1,3}$")) {
            return true;
        }

        return host.contains(":");
    }

    private List<String> parseSuffixes(String rawSuffixes) {
        String suffixesSource = (rawSuffixes == null || rawSuffixes.isBlank())
                ? ".smartschool.be"
                : rawSuffixes;

        return java.util.Arrays.stream(suffixesSource.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> value.startsWith(".")
                        ? value.toLowerCase(Locale.ROOT)
                        : "." + value.toLowerCase(Locale.ROOT))
                .toList();
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

    @SuppressWarnings("unchecked")
    private String readMetadataString(Map<String, Object> user, String key) {
        Object metadataObject = user.get("metadata");

        if (!(metadataObject instanceof Map<?, ?> metadata)) {
            return null;
        }

        Object value = metadata.get(key);

        if (value instanceof String text && !text.isBlank()) {
            return text;
        }

        return null;
    }

    List<String> filterUmbrellaClassLabels(List<String> labels) {
        if (labels.size() < 2) {
            return labels;
        }

        List<String> filtered = new ArrayList<>();

        for (String candidate : labels) {
            if (!isUmbrellaClassLabel(candidate, labels)) {
                filtered.add(candidate);
            }
        }

        return filtered;
    }

    private boolean isUmbrellaClassLabel(String candidate, List<String> labels) {
        String candidateLower = candidate.toLowerCase(Locale.ROOT);

        for (String other : labels) {
            if (candidate.equals(other)) {
                continue;
            }

            String otherLower = other.toLowerCase(Locale.ROOT);

            if (!otherLower.startsWith(candidateLower) || other.length() <= candidate.length()) {
                continue;
            }

            char nextChar = other.charAt(candidate.length());

            if (Character.isDigit(nextChar)
                    || nextChar == '-'
                    || nextChar == '_'
                    || nextChar == '/'
                    || nextChar == '.') {
                return true;
            }
        }

        return false;
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

    public record AudiencePayload(
            List<String> classes,
            List<String> years,
            List<String> schools) {
    }
}