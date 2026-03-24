package edu.ap.bib_backend.service;

import edu.ap.bib_backend.jpa.entity.Role;
import edu.ap.bib_backend.jpa.entity.User;
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

import java.util.Collections;
import java.util.Map;

@Service
public class SmartschoolOAuth2UserService
        implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    public static final String CLAIM_CLASS_CODE = "classCode";
    public static final String CLAIM_YEAR_CODE = "yearCode";
    public static final String CLAIM_SCHOOL_CODE = "schoolCode";

    private final RestTemplate restTemplate = new RestTemplate();
    private final UserService userService;
    private final RoleService roleService;
    private final SmartschoolAudienceResolverService audienceResolverService;

    public SmartschoolOAuth2UserService(
            UserService userService,
            RoleService roleService,
            SmartschoolAudienceResolverService audienceResolverService) {
        this.userService = userService;
        this.roleService = roleService;
        this.audienceResolverService = audienceResolverService;
    }

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
                Map.class);

        Map<String, Object> attributes = audienceResolverService.toStringObjectMap(
                userInfoResponse.getBody() instanceof Map<?, ?> map ? map : null);

        String userId = requireStringAttribute(attributes, "userID");
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

        // Expose audience data as claims carried through JWT/auth context.
        attributes.put(CLAIM_CLASS_CODE, classCode);
        attributes.put(CLAIM_YEAR_CODE, yearCode);
        attributes.put(CLAIM_SCHOOL_CODE, schoolCode);

        User user = userService.getUser(userId)
                .orElseGet(() -> userService.createUser(userId, roleService.getRoleEntity(basisrol)));

        String roleString = getOAuthUserRole(user.getRole().getRole());

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(roleString)),
                attributes,
                "userID");
    }

    private String getOAuthUserRole(Role role) {
        return "ROLE_" + role.name();
    }

    private String requireStringAttribute(Map<String, Object> attributes, String key) {
        Object value = attributes.get(key);
        if (value instanceof String str && !str.isBlank()) {
            return str;
        }

        throw new OAuth2AuthenticationException(new OAuth2Error("invalid_user_info"),
                "Smartschool user info mist veld: " + key);
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
                    Map.class);

            HttpStatusCode status = groupInfoResponse.getStatusCode();
            if (!status.is2xxSuccessful()) {
                return null;
            }

            Map<String, Object> payload = audienceResolverService.toStringObjectMap(
                    groupInfoResponse.getBody() instanceof Map<?, ?> map ? map : null);

            String resolved = audienceResolverService.resolveClassCode(payload);
            if (resolved == null || SmartschoolAudienceResolverService.FALLBACK_CLASS_CODE.equalsIgnoreCase(resolved)) {
                return null;
            }
            return resolved;
        } catch (RuntimeException ex) {
            // groupinfo is best-effort; keep login flow working when scope/endpoint is unavailable
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
}
