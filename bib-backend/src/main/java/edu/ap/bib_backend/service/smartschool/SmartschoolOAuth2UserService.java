package edu.ap.bib_backend.service.smartschool;

import edu.ap.bib_backend.dto.SmartschoolGroupDto;
import edu.ap.bib_backend.jpa.entity.Group;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.enums.Role;
import edu.ap.bib_backend.jpa.enums.SchoolStatus;
import edu.ap.bib_backend.jpa.repository.SchoolRepository;
import edu.ap.bib_backend.service.GroupService;
import edu.ap.bib_backend.service.EnvAdminService;
import edu.ap.bib_backend.service.user.UserService;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class SmartschoolOAuth2UserService
        implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    public static final String CLAIM_CLASS_CODE = "classCode";
    public static final String CLAIM_YEAR_CODE = "yearCode";
    public static final String CLAIM_SCHOOL_CODE = "schoolCode";

    private final UserService userService;
    private final EnvAdminService envAdminService;
    private final SmartschoolAudienceResolverService audienceResolverService;
    private final GroupService groupService;
    private final SmartschoolProfileService smartschoolProfileService;
    private final SchoolRepository schoolRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        String userInfoEndpoint = userRequest
                .getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUri();

        String accessToken = userRequest.getAccessToken().getTokenValue();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<?> userInfoResponse = restTemplate.exchange(
                userInfoEndpoint,
                HttpMethod.GET,
                entity,
                Map.class
        );

        Map<String, Object> attributes = audienceResolverService.toStringObjectMap(
                userInfoResponse.getBody() instanceof Map<?, ?> map ? map : null
        );

        String userId = requireStringAttribute(attributes, "userID");
        String username = attributes.get("username") instanceof String s ? s : null;
        String basisrol = requireStringAttribute(attributes, "basisrol");

        String classCode = audienceResolverService.resolveClassCode(attributes);
        if (SmartschoolAudienceResolverService.FALLBACK_CLASS_CODE.equalsIgnoreCase(classCode)) {
            String groupInfoClassCode = resolveClassCodeFromGroupInfo(userInfoEndpoint, accessToken);
            if (groupInfoClassCode != null) {
                classCode = groupInfoClassCode;
            }
        }

        String yearCode = audienceResolverService.resolveYearCode(attributes, classCode);
        String schoolCode = audienceResolverService.resolveSchoolCode(attributes);

        attributes.put(CLAIM_CLASS_CODE, classCode);
        attributes.put(CLAIM_YEAR_CODE, yearCode);

        String schoolDomain = mapSchoolCodeToDomainName(schoolCode);
        attributes.put(CLAIM_SCHOOL_CODE, schoolDomain);

        boolean isEnvAdmin = envAdminService.isEnvAdmin(userId, schoolDomain)
                || (username != null && envAdminService.isEnvAdmin(username, schoolDomain));

        if (isEnvAdmin) {
            School school = null;
            try {
                school = resolveSchool(schoolCode);
            } catch (OAuth2AuthenticationException ignored) {
            }

            attributes.put("schoolId", school != null ? school.getId() : null);

            fetchSmartschoolProfile(attributes, accessToken);

            return new DefaultOAuth2User(
                    Collections.singleton(new SimpleGrantedAuthority("ROLE_ADMIN")),
                    attributes,
                    "userID"
            );
        }

        School school = resolveSchool(schoolCode);

        User user = userService.getUser(userId)
                .orElseThrow(() -> new OAuth2AuthenticationException(
                        new OAuth2Error("user_not_authorized"),
                        "Je account is nog niet geregistreerd in het systeem. Neem contact op met de bibliotheekbeheerder."
                ));

        if (user.getSchool() == null || !user.getSchool().getId().equals(school.getId())) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("user_not_authorized"),
                    "Je account behoort niet tot deze school. Neem contact op met de bibliotheekbeheerder."
            );
        }

        if (user.getSchool() == null) {
            user.setSchool(school);
        }

        userService.save(user);

        SmartschoolGroupDto groupDto = null;
        try {
            groupDto = smartschoolProfileService.fetchGroups(accessToken);
        } catch (RuntimeException ignored) {
            // login blijft werken
        }

        if (groupDto != null) {
            String primaryGroupName = groupDto.primaryGroupName();

            if (primaryGroupName != null && !primaryGroupName.isBlank()) {
                attributes.put("primaryGroupName", primaryGroupName);

                Group group;
                try {
                    group = groupService.getGroup(school.getId(), primaryGroupName);
                } catch (RuntimeException e) {
                    group = groupService.createGroup(school.getId(), primaryGroupName);
                }

                if (group != null) {
                    user.setGroups(Set.of(group));
                    userService.save(user);
                }
            }

            if (groupDto.parentGroupNames() != null && !groupDto.parentGroupNames().isEmpty()) {
                attributes.put("parentGroupNames", groupDto.parentGroupNames());
            }
        }

        attributes.put("schoolId", school.getId());

        String roleString = getOAuthUserRole(user.getRole().getRole());

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(roleString)),
                attributes,
                "userID"
        );
    }

    private School resolveSchool(String schoolCode) {
        String domainName = mapSchoolCodeToDomainName(schoolCode);

        School school = schoolRepository.findByDomainName(domainName)
                .orElseThrow(() -> new OAuth2AuthenticationException(
                        new OAuth2Error("school_not_authorized"),
                        "Je school is niet gemachtigd voor deze website. Neem contact op met GO! Antwerpen voor hulp: https://go-antwerpen.be/contact/"
                ));

        if (school.getStatus() != SchoolStatus.ACTIVE) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("school_not_authorized"),
                    "Je school is momenteel niet beschikbaar. Neem contact op met GO! Antwerpen voor hulp: https://go-antwerpen.be/contact/"
            );
        }

        return school;
    }

    private String mapSchoolCodeToDomainName(String schoolCode) {
        if (schoolCode == null || schoolCode.isBlank()) {
            return "aphogeschool.smartschool.be";
        }

        String normalized = schoolCode.toLowerCase().trim();

        if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
            String domain = normalized
                    .replaceFirst("^https?://", "")
                    .replaceFirst("/$", "");

            if (domain.endsWith(".smartschool.be") || domain.equals("smartschool.be")) {
                return normalizeDomainName(domain);
            }

            throw new OAuth2AuthenticationException(
                    new OAuth2Error("unknown_school_code"),
                    "Onbekend Smartschool domein: " + domain
            );
        }

        String mapped = switch (normalized) {
            case "aphogeschool", "ap" -> "aphogeschool.smartschool.be";
            case "bazandpoort" -> "bazandpoort.smartschool.be";
            case "lachpoort" -> "lachpoort.smartschool.be";
            default -> null;
        };

        if (mapped != null) {
            return mapped;
        }

        return normalized + ".smartschool.be";
    }

    private String normalizeDomainName(String domain) {
        if (domain.startsWith("www.")) {
            domain = domain.substring(4);
        }
        return domain.toLowerCase();
    }

    private String requireStringAttribute(Map<String, Object> attributes, String key) {
        Object value = attributes.get(key);
        if (value instanceof String str && !str.isBlank()) {
            return str;
        }

        throw new OAuth2AuthenticationException(
                new OAuth2Error("invalid_user_info"),
                "Smartschool user info mist veld: " + key
        );
    }

    private String resolveClassCodeFromGroupInfo(String userInfoEndpoint, String accessToken) {
        String groupInfoEndpoint = toGroupInfoEndpoint(userInfoEndpoint);
        if (groupInfoEndpoint == null) {
            return null;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<?> groupInfoResponse = restTemplate.exchange(
                    groupInfoEndpoint,
                    HttpMethod.GET,
                    request,
                    Map.class
            );

            HttpStatusCode status = groupInfoResponse.getStatusCode();
            if (!status.is2xxSuccessful()) {
                return null;
            }

            Map<String, Object> payload = audienceResolverService.toStringObjectMap(
                    groupInfoResponse.getBody() instanceof Map<?, ?> map ? map : null
            );

            String resolved = audienceResolverService.resolveClassCode(payload);
            if (resolved == null || SmartschoolAudienceResolverService.FALLBACK_CLASS_CODE.equalsIgnoreCase(resolved)) {
                return null;
            }

            return resolved;
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private String toGroupInfoEndpoint(String userInfoEndpoint) {
        if (userInfoEndpoint == null || userInfoEndpoint.isBlank()) {
            return null;
        }

        String normalized = userInfoEndpoint.trim();

        if (normalized.endsWith("/fulluserinfo")) {
            return normalized.substring(0, normalized.length() - "fulluserinfo".length()) + "groupinfo";
        }

        if (normalized.endsWith("/userinfo")) {
            return normalized.substring(0, normalized.length() - "userinfo".length()) + "groupinfo";
        }

        return null;
    }

    private void fetchSmartschoolProfile(Map<String, Object> attributes, String accessToken) {
        try {
            SmartschoolGroupDto groupDto = smartschoolProfileService.fetchGroups(accessToken);
            if (groupDto != null) {
                String primaryGroupName = groupDto.primaryGroupName();
                if (primaryGroupName != null && !primaryGroupName.isBlank()) {
                    attributes.put("primaryGroupName", primaryGroupName);
                }
                if (groupDto.parentGroupNames() != null && !groupDto.parentGroupNames().isEmpty()) {
                    attributes.put("parentGroupNames", groupDto.parentGroupNames());
                }
            }
        } catch (RuntimeException ignored) {
        }
    }

    private String getOAuthUserRole(Role role) {
        return "ROLE_" + role.name();
    }
}
