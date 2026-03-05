package edu.ap.bib_backend.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

@Component
public class DevMockRoleFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // The Angular frontend will send this header
        String mockRole = request.getHeader("X-Mock-Role");

        if (mockRole != null && !mockRole.trim().isEmpty()) {
            // Spring Security requires the "ROLE_" prefix for authorities
            SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + mockRole.toUpperCase());
            
            // Create a mock authentication object and set it in the Security Context
            UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken("MockUser", null, Collections.singletonList(authority));
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }


        filterChain.doFilter(request, response);
    }
}