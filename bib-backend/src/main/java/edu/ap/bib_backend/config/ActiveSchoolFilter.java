package edu.ap.bib_backend.config;

import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.enums.SchoolStatus;
import edu.ap.bib_backend.jpa.repository.SchoolRepository;
import edu.ap.bib_backend.service.ActiveSchoolContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

@Slf4j
@Component
public class ActiveSchoolFilter extends OncePerRequestFilter {

    private static final String HEADER_ACTIVE_SCHOOL_ID = "X-Active-School-Id";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final ActiveSchoolContext activeSchoolContext;
    private final SchoolRepository schoolRepository;

    public ActiveSchoolFilter(ActiveSchoolContext activeSchoolContext, SchoolRepository schoolRepository) {
        this.activeSchoolContext = activeSchoolContext;
        this.schoolRepository = schoolRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String activeSchoolIdHeader = request.getHeader(HEADER_ACTIVE_SCHOOL_ID);

        if (activeSchoolIdHeader != null && !activeSchoolIdHeader.isBlank()) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null && isAdmin(authentication)) {
                try {
                    Long schoolId = Long.parseLong(activeSchoolIdHeader.trim());
                    School school = schoolRepository.findById(schoolId)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                    "De geselecteerde school is niet beschikbaar."));

                    if (school.getStatus() != SchoolStatus.ACTIVE) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "De geselecteerde school is niet beschikbaar.");
                    }

                    activeSchoolContext.setActiveSchoolId(schoolId);
                    log.debug("Active school override ingesteld voor admin {}: schoolId={}",
                            authentication.getName(), schoolId);
                } catch (NumberFormatException e) {
                    log.warn("Ongeldige waarde voor header {}: {}", HEADER_ACTIVE_SCHOOL_ID, activeSchoolIdHeader);
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Ongeldige schoolidentificatie ontvangen.");
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> ROLE_ADMIN.equals(a.getAuthority()));
    }
}
