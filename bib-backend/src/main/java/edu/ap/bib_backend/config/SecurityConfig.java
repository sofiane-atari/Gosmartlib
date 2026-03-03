package edu.ap.bib_backend.config;

import edu.ap.bib_backend.security.DevMockRoleFilter; // Let op: controleer of dit pad klopt met jouw mappenstructuur!
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

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
            .csrf(AbstractHttpConfigurer::disable) // CSRF uitschakelen voor REST API's
            .cors(cors -> cors.configurationSource(corsConfigurationSource())) // 1. CORS AANZETTEN!
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Voeg onze custom filter toe vóór de standaard authenticatie
            .addFilterBefore(devMockRoleFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                // 2. Laat de onzichtbare CORS 'OPTIONS' test-verzoeken van Angular ALTIJD door
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() 
                // 3. Zet deze test-endpoints open zodat Angular erbij kan
                .requestMatchers("/api/auth/me").permitAll() 
                // Alle andere endpoints vereisen dat je ingelogd bent (dus dat de X-Mock-Role is meegestuurd)
                .anyRequest().authenticated()
            );

        return http.build();
    }

    // Cors Regels
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Veilige afzender
        configuration.setAllowedOrigins(List.of("http://localhost:4200"));
        
        // Sta alle HTTP methodes toe
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        
        // Sta ALLE headers toe ( 'X-Mock-Role' )
        configuration.setAllowedHeaders(List.of("*"));
        
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Pas deze veilige regels toe op ALLE url's van onze backend (/**)
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}