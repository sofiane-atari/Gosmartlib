package edu.ap.bib_backend.service.user;

import edu.ap.bib_backend.jpa.entity.Campus;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
public class CurrentUserService {

    private static final String ROLE_TEACHER = "ROLE_TEACHER";
    private static final String ROLE_LIBRARIAN = "ROLE_LIBRARIAN";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String extractCurrentUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new IllegalStateException("Geen geauthenticeerde gebruiker gevonden.");
        }

        return authentication.getName();
    }

    public User getCurrentUser(Authentication authentication) {
        String userId = extractCurrentUserId(authentication);

        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Gebruiker niet gevonden: " + userId));
    }

    public Campus getCurrentUserCampus(Authentication authentication) {
        User user = getCurrentUser(authentication);

        if (user.getCampus() == null) {
            throw new IllegalStateException("Gebruiker heeft geen campus gekoppeld.");
        }

        return user.getCampus();
    }

    public School getCurrentUserSchool(Authentication authentication) {
        Campus campus = getCurrentUserCampus(authentication);

        if (campus.getSchool() == null) {
            throw new IllegalStateException("Campus heeft geen school gekoppeld.");
        }

        return campus.getSchool();
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
                .anyMatch(authority -> Arrays.asList(allowedAuthorities).contains(authority.getAuthority()));
    }
}