package edu.ap.bib_backend.controller;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.WebAttributes;
import org.springframework.web.bind.annotation.*;

import edu.ap.bib_backend.dto.AuthUserDto;
import edu.ap.bib_backend.dto.SmartschoolGroupDto;
import edu.ap.bib_backend.dto.SmartschoolProfileDto;
import edu.ap.bib_backend.jpa.enums.Role;
import edu.ap.bib_backend.service.JwtService;
import edu.ap.bib_backend.service.SmartschoolOAuth2UserService;
import edu.ap.bib_backend.service.SmartschoolProfileService;
import edu.ap.bib_backend.service.TokenStorageService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final JwtService jwtService;
    private final TokenStorageService tokenStorageService;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final SmartschoolProfileService smartschoolProfileService;

    @Value("${FRONTEND_REDIRECT_URL}")
    private String frontendRedirectUrl;

    public AuthController(
            JwtService jwtService,
            TokenStorageService tokenStorageService,
            OAuth2AuthorizedClientService authorizedClientService,
            SmartschoolProfileService smartschoolProfileService
    ) {
        this.jwtService = jwtService;
        this.tokenStorageService = tokenStorageService;
        this.authorizedClientService = authorizedClientService;
        this.smartschoolProfileService = smartschoolProfileService;
    }


    // ME ENDPOINT
    @GetMapping("/me")
    public ResponseEntity<AuthUserDto> me(Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String userId = authentication.getName();
        Role role = extractRole(authentication.getAuthorities());

        String smartschoolToken = tokenStorageService.getSmartschoolAccessToken(userId);

        // 1. Probeer Smartschool API
        if (smartschoolToken != null && !smartschoolToken.isBlank()) {
            try {
                SmartschoolProfileDto profile =
                        smartschoolProfileService.fetchProfile(smartschoolToken);

                SmartschoolGroupDto groups =
                        smartschoolProfileService.fetchGroups(smartschoolToken);

                return ResponseEntity.ok(new AuthUserDto(
                        userId,
                        profile.firstName(),
                        profile.lastName(),
                        profile.fullName(),
                        role,
                        profile.email(),
                        profile.userId(),
                        profile.platform(),
                        groups.primaryGroupName(),
                        groups.parentGroupNames()
                ));
            } catch (Exception ignored) {
                // fallback hieronder
            }
        }

        // 2. Fallback naar OAuth2 principal
        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            OAuth2User principal = oauthToken.getPrincipal();

            String name = principal.getAttribute("name");

            return ResponseEntity.ok(new AuthUserDto(
                    userId,
                    name,
                    null,
                    name,
                    role,
                    null,
                    null,
                    null,
                    null,
                    List.of()
            ));
        }

        return ResponseEntity.ok().build();
    }

    // CALLBACK (LOGIN SUCCESS)
    @GetMapping("/callback")
    public void callback(Authentication authentication, HttpServletResponse response) throws IOException {

        if (!(authentication instanceof OAuth2AuthenticationToken oauthToken) || !authentication.isAuthenticated()) {
            response.sendRedirect(frontendRedirectUrl + "?error=authentication_failed");
            return;
        }

        OAuth2User principal = oauthToken.getPrincipal();

        String userId = oauthToken.getName();
        String role = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No role found"));

        // Claims
        String classCode = principal.getAttribute(SmartschoolOAuth2UserService.CLAIM_CLASS_CODE);
        String yearCode = principal.getAttribute(SmartschoolOAuth2UserService.CLAIM_YEAR_CODE);
        String schoolCode = principal.getAttribute(SmartschoolOAuth2UserService.CLAIM_SCHOOL_CODE);

        Map<String, Object> audienceClaims = Map.of(
                SmartschoolOAuth2UserService.CLAIM_CLASS_CODE, classCode == null ? "GENERAL" : classCode,
                SmartschoolOAuth2UserService.CLAIM_YEAR_CODE, yearCode == null ? "0" : yearCode,
                SmartschoolOAuth2UserService.CLAIM_SCHOOL_CODE, schoolCode == null ? "ALLEMAAL" : schoolCode
        );

        // SMARTSCHOOL ACCESS TOKEN
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                oauthToken.getAuthorizedClientRegistrationId(),
                oauthToken.getName()
        );

        if (client != null && client.getAccessToken() != null) {
            String smartschoolAccessToken = client.getAccessToken().getTokenValue();
            tokenStorageService.storeSmartschoolAccessToken(userId, smartschoolAccessToken);
        }

        // JWT MET CLAIMS
        String jwt = jwtService.generateToken(userId, role, audienceClaims);

        tokenStorageService.storeToken(userId, jwt);
        tokenStorageService.storeUserMetadata(userId, role);

        response.sendRedirect(frontendRedirectUrl + "?token=" + jwt);
    }

    // ERROR
    @GetMapping("/error")
    public ResponseEntity<?> oauthError(HttpSession session,
                                        @RequestParam(value = "message", required = false) String message) {

        Object exception = session.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);

        String errorMessage = message != null && !message.isBlank()
                ? message
                : (exception == null ? "OAuth2 login failed" : exception.toString());

        return ResponseEntity.status(401).body(Map.of("error", errorMessage));
    }

    // LOGOUT
    @PostMapping("/logout")
    public ResponseEntity<?> logout(Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String userId = authentication.getName();

        tokenStorageService.revokeToken(userId);
        tokenStorageService.removeUserMetadata(userId);

        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    // ROLE HELPER
    private Role extractRole(Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .map(Role::valueOf)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No valid role found"));
    }
}