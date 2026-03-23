package edu.ap.bib_backend.service;

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

@Service
public class SmartschoolProfileService {

    private final RestTemplate restTemplate;
    private final String fullUserInfoUrl;
    private final String groupInfoUrl;

    public SmartschoolProfileService(
            RestTemplate restTemplate,
            @Value("${smartschool.fulluserinfo-url}") String fullUserInfoUrl,
            @Value("${smartschool.groupinfo-url}") String groupInfoUrl) {
        this.restTemplate = restTemplate;
        this.fullUserInfoUrl = fullUserInfoUrl;
        this.groupInfoUrl = groupInfoUrl;
    }

    public SmartschoolProfileDto fetchProfile(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    fullUserInfoUrl,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<>() {});

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
                    platform);
        } catch (RestClientException exception) {
            throw new IllegalStateException("Failed to fetch Smartschool profile", exception);
        }
    }

    public SmartschoolGroupDto fetchGroups(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    groupInfoUrl,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<>() {});

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

    private String readString(Map<String, Object> source, String key) {
        Object value = source.get(key);
        if (value instanceof String text && !text.isBlank()) {
            return text;
        }
        return null;
    }
}