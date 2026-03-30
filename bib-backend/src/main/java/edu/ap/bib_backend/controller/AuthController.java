package edu.ap.bib_backend.controller;

import java.io.IOException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.WebAttributes;
import org.springframework.web.bind.annotation.*;

import edu.ap.bib_backend.dto.AuthUserDto;
import edu.ap.bib_backend.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    @Value("${FRONTEND_REDIRECT_URL}")
    private String frontendRedirectUrl;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/me")
    public ResponseEntity<AuthUserDto> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        AuthUserDto userDetails = authService.getUserDetails(authentication);
        return userDetails != null ? ResponseEntity.ok(userDetails) : ResponseEntity.ok().build();
    }

    @GetMapping("/callback")
    public void callback(Authentication authentication, HttpServletResponse response) throws IOException {
        if (!(authentication instanceof OAuth2AuthenticationToken oauthToken) || !authentication.isAuthenticated()) {
            response.sendRedirect(frontendRedirectUrl + "?error=authentication_failed");
            return;
        }

        String jwt = authService.processCallbackAndGenerateJwt(oauthToken);
        response.sendRedirect(frontendRedirectUrl + "?token=" + jwt);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            authService.logout(authentication.getName());
            return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
        }
        return ResponseEntity.status(401).build();
    }

    @GetMapping("/error")
    public ResponseEntity<?> oauthError(HttpSession session, @RequestParam(value = "message", required = false) String message) {
        Object exception = session.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        String errorMessage = (message != null && !message.isBlank()) ? message : 
                             (exception == null ? "OAuth2 login failed" : exception.toString());
        return ResponseEntity.status(401).body(Map.of("error", errorMessage));
    }
}