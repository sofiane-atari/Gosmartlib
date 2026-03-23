package edu.ap.bib_backend.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import edu.ap.bib_backend.dto.AuthUserDto;
import edu.ap.bib_backend.jpa.entity.Role;

@Service
public class AuthService   {

    public AuthUserDto getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getAuthorities() == null
                || authentication.getAuthorities().isEmpty()) {
            throw new RuntimeException("Geen ingelogde gebruiker gevonden");
        }

        String userId = String.valueOf(authentication.getPrincipal());

        String authority = authentication.getAuthorities()
                .iterator()
                .next()
                .getAuthority();

        Role role = Role.valueOf(authority.replace("ROLE_", ""));

        return new AuthUserDto(userId, role);
    }
}