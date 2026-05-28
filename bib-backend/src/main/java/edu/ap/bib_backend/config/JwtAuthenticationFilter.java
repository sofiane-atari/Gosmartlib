package edu.ap.bib_backend.config;

import edu.ap.bib_backend.service.AdminOverrideService;
import edu.ap.bib_backend.service.JwtService;
import edu.ap.bib_backend.service.TokenStorageService;
import edu.ap.bib_backend.service.smartschool.SmartschoolOAuth2UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AdminOverrideService adminOverrideService;
    private final TokenStorageService tokenStorageService;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            AdminOverrideService adminOverrideService,
            TokenStorageService tokenStorageService
    ) {
        this.jwtService = jwtService;
        this.adminOverrideService = adminOverrideService;
        this.tokenStorageService = tokenStorageService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        if (!jwtService.isValid(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write(
                "{\"error\":\"Unauthorized\",\"message\":\"Token is verlopen of ongeldig\",\"code\":\"TOKEN_INVALID\"}");
            return;
        }

        String username = jwtService.extractUsername(token);

        String tokenType = (String) jwtService.extractClaim(token, "type");
        if ("oneroster".equals(tokenType)) {
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    username,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_ONEROSTER_CLIENT"))
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
            return;
        }

        if (!tokenStorageService.isCurrentToken(username, token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write(
                "{\"error\":\"Unauthorized\",\"message\":\"Token is ingetrokken. Opnieuw aanmelden vereist\",\"code\":\"TOKEN_INVALID\"}");
            return;
        }

        String role = (String) jwtService.extractClaim(token, "roles");

        if (adminOverrideService.isAdminOverrideActive(username)) {
            role = "ROLE_ADMIN";
        }

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                username,
                null,
                List.of(new SimpleGrantedAuthority(role))
        );

        Map<String, Object> details = new HashMap<>();
        details.put("classCode", asStringOrDefault(
                jwtService.extractClaim(token, SmartschoolOAuth2UserService.CLAIM_CLASS_CODE),
                "GENERAL"
        ));
        details.put("yearCode", asStringOrDefault(
                jwtService.extractClaim(token, SmartschoolOAuth2UserService.CLAIM_YEAR_CODE),
                "0"
        ));
        details.put("schoolCode", asStringOrDefault(
                jwtService.extractClaim(token, SmartschoolOAuth2UserService.CLAIM_SCHOOL_CODE),
                "ALLEMAAL"
        ));
        details.put("schoolId", jwtService.extractClaim(token, "schoolId"));
        details.put("campusId", jwtService.extractClaim(token, "campusId"));
        details.put("name", jwtService.extractClaim(token, "name"));
        details.put("surname", jwtService.extractClaim(token, "surname"));
        details.put("email", jwtService.extractClaim(token, "email"));
        details.put("platform", jwtService.extractClaim(token, "platform"));
        details.put("primaryGroupName", jwtService.extractClaim(token, "primaryGroupName"));
        details.put("parentGroupNames", jwtService.extractClaim(token, "parentGroupNames"));

        authentication.setDetails(details);

        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

    private String asStringOrDefault(Object value, String fallback) {
        if (value instanceof String str && !str.isBlank()) {
            return str;
        }
        return fallback;
    }
}