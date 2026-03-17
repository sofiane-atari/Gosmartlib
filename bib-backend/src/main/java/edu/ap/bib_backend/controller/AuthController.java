package edu.ap.bib_backend.controller;

import lombok.RequiredArgsConstructor;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.ap.bib_backend.dto.AuthUserDto;
import edu.ap.bib_backend.service.AuthService;
import edu.ap.bib_backend.service.JwtService;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @GetMapping("/me")
    public AuthUserDto me() {
        return authService.getCurrentUser();
    }

    @GetMapping("/callback")
    public ResponseEntity<?> callback(@AuthenticationPrincipal OAuth2User principal,
            HttpServletResponse response) {
        String username = principal.getAttribute("username");
        String roles = principal.getAuthorities().toString();

        String jwt = jwtService.generateToken(username, roles);

        return ResponseEntity.ok(Map.of("token", jwt));
    }

}
