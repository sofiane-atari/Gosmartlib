package edu.ap.bib_backend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import edu.ap.bib_backend.dto.AssignLibrarianRequestDto;
import edu.ap.bib_backend.dto.AuthUserDto;
import edu.ap.bib_backend.service.RoleService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/roles")
@Profile("!prod")
public class AdminPromotionController {

    private final RoleService roleService;
    private final boolean devModeEnabled;

    public AdminPromotionController(
            RoleService roleService,
            @Value("${app.dev-mode-enabled:false}") boolean devModeEnabled) {
        this.roleService = roleService;
        this.devModeEnabled = devModeEnabled;
    }

    private void requireDevMode() {
        if (!devModeEnabled) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Admin promotion is alleen beschikbaar in development mode.");
        }
    }

    @PostMapping("/assign-admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuthUserDto> assignAdmin(
            @Valid @RequestBody AssignLibrarianRequestDto request,
            Authentication authentication) {
        requireDevMode();
        AuthUserDto result = roleService.assignAdmin(authentication, request.userId(), request.schoolCode());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/revoke-admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuthUserDto> revokeAdmin(
            @Valid @RequestBody AssignLibrarianRequestDto request,
            Authentication authentication) {
        requireDevMode();
        AuthUserDto result = roleService.revokeAdmin(authentication, request.userId(), request.schoolCode());
        return ResponseEntity.ok(result);
    }
}
