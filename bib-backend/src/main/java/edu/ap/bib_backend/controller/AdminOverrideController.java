package edu.ap.bib_backend.controller;

import edu.ap.bib_backend.dto.AdminOverrideRequestDto;
import edu.ap.bib_backend.dto.AdminOverrideStatusDto;
import edu.ap.bib_backend.service.AdminOverrideService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// TEMP_ADMIN_OVERRIDE: remove before production release.
@RestController
@RequestMapping("/auth/admin-override")
@Profile("!prod")
public class AdminOverrideController {

    private final AdminOverrideService adminOverrideService;

    public AdminOverrideController(AdminOverrideService adminOverrideService) {
        this.adminOverrideService = adminOverrideService;
    }

    @PostMapping
    public ResponseEntity<AdminOverrideStatusDto> setOverride(
            Authentication authentication,
            @RequestBody AdminOverrideRequestDto request,
            HttpServletRequest servletRequest) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(
                adminOverrideService.setOverride(
                        authentication.getName(),
                        request.enabled(),
                        servletRequest.getRemoteAddr()));
    }

    @GetMapping("/status")
    public ResponseEntity<AdminOverrideStatusDto> getStatus(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(
                adminOverrideService.getStatus(authentication.getName()));
    }
}
