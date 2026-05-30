package edu.ap.bib_backend.service.report;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
public class ReportAccessService {

    /**
     * Requires a non-null authenticated principal.
     *
     * @param authentication current authentication
     */
    public void requireAuthenticated(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(
                    UNAUTHORIZED,
                    "Je moet ingelogd zijn om klasrapporten te bekijken");
        }
    }

    /**
     * Requires a role allowed to view class reports.
     *
     * @param authentication current authentication
     */
    public void requireReportAccess(Authentication authentication) {
        authentication.getAuthorities()
                .forEach(authority -> System.out.println("REPORT AUTHORITY = " + authority.getAuthority()));

        boolean allowed = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equalsIgnoreCase("ROLE_TEACHER")
                        || role.equalsIgnoreCase("ROLE_LIBRARIAN")
                        || role.equalsIgnoreCase("ROLE_ADMIN"));

        if (!allowed) {
            throw new ResponseStatusException(
                    FORBIDDEN,
                    "Alleen leerkrachten en bibliothecarissen kunnen klasrapporten bekijken");
        }
    }

    /**
     * Requires access to the requested school or admin privileges.
     *
     * @param authentication current authentication
     * @param userSchoolId school id linked to the current user
     * @param requestedSchoolId requested school id, or null for unrestricted access
     */
    public void requireSameSchoolOrAdmin(Authentication authentication, Long userSchoolId, Long requestedSchoolId) {
        if (requestedSchoolId == null || requestedSchoolId.equals(userSchoolId) || hasRole(authentication, "ROLE_ADMIN")) {
            return;
        }

        throw new ResponseStatusException(
                FORBIDDEN,
                "Je hebt geen toegang tot rapporten van deze school");
    }

    /**
     * Requires administrator privileges.
     *
     * @param authentication current authentication
     */
    public void requireAdmin(Authentication authentication) {
        if (hasRole(authentication, "ROLE_ADMIN")) {
            return;
        }

        throw new ResponseStatusException(
                FORBIDDEN,
                "Alleen administrators kunnen systeemrapporten bekijken");
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equalsIgnoreCase(role));
    }
}
