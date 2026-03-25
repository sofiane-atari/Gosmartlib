package edu.ap.bib_backend.config;

import edu.ap.bib_backend.service.JwtService;
import edu.ap.bib_backend.service.SmartschoolOAuth2UserService;
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

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        if (!jwtService.isValid(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        String username = jwtService.extractUsername(token);
        String role = (String) jwtService.extractClaim(token, "roles");

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        List.of(new SimpleGrantedAuthority(role))
                );

        Map<String, Object> audienceDetails = new HashMap<>();
        audienceDetails.put("classCode", asStringOrDefault(jwtService.extractClaim(token, SmartschoolOAuth2UserService.CLAIM_CLASS_CODE), "GENERAL"));
        audienceDetails.put("yearCode", asStringOrDefault(jwtService.extractClaim(token, SmartschoolOAuth2UserService.CLAIM_YEAR_CODE), "0"));
        audienceDetails.put("schoolCode", asStringOrDefault(jwtService.extractClaim(token, SmartschoolOAuth2UserService.CLAIM_SCHOOL_CODE), "ALLEMAAL"));
        authentication.setDetails(audienceDetails);

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
