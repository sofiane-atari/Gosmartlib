package edu.ap.bib_backend.service.user;

import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.repository.SchoolRepository;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import edu.ap.bib_backend.exception.ResourceNotFoundException;

import org.springframework.security.access.AccessDeniedException;
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

    public CurrentUserService(UserRepository userRepository, SchoolRepository schoolRepository) {
        this.userRepository = userRepository;
        this.schoolRepository = schoolRepository;
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
        User user = getCurrentUser(authentication);

        if (user.getSchool() == null) {
            throw new IllegalStateException("Gebruiker heeft geen school gekoppeld.");
        }

        return user.getSchool();
    }

    public boolean isAdmin(Authentication authentication) {
        return hasAnyAuthority(authentication, ROLE_ADMIN);
    }

    public boolean isModerator(Authentication authentication) {
        return hasAnyAuthority(authentication, ROLE_LIBRARIAN, ROLE_ADMIN);
    }

    public boolean canSeeDidacticBooks(Authentication authentication) {
        return hasAnyAuthority(authentication, ROLE_TEACHER, ROLE_LIBRARIAN, ROLE_ADMIN);
    }

    public boolean canSeeOtherSchools(Authentication authentication) {
        return hasAnyAuthority(authentication, ROLE_TEACHER, ROLE_LIBRARIAN, ROLE_ADMIN);
    }

    public School resolveSchool(Long schoolId, Authentication authentication) {
        School userSchool = getCurrentUserSchool(authentication);

        if (schoolId == null || schoolId.equals(userSchool.getId())) {
            return userSchool;
        }

        if (!canSeeOtherSchools(authentication)) {
            throw new AccessDeniedException("Je hebt geen toestemming om gegevens van andere scholen te bekijken.");
        }

        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("schoolId", "School niet gevonden."));

        if (!school.getCatalogVisible()) {
            throw new AccessDeniedException("De catalogus van deze school is niet zichtbaar.");
        }

        return school;
    }

    public boolean currentUserHasAnyAuthority(String... allowedAuthorities) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        return hasAnyAuthority(authentication, allowedAuthorities);
    }

    private boolean hasAnyAuthority(Authentication authentication, String... allowedAuthorities) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .anyMatch(authority -> Arrays.asList(allowedAuthorities).contains(authority.getAuthority()));
    }
}
