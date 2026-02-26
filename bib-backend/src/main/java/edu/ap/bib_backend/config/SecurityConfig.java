package edu.ap.bib_backend.config;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import edu.ap.bib_backend.security.DevMockRoleFilter;

@Configuration
@EnableMethodSecurity // To use @preAuthorized
public class SecurityConfig {

    private final DevMockRoleFilter devMockRoleFilter;

    // Inject the filter optionally. It will be null in the "prod" profile because the bean won't exist.
    public SecurityConfig(@Autowired(required = false) DevMockRoleFilter devMockRoleFilter) {
        this.devMockRoleFilter = devMockRoleFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable);

        // If we are in dev mode, add our custom mock filter
        if (devMockRoleFilter != null) {
            http.addFilterBefore(devMockRoleFilter, UsernamePasswordAuthenticationFilter.class);
        }

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/me").permitAll() // Allow unauthenticated access to fetch current user state
                .anyRequest().authenticated()                // Secure all other endpoints
        );

        return http.build();
    }
}