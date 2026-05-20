package edu.ap.bib_backend.controller;

import java.io.IOException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.WebAttributes;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import edu.ap.bib_backend.dto.AuthUserDto;
import edu.ap.bib_backend.dto.JwtTokenDTO;
import edu.ap.bib_backend.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;


@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    @Value("${frontend.redirect-url:http://localhost:4200/login}")
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
    public void oauthError(HttpSession session, HttpServletResponse response) throws IOException {
        Object exception = session.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        String errorCode = "authentication_failed";

        if (exception instanceof org.springframework.security.oauth2.core.OAuth2AuthenticationException oauthEx) {
            String errorType = oauthEx.getError().getErrorCode();
            if ("school_not_authorized".equals(errorType)) {
                errorCode = "school_not_authorized";
            }
        }

        response.sendRedirect(frontendRedirectUrl + "?error=" + errorCode);
    }

    @PostMapping("/refresh")
    public ResponseEntity<JwtTokenDTO> refresh(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing bearer token");
        }

        String oldToken = authHeader.substring(7);

        return ResponseEntity.ok(authService.refreshJwtToken(oldToken));
    }
}