package edu.ap.bib_backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @PostMapping("/assign-librarian")
    public ResponseEntity<AuthUserDto> assignLibrarian(
            @Valid @RequestBody AssignLibrarianRequestDto request,
            Authentication authentication) {
        AuthUserDto result = roleService.assignLibrarian(authentication, request.userId());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/revoke-librarian")
    public ResponseEntity<AuthUserDto> revokeLibrarian(
            @Valid @RequestBody AssignLibrarianRequestDto request,
            Authentication authentication) {
        AuthUserDto result = roleService.revokeLibrarian(authentication, request.userId());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/assign-admin")
    public ResponseEntity<AuthUserDto> assignAdmin(
            @Valid @RequestBody AssignLibrarianRequestDto request,
            Authentication authentication) {
        AuthUserDto result = roleService.assignAdmin(authentication, request.userId());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/revoke-admin")
    public ResponseEntity<AuthUserDto> revokeAdmin(
            @Valid @RequestBody AssignLibrarianRequestDto request,
            Authentication authentication) {
        AuthUserDto result = roleService.revokeAdmin(authentication, request.userId());
        return ResponseEntity.ok(result);
    }
}
