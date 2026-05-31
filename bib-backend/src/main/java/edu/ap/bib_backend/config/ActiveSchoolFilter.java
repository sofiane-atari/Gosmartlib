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
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class ActiveSchoolFilter extends OncePerRequestFilter {

    private static final String HEADER_ACTIVE_SCHOOL_ID = "X-Active-School-Id";
    private static final String HEADER_ACTIVE_CLASS_CODES = "X-Active-Class-Codes";
    private static final String HEADER_ACTIVE_CLASS_NAMES = "X-Active-Class-Names";
    private static final String HEADER_ACTIVE_PRIMARY_GROUPS = "X-Active-Primary-Group-Names";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_LIBRARIAN = "ROLE_LIBRARIAN";

    private final ActiveSchoolContext activeSchoolContext;
    private final SchoolRepository schoolRepository;

    public ActiveSchoolFilter(ActiveSchoolContext activeSchoolContext, SchoolRepository schoolRepository) {
        this.activeSchoolContext = activeSchoolContext;
        this.schoolRepository = schoolRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
            processSchoolOverride(request, authentication);
            processClassOverride(request, authentication);
        }

        filterChain.doFilter(request, response);
    }

    private void processSchoolOverride(HttpServletRequest request, Authentication authentication) {
        String activeSchoolIdHeader = request.getHeader(HEADER_ACTIVE_SCHOOL_ID);

        if (activeSchoolIdHeader == null || activeSchoolIdHeader.isBlank()) {
            return;
        }

        if (!isAdmin(authentication)) {
            return;
        }

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
            activeSchoolContext.setActiveSchoolDomainName(school.getDomainName());
            log.debug("Active school override ingesteld voor admin {}: schoolId={}",
                    authentication.getName(), schoolId);
        } catch (NumberFormatException e) {
            log.warn("Ongeldige waarde voor header {}: {}", HEADER_ACTIVE_SCHOOL_ID, activeSchoolIdHeader);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ongeldige schoolidentificatie ontvangen.");
        }
    }

    private void processClassOverride(HttpServletRequest request, Authentication authentication) {
        List<String> classCodes = parseCommaSeparatedHeader(request, HEADER_ACTIVE_CLASS_CODES);
        List<String> classNames = parseCommaSeparatedHeader(request, HEADER_ACTIVE_CLASS_NAMES);
        List<String> primaryGroups = parseCommaSeparatedHeader(request, HEADER_ACTIVE_PRIMARY_GROUPS);

        if (classCodes.isEmpty() && classNames.isEmpty() && primaryGroups.isEmpty()) {
            return;
        }

        if (!isAdminOrLibrarian(authentication)) {
            return;
        }

        activeSchoolContext.setActiveClassCodes(classCodes);
        activeSchoolContext.setActiveClassNames(classNames);
        activeSchoolContext.setActivePrimaryGroupNames(primaryGroups);

        overrideDetailsMap(authentication);
    }

    private List<String> parseCommaSeparatedHeader(HttpServletRequest request, String headerName) {
        String raw = request.getHeader(headerName);
        if (raw == null || raw.isBlank()) {
            return new ArrayList<>();
        }
        List<String> result = new ArrayList<>();
        for (String part : raw.split(",")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) continue;
            try {
                result.add(URLDecoder.decode(trimmed, StandardCharsets.UTF_8));
            } catch (IllegalArgumentException e) {
                result.add(trimmed);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private void overrideDetailsMap(Authentication authentication) {
        Object details = authentication.getDetails();
        if (!(details instanceof Map)) {
            return;
        }

        Map<String, Object> detailsMap = (Map<String, Object>) details;

        if (activeSchoolContext.getActiveSchoolDomainName() != null) {
            detailsMap.put("schoolCode", activeSchoolContext.getActiveSchoolDomainName());
        }

        List<String> classCodes = activeSchoolContext.getActiveClassCodes();
        if (classCodes != null && !classCodes.isEmpty()) {
            detailsMap.put("classCode", classCodes.get(0));
            detailsMap.put("classCodes", new ArrayList<>(classCodes));
        }

        List<String> primaryGroups = activeSchoolContext.getActivePrimaryGroupNames();
        if (primaryGroups != null && !primaryGroups.isEmpty()) {
            detailsMap.put("primaryGroupName", primaryGroups.get(0));
        }
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> ROLE_ADMIN.equals(a.getAuthority()));
    }

    private boolean isAdminOrLibrarian(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> ROLE_ADMIN.equals(a.getAuthority()) || ROLE_LIBRARIAN.equals(a.getAuthority()));
    }
}
