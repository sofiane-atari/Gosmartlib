package edu.ap.bib_backend.service.user;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    private static final String ROLE_TEACHER = "ROLE_TEACHER";
    private static final String ROLE_LIBRARIAN = "ROLE_LIBRARIAN";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    public String extractCurrentUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new IllegalStateException("Geen geauthenticeerde gebruiker gevonden.");
        }

        return authentication.getName();
    }

    public boolean isModerator(Authentication authentication) {
        return hasAnyAuthority(authentication, ROLE_LIBRARIAN, ROLE_ADMIN);
    }

    public boolean canSeeDidacticBooks(Authentication authentication) {
        return hasAnyAuthority(authentication, ROLE_TEACHER, ROLE_LIBRARIAN, ROLE_ADMIN);
    }

    private boolean hasAnyAuthority(Authentication authentication, String... allowedAuthorities) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .anyMatch(authority -> java.util.Arrays.asList(allowedAuthorities).contains(authority.getAuthority()));
    }
}