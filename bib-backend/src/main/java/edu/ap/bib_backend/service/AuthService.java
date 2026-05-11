package edu.ap.bib_backend.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import edu.ap.bib_backend.dto.AuthUserDto;
import edu.ap.bib_backend.dto.JwtTokenDTO;
import edu.ap.bib_backend.dto.SchoolSummaryDto;
import edu.ap.bib_backend.dto.SmartschoolGroupDto;
import edu.ap.bib_backend.dto.SmartschoolProfileDto;
import edu.ap.bib_backend.dto.SmartschoolRefreshResult;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.enums.Role;
import edu.ap.bib_backend.jpa.repository.SchoolRepository;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import edu.ap.bib_backend.service.smartschool.SmartschoolOAuth2UserService;
import edu.ap.bib_backend.service.smartschool.SmartschoolProfileService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.MultiValueMap;

@Service
public class AuthService {

    private final JwtService jwtService;
    private final TokenStorageService tokenStorageService;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final SmartschoolProfileService smartschoolProfileService;
    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;

    @Value("${spring.security.oauth2.client.registration.smartschool.client-id}")
    private String smartschoolClientId;

    @Value("${spring.security.oauth2.client.registration.smartschool.client-secret}")
    private String smartschoolClientSecret;

    @Value("${spring.security.oauth2.client.provider.smartschool.token-uri}")
    private String smartschoolTokenUri;

    public AuthService(
            JwtService jwtService,
            TokenStorageService tokenStorageService,
            OAuth2AuthorizedClientService authorizedClientService,
            SmartschoolProfileService smartschoolProfileService,
            UserRepository userRepository,
            SchoolRepository schoolRepository) {
        this.jwtService = jwtService;
        this.tokenStorageService = tokenStorageService;
        this.authorizedClientService = authorizedClientService;
        this.smartschoolProfileService = smartschoolProfileService;
        this.userRepository = userRepository;
        this.schoolRepository = schoolRepository;
    }

    public AuthUserDto getUserDetails(Authentication authentication) {
        String userId = authentication.getName();
        Role role = extractRole(authentication.getAuthorities());
        String smartschoolToken = tokenStorageService.getSmartschoolAccessToken(userId);

        User dbUser = userRepository.findById(userId).orElse(null);

        boolean reviewBlocked = dbUser != null && dbUser.isReviewBlocked();

        Long schoolId = dbUser != null && dbUser.getSchool() != null
                ? dbUser.getSchool().getId()
                : null;

        String schoolName = dbUser != null && dbUser.getSchool() != null
                ? dbUser.getSchool().getName()
                : null;

        Long campusId = dbUser != null && dbUser.getCampus() != null
                ? dbUser.getCampus().getId()
                : null;

        String campusName = dbUser != null && dbUser.getCampus() != null
                ? dbUser.getCampus().getName()
                : null;

        SchoolSummaryDto schoolSummary = dbUser != null && dbUser.getSchool() != null
                ? dbUser.getSchool().getSchoolSummaryDto()
                : null;
        if (smartschoolToken != null && !smartschoolToken.isBlank()) {
            try {
                SmartschoolProfileDto profile = smartschoolProfileService.fetchProfile(smartschoolToken);
                SmartschoolGroupDto groups = smartschoolProfileService.fetchGroups(smartschoolToken);

                return new AuthUserDto(
                        userId,
                        profile.firstName(),
                        profile.lastName(),
                        profile.fullName(),
                        role,
                        profile.email(),
                        profile.userId(),
                        profile.platform(),
                        groups.primaryGroupName(),
                        groups.parentGroupNames(),
                        reviewBlocked,
                        schoolId,
                        schoolName,
                        campusId,
                        campusName,
                        schoolSummary
                );
            } catch (Exception ignored) {
                // Fallback hieronder
            }
        }

        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            String name = oauthToken.getPrincipal().getAttribute("name");

            return new AuthUserDto(
                    userId,
                    name,
                    null,
                    name,
                    role,
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    reviewBlocked,
                    schoolId,
                    schoolName,
                    campusId,
                    campusName,
                    schoolSummary
            );
        }

        return new AuthUserDto(
                userId,
                null,
                null,
                null,
                role,
                null,
                null,
                null,
                null,
                List.of(),
                reviewBlocked,
                schoolId,
                schoolName,
                campusId,
                campusName,
                schoolSummary
        );
    }

    public String processCallbackAndGenerateJwt(OAuth2AuthenticationToken oauthToken) {
        String userId = oauthToken.getName();
        String roleStr = oauthToken.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No role found"));

        Map<String, Object> claims = Map.of(
                SmartschoolOAuth2UserService.CLAIM_CLASS_CODE,
                getAttributeOrDefault(oauthToken, SmartschoolOAuth2UserService.CLAIM_CLASS_CODE, "GENERAL"),
                SmartschoolOAuth2UserService.CLAIM_YEAR_CODE,
                getAttributeOrDefault(oauthToken, SmartschoolOAuth2UserService.CLAIM_YEAR_CODE, "0"),
                SmartschoolOAuth2UserService.CLAIM_SCHOOL_CODE,
                getAttributeOrDefault(oauthToken, SmartschoolOAuth2UserService.CLAIM_SCHOOL_CODE, "ALLEMAAL")
        );

        String jwt = jwtService.generateToken(userId, roleStr, claims);

        tokenStorageService.storeToken(
                userId,
                jwt,
                roleStr,
                jwtService.extractExpirationInstant(jwt)
        );

        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                oauthToken.getAuthorizedClientRegistrationId(),
                userId
        );

        if (client != null && client.getAccessToken() != null) {
            String smartschoolAccessToken = client.getAccessToken().getTokenValue();

            String smartschoolRefreshToken = client.getRefreshToken() != null
                    ? client.getRefreshToken().getTokenValue()
                    : null;

            tokenStorageService.storeSmartschoolTokens(
                    userId,
                    smartschoolAccessToken,
                    smartschoolRefreshToken
            );
        }

        return jwt;
    }

    public void logout(String userId) {
        tokenStorageService.revokeToken(userId);
    }

    private Role extractRole(Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .map(Role::valueOf)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No valid role found"));
    }

    private String getAttributeOrDefault(OAuth2AuthenticationToken token, String attr, String defaultValue) {
        Object val = token.getPrincipal().getAttribute(attr);
        return val == null ? defaultValue : val.toString();
    }

    public JwtTokenDTO refreshJwtToken(String oldToken) {
        if (!jwtService.isValid(oldToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        }

        String userId = jwtService.extractUsername(oldToken);
        String role = (String) jwtService.extractClaim(oldToken, "roles");

        if (!tokenStorageService.isCurrentToken(userId, oldToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token is no longer active");
        }

        String smartschoolRefreshToken = tokenStorageService.getSmartschoolRefreshToken(userId);

        if (smartschoolRefreshToken == null || smartschoolRefreshToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No Smartschool refresh token available");
        }

        SmartschoolRefreshResult refreshResult = refreshSmartschoolAccessToken(smartschoolRefreshToken);

        String newToken = jwtService.generateToken(userId, role);

        tokenStorageService.replaceToken(
                userId,
                newToken,
                role,
                jwtService.extractExpirationInstant(newToken)
        );

        tokenStorageService.storeSmartschoolTokens(
                userId,
                refreshResult.accessToken(),
                refreshResult.refreshToken() != null
                        ? refreshResult.refreshToken()
                        : smartschoolRefreshToken
        );

        return new JwtTokenDTO(
                newToken,
                jwtService.extractExpirationInstant(newToken)
        );
    }

    private SmartschoolRefreshResult refreshSmartschoolAccessToken(String refreshToken) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("refresh_token", refreshToken);
        body.add("client_id", smartschoolClientId);
        body.add("client_secret", smartschoolClientSecret);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response;

        try {
            response = restTemplate.exchange(
                    smartschoolTokenUri,
                    HttpMethod.POST,
                    request,
                    Map.class
            );
        } catch (HttpStatusCodeException ex) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Smartschool refresh failed",
                    ex
            );
        }

        Map<String, Object> responseBody = response.getBody();

        if (!response.getStatusCode().is2xxSuccessful() || responseBody == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Smartschool refresh failed");
        }

        Object accessToken = responseBody.get("access_token");
        Object newRefreshToken = responseBody.get("refresh_token");

        if (!(accessToken instanceof String accessTokenString) || accessTokenString.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Smartschool did not return access token");
        }

        return new SmartschoolRefreshResult(
                accessTokenString,
                newRefreshToken instanceof String refreshTokenString ? refreshTokenString : null
        );
    }
}