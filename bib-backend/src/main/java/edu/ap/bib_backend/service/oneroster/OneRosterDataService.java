package edu.ap.bib_backend.service.oneroster;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;

class OneRosterDataService {
    private final RestTemplate restTemplate;
    private final String apiPath;
    private final OneRosterLabelFilter labelFilter;

    OneRosterDataService(RestTemplate restTemplate, String apiPath, OneRosterLabelFilter labelFilter) {
        this.restTemplate = restTemplate;
        this.apiPath = apiPath;
        this.labelFilter = labelFilter;
    }

    OneRosterClient.AudiencePayload fetchAudiencePayloadWithResolvedHost(String host, String accessToken) {
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

        // Use human-readable labels in the UI dropdown.
        // For class matching we prefer canonical codes over display titles,
        // so created CLASS targetValue aligns with student classCode claims.
        List<String> classes = labelFilter.filterUmbrellaClassLabels(extractDistinctSortedLabels(
                classPayload,
                "classes",
                "classCode",
                "identifier",
                "code",
                "sourcedId",
                "title",
                "name"));
        List<String> years = extractDistinctSortedLabels(yearPayload, "academicSessions", "title", "schoolYear",
                "identifier", "sourcedId");
        List<String> schools = extractDistinctSortedLabels(schoolPayload, "schools", "name", "title", "identifier",
                "sourcedId");

        if (schools.isEmpty()) {
            schools = extractDistinctSortedLabels(schoolPayload, "orgs", "name", "title", "identifier", "sourcedId");
        }

        return new OneRosterClient.AudiencePayload(classes, years, schools);
    }

    List<Map<String, Object>> fetchUsersWithResolvedHost(String host, String accessToken) {
        URI usersUri = buildApiUri(host, "/users");
        Map<String, Object> payload = getJsonWithBearer(usersUri, accessToken);
        return extractRecordCollection(payload, "users");
    }

    Map<String, String> fetchUserDisplayNamesWithResolvedHost(String host, String accessToken, Set<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }

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
                        this::buildDisplayName,
                        (left, right) -> left));
    }

    List<Map<String, Object>> fetchClassesWithResolvedHost(String host, String accessToken) {
        URI classesUri = buildApiUri(host, "/classes");
        Map<String, Object> payload = getJsonWithBearer(classesUri, accessToken);
        return extractRecordCollection(payload, "classes");
    }

    List<Map<String, Object>> fetchEnrollmentsWithResolvedHost(String host, String accessToken) {
        URI enrollmentsUri = buildApiUri(host, "/enrollments");
        Map<String, Object> payload = getJsonWithBearer(enrollmentsUri, accessToken);
        return extractRecordCollection(payload, "enrollments");
    }

    List<Map<String, Object>> fetchStudentsForClassWithResolvedHost(String host, String accessToken, String classSourcedId) {
        URI studentsUri = buildApiUri(host, "/classes/" + classSourcedId + "/students");
        Map<String, Object> payload = getJsonWithBearer(studentsUri, accessToken);
        List<Map<String, Object>> students = extractRecordCollection(payload, "users");
        if (students.isEmpty()) {
            students = extractRecordCollection(payload, "students");
        }
        return students;
    }

    String findSourcedIdByLegacyIdentifierWithResolvedHost(String host, String accessToken, String legacyIdentifier) {
        if (legacyIdentifier == null || legacyIdentifier.isBlank()) {
            return null;
        }

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
        } catch (org.springframework.web.client.HttpClientErrorException.Unauthorized unauthorized) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "OneRoster access token ongeldig", unauthorized);
        } catch (RestClientException exception) {
            throw new ResponseStatusException(BAD_GATEWAY, "OneRoster data ophalen mislukt", exception);
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
                records.add(mapItem.entrySet().stream()
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

    private String buildDisplayName(Map<String, Object> user) {
        String fullName = firstNonBlank(user, "fullName", "name", "displayName");

        if (fullName != null) {
            return fullName;
        }

        String givenName = firstNonBlank(user, "givenName", "firstName");
        String familyName = firstNonBlank(user, "familyName", "lastName", "surname");

        String combined = ((givenName == null ? "" : givenName) + " " + (familyName == null ? "" : familyName))
                .trim();

        if (!combined.isBlank()) {
            return combined;
        }

        String username = firstNonBlank(user, "username", "userName");

        return username != null ? username : "Onbekende gebruiker";
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

    private URI buildApiUri(String host, String endpoint) {
        return URI.create("https://" + host + apiPath + endpoint);
    }
}
