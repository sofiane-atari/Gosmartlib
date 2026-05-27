package edu.ap.bib_backend.service.oneroster;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.client.SimpleClientHttpRequestFactory;

import edu.ap.bib_backend.dto.oneroster.OneRosterClassDTO;
import edu.ap.bib_backend.dto.oneroster.OneRosterEnrollmentDTO;
import edu.ap.bib_backend.dto.oneroster.OneRosterUserDTO;
import edu.ap.bib_backend.mapper.OneRosterMapper;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.repository.OneRosterCredentialRepository;
import edu.ap.bib_backend.jpa.repository.OneRosterTokenRepository;
import edu.ap.bib_backend.jpa.repository.SchoolRepository;
import edu.ap.bib_backend.service.api.SchoolApiProvider;
import edu.ap.bib_backend.service.api.SchoolApiProviderFactory;
import edu.ap.bib_backend.service.mock.MockSchoolData;
import edu.ap.bib_backend.service.mock.MockSchoolRegistry;

@Service
public class OneRosterClient {
    private static final String DEFAULT_ONE_ROSTER_PATH = "/ims/oneroster/v1p1";

    public record AudiencePayload(List<String> classes, List<String> years, List<String> schools) {
    }

    private final OneRosterHostValidator hostValidator;
    private final OneRosterTokenService tokenService;
    private final OneRosterDataService dataService;
    private final OneRosterMapper oneRosterMapper;
    private final SchoolRepository schoolRepository;
    private final SchoolApiProviderFactory providerFactory;
    private final MockSchoolRegistry mockSchoolRegistry;

    public OneRosterClient(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${oneroster.token-path:/ims/oneroster/token}") String tokenPath,
            @Value("${oneroster.api-path:" + DEFAULT_ONE_ROSTER_PATH + "}") String apiPath,
            @Value("${oneroster.allowed-host-suffixes:.smartschool.be}") String allowedHostSuffixes,
            @Value("${oneroster.connect-timeout-ms:5000}") int connectTimeoutMs,
            @Value("${oneroster.read-timeout-ms:7000}") int readTimeoutMs,
            @Value("${oneroster.token-auth-method:basic}") String tokenAuthMethod,
            @Value("${oneroster.scope:}") String tokenScope,
            @Value("${oneroster.base-url:}") String baseUrl,
            @Value("${oneroster.allow-private-hosts:false}") boolean allowPrivateHosts,
            OneRosterCredentialRepository credentialRepository,
            OneRosterTokenRepository tokenRepository,
            OneRosterMapper oneRosterMapper,
            SchoolRepository schoolRepository,
            SchoolApiProviderFactory providerFactory,
            MockSchoolRegistry mockSchoolRegistry) {
        RestTemplate template = restTemplateBuilder.build();
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMs);
        requestFactory.setReadTimeout(readTimeoutMs);
        template.setRequestFactory(requestFactory);

        this.oneRosterMapper = oneRosterMapper;
        this.hostValidator = new OneRosterHostValidator(allowedHostSuffixes, allowPrivateHosts);
        this.tokenService = new OneRosterTokenService(
                template,
                tokenPath,
                tokenAuthMethod,
                tokenScope,
                credentialRepository,
                tokenRepository);
        this.dataService = new OneRosterDataService(
                template,
                normalizeApiPath(apiPath),
                baseUrl,
                new OneRosterLabelFilter());
        this.schoolRepository = schoolRepository;
        this.providerFactory = providerFactory;
        this.mockSchoolRegistry = mockSchoolRegistry;
    }

    /**
     * @param schoolCode school domain name or URL
     * @return aggregated class, year, and school labels for the audience filter UI
     */
    public AudiencePayload fetchAudiencePayload(String schoolCode) {
        SchoolApiProvider mock = mockProvider(schoolCode);
        if (mock != null) {
            List<Map<String, Object>> rawClasses = mock.fetchClasses(schoolCode);
            List<Map<String, Object>> rawSessions = mock.fetchAcademicSessions(schoolCode);
            List<Map<String, Object>> rawOrgs = mock.fetchOrgs(schoolCode);
            List<String> classes = extractDistinctSortedLabels(rawClasses, "classCode", "title", "sourcedId");
            List<String> years = extractDistinctSortedLabels(rawSessions, "title", "schoolYear", "sourcedId");
            List<String> schools = extractDistinctSortedLabels(rawOrgs, "name", "title", "sourcedId");
            return new AudiencePayload(classes, years, schools);
        }
        String host = hostValidator.normalizeAndValidateSchoolHost(schoolCode);
        School school = resolveSchool(schoolCode);
        String accessToken = tokenService.fetchAccessToken(school, host);
        try {
            return dataService.fetchAudiencePayloadWithResolvedHost(host, accessToken);
        } catch (ResponseStatusException ex) {
            if (ex.getStatusCode().value() == 401) {
                tokenService.invalidateToken(school);
                String refreshed = tokenService.fetchAccessToken(school, host);
                return dataService.fetchAudiencePayloadWithResolvedHost(host, refreshed);
            }
            throw ex;
        }
    }

    /**
     * @param schoolCode school domain name or URL
     * @return raw OneRoster user records (List of Map), with 401 retry
     */
    public List<Map<String, Object>> fetchUsers(String schoolCode) {
        SchoolApiProvider mock = mockProvider(schoolCode);
        if (mock != null) return mock.fetchUsers(schoolCode);
        String host = hostValidator.normalizeAndValidateSchoolHost(schoolCode);
        School school = resolveSchool(schoolCode);
        String accessToken = tokenService.fetchAccessToken(school, host);
        try {
            return dataService.fetchUsersWithResolvedHost(host, accessToken);
        } catch (ResponseStatusException ex) {
            if (ex.getStatusCode().value() == 401) {
                tokenService.invalidateToken(school);
                String refreshed = tokenService.fetchAccessToken(school, host);
                return dataService.fetchUsersWithResolvedHost(host, refreshed);
            }
            throw ex;
        }
    }

    public AudiencePayload fetchAudiencePayloadWithBearer(String schoolCode, String bearerToken) {
        SchoolApiProvider mock = mockProvider(schoolCode);
        if (mock != null) return fetchAudiencePayload(schoolCode);
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

    public List<OneRosterClassDTO> fetchClassesWithBearer(String schoolCode, String bearerToken) {
        SchoolApiProvider mock = mockProvider(schoolCode);
        if (mock != null) return fetchClasses(schoolCode);
        String host = hostValidator.normalizeAndValidateSchoolHost(schoolCode);
        if (!StringUtils.hasText(bearerToken)) {
            return fetchClasses(schoolCode);
        }

        try {
            List<Map<String, Object>> classes = dataService.fetchClassesWithResolvedHost(host, bearerToken.trim());
            return classes.stream()
                    .map(oneRosterMapper::toOneRosterClass)
                    .filter(item -> item.sourcedId() != null && !item.sourcedId().isBlank())
                    .sorted(java.util.Comparator.comparing(OneRosterClassDTO::title, String.CASE_INSENSITIVE_ORDER))
                    .toList();
        } catch (ResponseStatusException ex) {
            return fetchClasses(schoolCode);
        }
    }

    public List<OneRosterEnrollmentDTO> fetchEnrollmentsWithBearer(String schoolCode, String bearerToken) {
        SchoolApiProvider mock = mockProvider(schoolCode);
        if (mock != null) return fetchEnrollments(schoolCode);
        String host = hostValidator.normalizeAndValidateSchoolHost(schoolCode);
        if (!StringUtils.hasText(bearerToken)) {
            return fetchEnrollments(schoolCode);
        }

        try {
            List<Map<String, Object>> enrollments = dataService.fetchEnrollmentsWithResolvedHost(host, bearerToken.trim());
            return enrollments.stream()
                    .map(oneRosterMapper::toOneRosterEnrollment)
                    .filter(enrollment -> enrollment.userSourcedId() != null && !enrollment.userSourcedId().isBlank())
                    .filter(enrollment -> enrollment.classSourcedId() != null && !enrollment.classSourcedId().isBlank())
                    .toList();
        } catch (ResponseStatusException ex) {
            return fetchEnrollments(schoolCode);
        }
    }

    public List<OneRosterUserDTO> fetchStudentsForClassWithBearer(String schoolCode, String bearerToken, String classSourcedId) {
        SchoolApiProvider mock = mockProvider(schoolCode);
        if (mock != null) return fetchStudentsForClass(schoolCode, classSourcedId);
        String host = hostValidator.normalizeAndValidateSchoolHost(schoolCode);
        if (!StringUtils.hasText(bearerToken)) {
            return fetchStudentsForClass(schoolCode, classSourcedId);
        }

        try {
            List<Map<String, Object>> students = dataService.fetchStudentsForClassWithResolvedHost(host, bearerToken.trim(), classSourcedId);
            return students.stream()
                    .map(oneRosterMapper::toOneRosterUser)
                    .filter(user -> user.sourcedId() != null && !user.sourcedId().isBlank())
                    .sorted(java.util.Comparator.comparing(OneRosterUserDTO::displayName, String.CASE_INSENSITIVE_ORDER))
                    .toList();
        } catch (ResponseStatusException ex) {
            return fetchStudentsForClass(schoolCode, classSourcedId);
        }
    }

    /**
     * @param schoolCode  school domain name or URL
     * @param bearerToken optional Smartschool delegated token
     * @return raw OneRoster user records (falls back to client-credentials if needed)
     */
    public List<Map<String, Object>> fetchUsersWithBearer(String schoolCode, String bearerToken) {
        SchoolApiProvider mock = mockProvider(schoolCode);
        if (mock != null) return fetchUsers(schoolCode);
        String host = hostValidator.normalizeAndValidateSchoolHost(schoolCode);
        if (!StringUtils.hasText(bearerToken)) {
            return fetchUsers(schoolCode);
        }

        try {
            return dataService.fetchUsersWithResolvedHost(host, bearerToken.trim());
        } catch (ResponseStatusException ex) {
            return fetchUsers(schoolCode);
        }
    }

    /**
     * Validates OneRoster credentials by attempting to fetch an access token
     * from the OneRoster API for the given school domain.
     *
     * @param clientId     the OAuth2 client ID
     * @param clientSecret the OAuth2 client secret
     * @param schoolDomain the school domain name or URL
     * @throws ResponseStatusException if credentials are invalid or host is invalid
     */
    public void validateCredentials(String clientId, String clientSecret, String schoolDomain) {
        SchoolApiProvider mock = mockProvider(schoolDomain);
        if (mock != null) {
            String normalized = normalizeSchoolCode(schoolDomain);
            MockSchoolData mockSchool = mockSchoolRegistry.getSchoolByDomain(normalized);
            if (mockSchool == null) {
                mockSchool = mockSchoolRegistry.getSchoolByCode(normalized);
            }
            if (mockSchool != null) {
                if (!mockSchool.getOnRosterClientId().equals(clientId)
                        || !mockSchool.getOnRosterClientSecret().equals(clientSecret)) {
                    throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_GATEWAY,
                            "OneRoster credentials ongeldig voor mock school.");
                }
            }
            return;
        }
        String host = hostValidator.normalizeAndValidateSchoolHost(schoolDomain);
        tokenService.validateCredentials(clientId, clientSecret, host);
    }

    public Map<String, String> fetchUserDisplayNames(String schoolCode, Set<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }

        SchoolApiProvider mock = mockProvider(schoolCode);
        if (mock != null) return mock.fetchUserDisplayNames(schoolCode, userIds);

        String host = hostValidator.normalizeAndValidateSchoolHost(schoolCode);
        School school = resolveSchool(schoolCode);
        String accessToken = tokenService.fetchAccessToken(school, host);
        try {
            return dataService.fetchUserDisplayNamesWithResolvedHost(host, accessToken, userIds);
        } catch (ResponseStatusException ex) {
            if (ex.getStatusCode().value() == 401) {
                tokenService.invalidateToken(school);
                String refreshed = tokenService.fetchAccessToken(school, host);
                return dataService.fetchUserDisplayNamesWithResolvedHost(host, refreshed, userIds);
            }
            throw ex;
        }
    }

    public String findSourcedIdByLegacyIdentifier(String schoolCode, String legacyIdentifier) {
        if (legacyIdentifier == null || legacyIdentifier.isBlank()) {
            return null;
        }

        SchoolApiProvider mock = mockProvider(schoolCode);
        if (mock != null) return mock.findSourcedIdByLegacyIdentifier(schoolCode, legacyIdentifier);

        String host = hostValidator.normalizeAndValidateSchoolHost(schoolCode);
        School school = resolveSchool(schoolCode);
        String accessToken = tokenService.fetchAccessToken(school, host);
        try {
            return dataService.findSourcedIdByLegacyIdentifierWithResolvedHost(host, accessToken, legacyIdentifier);
        } catch (ResponseStatusException ex) {
            if (ex.getStatusCode().value() == 401) {
                tokenService.invalidateToken(school);
                String refreshed = tokenService.fetchAccessToken(school, host);
                return dataService.findSourcedIdByLegacyIdentifierWithResolvedHost(host, refreshed, legacyIdentifier);
            }
            throw ex;
        }
    }

    public List<OneRosterClassDTO> fetchClasses(String schoolCode) {
        SchoolApiProvider mock = mockProvider(schoolCode);
        if (mock != null) {
            return mock.fetchClasses(schoolCode).stream()
                    .map(oneRosterMapper::toOneRosterClass)
                    .filter(item -> item.sourcedId() != null && !item.sourcedId().isBlank())
                    .sorted(java.util.Comparator.comparing(OneRosterClassDTO::title, String.CASE_INSENSITIVE_ORDER))
                    .toList();
        }
        String host = hostValidator.normalizeAndValidateSchoolHost(schoolCode);
        School school = resolveSchool(schoolCode);
        String accessToken = tokenService.fetchAccessToken(school, host);
        List<Map<String, Object>> classes = dataService.fetchClassesWithResolvedHost(host, accessToken);
        return classes.stream()
                .map(oneRosterMapper::toOneRosterClass)
                .filter(item -> item.sourcedId() != null && !item.sourcedId().isBlank())
                .sorted(java.util.Comparator.comparing(OneRosterClassDTO::title, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public List<OneRosterEnrollmentDTO> fetchEnrollments(String schoolCode) {
        SchoolApiProvider mock = mockProvider(schoolCode);
        if (mock != null) {
            return mock.fetchEnrollments(schoolCode).stream()
                    .map(oneRosterMapper::toOneRosterEnrollment)
                    .filter(enrollment -> enrollment.userSourcedId() != null && !enrollment.userSourcedId().isBlank())
                    .filter(enrollment -> enrollment.classSourcedId() != null && !enrollment.classSourcedId().isBlank())
                    .toList();
        }
        String host = hostValidator.normalizeAndValidateSchoolHost(schoolCode);
        School school = resolveSchool(schoolCode);
        String accessToken = tokenService.fetchAccessToken(school, host);
        List<Map<String, Object>> enrollments = dataService.fetchEnrollmentsWithResolvedHost(host, accessToken);
        return enrollments.stream()
                .map(oneRosterMapper::toOneRosterEnrollment)
                .filter(enrollment -> enrollment.userSourcedId() != null && !enrollment.userSourcedId().isBlank())
                .filter(enrollment -> enrollment.classSourcedId() != null && !enrollment.classSourcedId().isBlank())
                .toList();
    }

    private List<OneRosterUserDTO> fetchStudentsForClass(String schoolCode, String classSourcedId) {
        if (!StringUtils.hasText(classSourcedId)) {
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Geen geldige klas geselecteerd");
        }
        SchoolApiProvider mock = mockProvider(schoolCode);
        if (mock != null) {
            List<Map<String, Object>> students = mock.fetchStudentsForClass(schoolCode, classSourcedId);
            return students.stream()
                    .map(oneRosterMapper::toOneRosterUser)
                    .filter(user -> user.sourcedId() != null && !user.sourcedId().isBlank())
                    .sorted(java.util.Comparator.comparing(OneRosterUserDTO::displayName, String.CASE_INSENSITIVE_ORDER))
                    .toList();
        }
        String host = hostValidator.normalizeAndValidateSchoolHost(schoolCode);
        School school = resolveSchool(schoolCode);
        String accessToken = tokenService.fetchAccessToken(school, host);
        return fetchStudentsForClassWithResolvedHost(host, accessToken, classSourcedId);
    }

    private List<OneRosterUserDTO> fetchStudentsForClassWithResolvedHost(String host, String accessToken, String classSourcedId) {
        if (!StringUtils.hasText(classSourcedId)) {
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Geen geldige klas geselecteerd");
        }

        try {
            List<Map<String, Object>> students = dataService.fetchStudentsForClassWithResolvedHost(host, accessToken, classSourcedId);
            return students.stream()
                    .map(oneRosterMapper::toOneRosterUser)
                    .filter(user -> user.sourcedId() != null && !user.sourcedId().isBlank())
                    .sorted(java.util.Comparator.comparing(OneRosterUserDTO::displayName, String.CASE_INSENSITIVE_ORDER))
                    .toList();
        } catch (ResponseStatusException ex) {
            return List.of();
        }
    }

    private SchoolApiProvider mockProvider(String schoolCode) {
        return providerFactory.getProvider(normalizeSchoolCode(schoolCode)).orElse(null);
    }

    private School resolveSchool(String schoolCode) {
        String normalized = normalizeSchoolCode(schoolCode);
        return schoolRepository.findByDomainName(normalized)
                .orElseThrow(() -> new ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST,
                        "School niet gevonden voor domain: " + normalized));
    }

    private String normalizeSchoolCode(String schoolCode) {
        if (!StringUtils.hasText(schoolCode)) {
            return schoolCode;
        }

        String trimmed = schoolCode.trim();
        String withoutScheme = trimmed.replaceFirst("^https?://", "");
        int slashIndex = withoutScheme.indexOf('/');
        if (slashIndex > -1) {
            return withoutScheme.substring(0, slashIndex);
        }
        return withoutScheme;
    }

    @SuppressWarnings("unchecked")
    private List<String> extractDistinctSortedLabels(List<Map<String, Object>> items, String... keys) {
        return items.stream()
                .flatMap(item -> {
                    for (String key : keys) {
                        Object val = item.get(key);
                        if (val instanceof String s && !s.isBlank()) {
                            return java.util.stream.Stream.of(s.trim());
                        }
                    }
                    return java.util.stream.Stream.empty();
                })
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
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
