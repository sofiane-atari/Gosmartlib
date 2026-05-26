package edu.ap.bib_backend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
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
public class RoleController {

    private final RoleService roleService;
    private final boolean adminPromotionEnabled;

    public RoleController(
            RoleService roleService,
            @Value("${app.admin-promotion-enabled:false}") boolean adminPromotionEnabled) {
        this.roleService = roleService;
        this.adminPromotionEnabled = adminPromotionEnabled;
    }

    @GetMapping("/admin-promotion-enabled")
    public ResponseEntity<AdminPromotionResponse> isAdminPromotionEnabled() {
        return ResponseEntity.ok(new AdminPromotionResponse(adminPromotionEnabled));
    }

    @PostMapping("/assign-librarian")
    public ResponseEntity<AuthUserDto> assignLibrarian(
            @Valid @RequestBody AssignLibrarianRequestDto request,
            Authentication authentication) {
        AuthUserDto result = roleService.assignLibrarian(authentication, request.userId(), request.schoolCode());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/revoke-librarian")
    public ResponseEntity<AuthUserDto> revokeLibrarian(
            @Valid @RequestBody AssignLibrarianRequestDto request,
            Authentication authentication) {
        AuthUserDto result = roleService.revokeLibrarian(authentication, request.userId(), request.schoolCode());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/assign-admin")
    public ResponseEntity<AuthUserDto> assignAdmin(
            @Valid @RequestBody AssignLibrarianRequestDto request,
            Authentication authentication) {
        if (!adminPromotionEnabled) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Promoveren tot admin is niet toegestaan.");
        }
        AuthUserDto result = roleService.assignAdmin(authentication, request.userId(), request.schoolCode());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/revoke-admin")
    public ResponseEntity<AuthUserDto> revokeAdmin(
            @Valid @RequestBody AssignLibrarianRequestDto request,
            Authentication authentication) {
        if (!adminPromotionEnabled) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Intrekken van adminrol is niet toegestaan.");
        }
        AuthUserDto result = roleService.revokeAdmin(authentication, request.userId(), request.schoolCode());
        return ResponseEntity.ok(result);
    }

    public record AdminPromotionResponse(boolean enabled) {
    }
}
