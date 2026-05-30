package edu.ap.bib_backend.service.user;

import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.repository.SchoolRepository;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import edu.ap.bib_backend.service.ActiveSchoolContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
public class CurrentUserService {

    private static final String ROLE_TEACHER = "ROLE_TEACHER";
    private static final String ROLE_LIBRARIAN = "ROLE_LIBRARIAN";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final ActiveSchoolContext activeSchoolContext;

    public CurrentUserService(UserRepository userRepository,
                              SchoolRepository schoolRepository,
                              ActiveSchoolContext activeSchoolContext) {
        this.userRepository = userRepository;
        this.schoolRepository = schoolRepository;
        this.activeSchoolContext = activeSchoolContext;
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

    public School getCurrentUserSchool(Authentication authentication) {
        if (isAdmin(authentication) && activeSchoolContext.getActiveSchoolId() != null) {
            return schoolRepository.findById(activeSchoolContext.getActiveSchoolId())
                    .orElseThrow(() -> new IllegalStateException("Geselecteerde school niet gevonden."));
        }

        User user = getCurrentUser(authentication);

        if (user.getSchool() == null) {
            throw new IllegalStateException("Gebruiker heeft geen school gekoppeld.");
        }

        return user.getSchool();
    }

    public boolean isModerator(Authentication authentication) {
        return hasAnyAuthority(authentication, ROLE_LIBRARIAN, ROLE_ADMIN);
    }

    public boolean canSeeDidacticBooks(Authentication authentication) {
        return hasAnyAuthority(authentication, ROLE_TEACHER, ROLE_LIBRARIAN, ROLE_ADMIN);
    }

    public boolean currentUserHasAnyAuthority(String... allowedAuthorities) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        return hasAnyAuthority(authentication, allowedAuthorities);
    }

    private boolean isAdmin(Authentication authentication) {
        return hasAnyAuthority(authentication, ROLE_ADMIN);
    }

    private boolean hasAnyAuthority(Authentication authentication, String... allowedAuthorities) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .anyMatch(authority -> Arrays.asList(allowedAuthorities).contains(authority.getAuthority()));
    }
}
