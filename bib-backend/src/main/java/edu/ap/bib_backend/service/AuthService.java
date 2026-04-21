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

import edu.ap.bib_backend.dto.AuthUserDto;
import edu.ap.bib_backend.dto.SmartschoolGroupDto;
import edu.ap.bib_backend.dto.SmartschoolProfileDto;
import edu.ap.bib_backend.jpa.enums.Role;
import edu.ap.bib_backend.jpa.repository.UserRepository;

@Service
public class AuthService {

    private final JwtService jwtService;
    private final TokenStorageService tokenStorageService;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final SmartschoolProfileService smartschoolProfileService;
    private final UserRepository userRepository;

    public AuthService(JwtService jwtService,
            TokenStorageService tokenStorageService,
            OAuth2AuthorizedClientService authorizedClientService,
            SmartschoolProfileService smartschoolProfileService,
            UserRepository userRepository) {
        this.jwtService = jwtService;
        this.tokenStorageService = tokenStorageService;
        this.authorizedClientService = authorizedClientService;
        this.smartschoolProfileService = smartschoolProfileService;
        this.userRepository = userRepository;
    }

    public AuthUserDto getUserDetails(Authentication authentication) {
        String userId = authentication.getName();
        Role role = extractRole(authentication.getAuthorities());
        String smartschoolToken = tokenStorageService.getSmartschoolAccessToken(userId);
        boolean reviewBlocked = userRepository.findById(userId)
                .map(user -> user.isReviewBlocked())
                .orElse(false);

        // 1. Probeer Smartschool API details op te halen
        if (smartschoolToken != null && !smartschoolToken.isBlank()) {
            try {
                SmartschoolProfileDto profile = smartschoolProfileService.fetchProfile(smartschoolToken);
                SmartschoolGroupDto groups = smartschoolProfileService.fetchGroups(smartschoolToken);

                return new AuthUserDto(
                        userId, profile.firstName(), profile.lastName(), profile.fullName(),
                        role, profile.email(), profile.userId(), profile.platform(),
                        groups.primaryGroupName(), groups.parentGroupNames(), reviewBlocked);
            } catch (Exception ignored) {
                /* Fallback */ }
        }

        // 2. Fallback naar OAuth2 principal
        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            String name = oauthToken.getPrincipal().getAttribute("name");
            return new AuthUserDto(userId, name, null, name, role, null, null, null, null, List.of(), reviewBlocked);
        }

        return null;
    }

    public String processCallbackAndGenerateJwt(OAuth2AuthenticationToken oauthToken) {
        String userId = oauthToken.getName();
        String roleStr = oauthToken.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No role found"));

        // Extraheer claims
        Map<String, Object> claims = Map.of(
                SmartschoolOAuth2UserService.CLAIM_CLASS_CODE,
                getAttributeOrDefault(oauthToken, SmartschoolOAuth2UserService.CLAIM_CLASS_CODE, "GENERAL"),
                SmartschoolOAuth2UserService.CLAIM_YEAR_CODE,
                getAttributeOrDefault(oauthToken, SmartschoolOAuth2UserService.CLAIM_YEAR_CODE, "0"),
                SmartschoolOAuth2UserService.CLAIM_SCHOOL_CODE,
                getAttributeOrDefault(oauthToken, SmartschoolOAuth2UserService.CLAIM_SCHOOL_CODE, "ALLEMAAL"));

        // Sla Smartschool Access Token op
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                oauthToken.getAuthorizedClientRegistrationId(), userId);

        if (client != null && client.getAccessToken() != null) {
            tokenStorageService.storeSmartschoolAccessToken(userId, client.getAccessToken().getTokenValue());
        }

        // Genereer en sla ons eigen JWT op
        String jwt = jwtService.generateToken(userId, roleStr, claims);
        tokenStorageService.storeToken(userId, jwt);
        tokenStorageService.storeUserMetadata(userId, roleStr);

        return jwt;
    }

    public void logout(String userId) {
        tokenStorageService.revokeToken(userId);
        tokenStorageService.removeUserMetadata(userId);
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
}
