package edu.ap.bib_backend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.ap.bib_backend.dto.AssignLibrarianRequestDto;
import edu.ap.bib_backend.dto.AuthUserDto;
import edu.ap.bib_backend.service.RoleService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/roles")
public class RoleController {

    private final RoleService roleService;
    private final boolean devModeEnabled;

    public RoleController(
            RoleService roleService,
            @Value("${app.dev-mode-enabled:false}") boolean devModeEnabled) {
        this.roleService = roleService;
        this.devModeEnabled = devModeEnabled;
    }

    @GetMapping("/admin-promotion-enabled")
    public ResponseEntity<AdminPromotionResponse> isAdminPromotionEnabled() {
        return ResponseEntity.ok(new AdminPromotionResponse(devModeEnabled));
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

    public record AdminPromotionResponse(boolean enabled) {
    }
}
