package edu.ap.bib_backend.controller;

import lombok.RequiredArgsConstructor;
import lombok.val;

import java.io.IOException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.WebAttributes;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.ap.bib_backend.dto.AuthUserDto;
import edu.ap.bib_backend.service.AuthService;
import edu.ap.bib_backend.service.JwtService;
import edu.ap.bib_backend.service.TokenStorageService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final TokenStorageService tokenStorageService;

    @Value("${FRONTEND_REDIRECT_URL}")
    private String frontendRedirectUrl;

    public AuthController(AuthService authService, JwtService jwtService, TokenStorageService tokenStorageService) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.tokenStorageService = tokenStorageService;
    }

    @GetMapping("/me")
    public AuthUserDto me() {
        return authService.getCurrentUser();
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
                .map(auth -> auth.getAuthority())
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No role found for user"));

        // Generate JWT token
        String jwt = jwtService.generateToken(userId, role);

        // Store token in Redis with user ID key (no personal data)
        tokenStorageService.storeToken(userId, jwt);

        // Store role metadata for quick reference
        tokenStorageService.storeUserMetadata(userId, role);

        response.sendRedirect(frontendRedirectUrl + "?oauth-success=true&token=" + jwt);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@AuthenticationPrincipal OAuth2User principal) {
        String userId = principal.getAttribute("userID");
        tokenStorageService.revokeToken(userId);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

}
