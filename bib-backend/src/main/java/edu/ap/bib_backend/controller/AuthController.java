package edu.ap.bib_backend.controller;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.WebAttributes;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.ap.bib_backend.dto.AuthUserDto;
import edu.ap.bib_backend.dto.SmartschoolGroupDto;
import edu.ap.bib_backend.dto.SmartschoolProfileDto;
import edu.ap.bib_backend.jpa.enums.Role;
import edu.ap.bib_backend.service.JwtService;
import edu.ap.bib_backend.service.SmartschoolProfileService;
import edu.ap.bib_backend.service.TokenStorageService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final JwtService jwtService;
    private final TokenStorageService tokenStorageService;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final SmartschoolProfileService smartschoolProfileService;

    @Value("${FRONTEND_REDIRECT_URL}")
    private String frontendRedirectUrl;

    public AuthController(JwtService jwtService, TokenStorageService tokenStorageService, OAuth2AuthorizedClientService authorizedClientService, SmartschoolProfileService smartschoolProfileService) {
        this.jwtService = jwtService;
        this.tokenStorageService = tokenStorageService;
        this.authorizedClientService = authorizedClientService;
        this.smartschoolProfileService = smartschoolProfileService;
    }

    @GetMapping("/me")
    public ResponseEntity<AuthUserDto> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String userId = authentication.getName();
        Role role = extractRole(authentication.getAuthorities());

        String smartschoolAccessToken = tokenStorageService.getSmartschoolAccessToken(userId);

        if (smartschoolAccessToken == null || smartschoolAccessToken.isBlank()) {
            AuthUserDto fallbackDto = new AuthUserDto(
                userId,
                null,
                null,
                null,
                role,
                null,
                null,
                null,
                null,
                List.of()
            );
            return ResponseEntity.ok(fallbackDto);
        }

        try {
            SmartschoolProfileDto smartschoolProfile =
                    smartschoolProfileService.fetchProfile(smartschoolAccessToken);

            SmartschoolGroupDto smartschoolGroups =
                    smartschoolProfileService.fetchGroups(smartschoolAccessToken);

            AuthUserDto dto = new AuthUserDto(
                userId,
                smartschoolProfile.firstName(),
                smartschoolProfile.lastName(),
                smartschoolProfile.fullName(),
                role,
                smartschoolProfile.email(),
                smartschoolProfile.userId(),
                smartschoolProfile.platform(),
                smartschoolGroups.primaryGroupName(),
                smartschoolGroups.parentGroupNames()
            );

            return ResponseEntity.ok(dto);
        } catch (Exception exception) {
            AuthUserDto fallbackDto = new AuthUserDto(
                userId,
                null,
                null,
                null,
                role,
                null,
                null,
                null,
                null,
                List.of()
            );
            return ResponseEntity.ok(fallbackDto);
        }
    }

    @GetMapping("/error")
    public ResponseEntity<?> oauthError(HttpSession session,
            @RequestParam(value = "message", required = false) String message) {
        Object exception = session.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        String errorMessage = message != null && !message.isBlank()
                ? message
                : (exception == null ? "OAuth2 login failed" : exception.toString());
        return ResponseEntity.status(401).body(Map.of("error", errorMessage));
    }

    @GetMapping("/callback")
    public void callback(Authentication authentication, HttpServletResponse response) throws IOException {
        if (!(authentication instanceof OAuth2AuthenticationToken oauthToken) || !authentication.isAuthenticated()) {
            response.sendRedirect(frontendRedirectUrl + "?error=authentication_failed");
            return;
        }

        String userId = oauthToken.getName();
        Role role = extractRole(authentication.getAuthorities());
        String roleAuthority = "ROLE_" + role.name();

        OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
            oauthToken.getAuthorizedClientRegistrationId(),
            oauthToken.getName()
        );

        if (authorizedClient == null || authorizedClient.getAccessToken() == null) {
            response.sendRedirect(frontendRedirectUrl + "?error=missing_access_token");
            return;
        }

        String smartschoolAccessToken = authorizedClient.getAccessToken().getTokenValue();

        String jwt = jwtService.generateToken(userId, roleAuthority);

        tokenStorageService.storeToken(userId, jwt);
        tokenStorageService.storeUserMetadata(userId, roleAuthority);
        tokenStorageService.storeSmartschoolAccessToken(userId, smartschoolAccessToken);

        response.sendRedirect(frontendRedirectUrl + "?token=" + jwt);
    }

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

    private Role extractRole(Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring(5))
                .map(Role::valueOf)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No valid role found"));
    }

}
