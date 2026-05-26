package edu.ap.bib_backend.service.smartschool;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import edu.ap.bib_backend.dto.SmartschoolGroupDto;
import edu.ap.bib_backend.dto.SmartschoolProfileDto;
import edu.ap.bib_backend.dto.groupinfo.GroupInfoItemDTO;
import edu.ap.bib_backend.dto.groupinfo.SmartschoolGroupInfoDTO;
import edu.ap.bib_backend.service.JwtService;
import edu.ap.bib_backend.service.api.SchoolApiProvider;
import edu.ap.bib_backend.service.api.SchoolApiProviderFactory;
import edu.ap.bib_backend.service.mock.MockSchoolRegistry;

@Service
public class SmartschoolProfileService {

    private final RestTemplate restTemplate;
    private final String fullUserInfoUrl;
    private final String groupInfoUrl;
    private final SchoolApiProviderFactory providerFactory;
    private final MockSchoolRegistry mockSchoolRegistry;
    private final JwtService jwtService;

    public SmartschoolProfileService(
            RestTemplate restTemplate,
            @Value("${smartschool.fulluserinfo-url}") String fullUserInfoUrl,
            @Value("${smartschool.groupinfo-url}") String groupInfoUrl,
            SchoolApiProviderFactory providerFactory,
            MockSchoolRegistry mockSchoolRegistry,
            JwtService jwtService) {
        this.restTemplate = restTemplate;
        this.fullUserInfoUrl = fullUserInfoUrl;
        this.groupInfoUrl = groupInfoUrl;
        this.providerFactory = providerFactory;
        this.mockSchoolRegistry = mockSchoolRegistry;
        this.jwtService = jwtService;
    }

    private SchoolApiProvider resolveMockProvider(String accessToken) {
        try {
            String legacyId = jwtService.extractUsername(accessToken);
            if (legacyId == null) return null;
            var school = mockSchoolRegistry.getSchoolByLegacyId(legacyId);
            if (school != null) {
                return providerFactory.getProvider(school.getDomainName()).orElse(null);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public SmartschoolProfileDto fetchProfile(String accessToken) {
        SchoolApiProvider mock = resolveMockProvider(accessToken);
        if (mock != null) return mock.fetchProfile(accessToken);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    fullUserInfoUrl,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<>() {
                    });

            Map<String, Object> body = response.getBody();
            if (body == null) {
                throw new IllegalStateException("Smartschool profile response was empty");
            }

            String firstName = readString(body, "name");
            String lastName = readString(body, "surname");
            String fullName = readString(body, "fullname");
            String email = readString(body, "email");
            String platform = readString(body, "platform");
            String smartschoolId = readString(body, "userID");

            return new SmartschoolProfileDto(
                    smartschoolId,
                    firstName,
                    lastName,
                    fullName,
                    email,
                    platform,
                    readString(body, "username"));
        } catch (RestClientException exception) {
            throw new IllegalStateException("Failed to fetch Smartschool profile", exception);
        }
    }

    public SmartschoolGroupDto fetchGroups(String accessToken) {
        SchoolApiProvider mock = resolveMockProvider(accessToken);
        if (mock != null) return mock.fetchGroups(accessToken);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    groupInfoUrl,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<>() {
                    });

            Map<String, Object> body = response.getBody();
            if (body == null) {
                throw new IllegalStateException("Smartschool group response was empty");
            }

            String primaryGroupName = null;
            List<String> parentGroupNames = new ArrayList<>();

            Object groupsObject = body.get("groups");
            if (groupsObject instanceof List<?> groups && !groups.isEmpty()) {
                Object firstGroup = groups.get(0);
                if (firstGroup instanceof Map<?, ?> groupMap) {
                    Object groupName = groupMap.get("name");
                    if (groupName instanceof String value && !value.isBlank()) {
                        primaryGroupName = value;
                    }
                }
            }

            Object parentGroupsObject = body.get("parentGroups");
            if (parentGroupsObject instanceof List<?> parentGroups) {
                for (Object parentGroup : parentGroups) {
                    if (parentGroup instanceof Map<?, ?> parentGroupMap) {
                        Object parentGroupName = parentGroupMap.get("name");
                        if (parentGroupName instanceof String value && !value.isBlank()) {
                            parentGroupNames.add(value);
                        }
                    }
                }
            }

            return new SmartschoolGroupDto(primaryGroupName, parentGroupNames);
        } catch (RestClientException exception) {
            throw new IllegalStateException("Failed to fetch Smartschool groups", exception);
        }
    }

    public SmartschoolGroupInfoDTO fetchGroupInfo(String accessToken, String platform) {
        SchoolApiProvider mock = resolveMockProvider(accessToken);
        if (mock != null) {
            SmartschoolGroupDto groups = mock.fetchGroups(accessToken);
            List<GroupInfoItemDTO> groupItems = new ArrayList<>();
            if (groups.primaryGroupName() != null) {
                groupItems.add(new GroupInfoItemDTO("mock-group-id", groups.primaryGroupName(), groups.primaryGroupName(), platform, null, null));
            }
            List<GroupInfoItemDTO> parentItems = new ArrayList<>();
            for (String parentName : groups.parentGroupNames()) {
                parentItems.add(new GroupInfoItemDTO("mock-parent-" + parentName.hashCode(), parentName, parentName, platform, null, null));
            }
            return new SmartschoolGroupInfoDTO(groupItems, parentItems);
        }
        String baseUrl = normalizePlatform(platform);

        ResponseEntity<SmartschoolGroupInfoDTO> response = new RestTemplate().getForEntity(
                baseUrl + "/Api/V1/groupinfo?access_token=" + accessToken,
                SmartschoolGroupInfoDTO.class);

        return response.getBody();
    }

    private String normalizePlatform(String platform) {
        if (platform == null || platform.isBlank()) {
            throw new IllegalArgumentException("Geen Smartschool platform gevonden");
        }

        String trimmed = platform.trim();

        if (trimmed.endsWith("/")) {
            return trimmed.substring(0, trimmed.length() - 1);
        }

        return trimmed;
    }

    private String readString(Map<String, Object> source, String key) {
        Object value = source.get(key);
        if (value instanceof String text && !text.isBlank()) {
            return text;
        }
        return null;
    }
}