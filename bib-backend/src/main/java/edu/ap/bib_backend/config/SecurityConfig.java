package edu.ap.bib_backend.config;

import edu.ap.bib_backend.security.DevMockRoleFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final DevMockRoleFilter devMockRoleFilter;

    public SecurityConfig(DevMockRoleFilter devMockRoleFilter) {
        this.devMockRoleFilter = devMockRoleFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            // Gebruik globale corsConfig
            .cors(Customizer.withDefaults()) 
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(devMockRoleFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                // Options preflight doorlaten
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() 
                // Test endpoint
                .requestMatchers("/api/auth/me").permitAll()
                .anyRequest().authenticated()
            );

        return http.build();
    }
}