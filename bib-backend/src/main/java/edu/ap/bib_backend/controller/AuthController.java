package edu.ap.bib_backend.controller;

import java.io.IOException;
import java.util.Map;

import edu.ap.bib_backend.jpa.entity.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.WebAttributes;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.ap.bib_backend.dto.AuthUserDto;
import edu.ap.bib_backend.service.JwtService;
import edu.ap.bib_backend.service.TokenStorageService;
import edu.ap.bib_backend.service.SmartschoolOAuth2UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final JwtService jwtService;
    private final TokenStorageService tokenStorageService;

    @Value("${FRONTEND_REDIRECT_URL}")
    private String frontendRedirectUrl;

    public AuthController(JwtService jwtService, TokenStorageService tokenStorageService) {
        this.jwtService = jwtService;
        this.tokenStorageService = tokenStorageService;
    }

    @GetMapping("/me")
    public AuthUserDto me(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return null;
        }

        String userId = principal.getAttribute("userID");
        String name = principal.getAttribute("name");
        String role = principal.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("ROLE_USER");

        return new AuthUserDto(userId, name,
                role.startsWith("ROLE_") ? Role.valueOf(role.substring(5)) : Role.valueOf(role));
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
    public void callback(@AuthenticationPrincipal OAuth2User principal,
                         HttpServletResponse response) throws IOException {

        // Extract user ID from Smartschool (only ID, no PII)
        String userId = principal.getAttribute("userID");

        // Extract role from authorities set by SmartschoolOAuth2UserService
        String role = principal.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No role found for user"));

        String classCode = principal.getAttribute(SmartschoolOAuth2UserService.CLAIM_CLASS_CODE);
        String yearCode = principal.getAttribute(SmartschoolOAuth2UserService.CLAIM_YEAR_CODE);
        String schoolCode = principal.getAttribute(SmartschoolOAuth2UserService.CLAIM_SCHOOL_CODE);

        Map<String, Object> audienceClaims = Map.of(
                SmartschoolOAuth2UserService.CLAIM_CLASS_CODE, classCode == null ? "GENERAL" : classCode,
                SmartschoolOAuth2UserService.CLAIM_YEAR_CODE, yearCode == null ? "0" : yearCode,
                SmartschoolOAuth2UserService.CLAIM_SCHOOL_CODE, schoolCode == null ? "ALLEMAAL" : schoolCode
        );

        // Generate JWT token
        String jwt = jwtService.generateToken(userId, role, audienceClaims);

        // Store token in Redis with user ID key (no personal data)
        tokenStorageService.storeToken(userId, jwt);

        // Store role metadata for quick reference
        tokenStorageService.storeUserMetadata(userId, role);

        response.sendRedirect(frontendRedirectUrl + "?token=" + jwt);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@AuthenticationPrincipal OAuth2User principal) {
        String userId = principal.getAttribute("userID");
        tokenStorageService.revokeToken(userId);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}
