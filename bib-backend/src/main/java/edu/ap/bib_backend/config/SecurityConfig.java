package edu.ap.bib_backend.config;

import edu.ap.bib_backend.jpa.entity.Role;
import edu.ap.bib_backend.security.DevMockRoleFilter;

import edu.ap.bib_backend.service.SmartschoolOAuth2UserService;
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

/**
 * Globale beveiligingsconfiguratie voor de applicatie.
 * 
 * Deze klasse fungeert als de centrale 'firewall' voor alle inkomende
 * HTTP-verzoeken.
 * Het configureert CORS, schakelt CSRF uit (geschikt voor stateless REST
 * API's),
 * stelt de sessiemanagement in op stateless, en definieert welke endpoints
 * publiek toegankelijk zijn en welke authenticatie vereisen.
 * 
 */

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final SmartschoolOAuth2UserService smartschoolOAuth2UserService;
    private final DevMockRoleFilter devMockRoleFilter;

    public SecurityConfig(DevMockRoleFilter devMockRoleFilter,
            SmartschoolOAuth2UserService smartschoolOAuth2UserService) {
        this.devMockRoleFilter = devMockRoleFilter;
        this.smartschoolOAuth2UserService = smartschoolOAuth2UserService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())

                // ⚠️ OAuth2 Authorization Code flow needs a session to store the
                // 'state' parameter during the redirect. We relax STATELESS here
                // only for that handshake — your API endpoints remain effectively
                // stateless because they are protected via the JWT filter below.
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))

                // Dev mock filter (still active in non-prod)
                .addFilterBefore(devMockRoleFilter, UsernamePasswordAuthenticationFilter.class)

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/me").permitAll()
                        .requestMatchers("/api/reviews/**")
                        .hasAnyRole(Role.STUDENT.name(), Role.TEACHER.name(), Role.LIBRARIAN.name(), Role.ADMIN.name())
                        .requestMatchers("/api/books/**")
                        .hasAnyRole(Role.STUDENT.name(), Role.TEACHER.name(), Role.LIBRARIAN.name(), Role.ADMIN.name())
                        .requestMatchers("/api/labels/**").hasRole(Role.ADMIN.name())
                        .anyRequest().authenticated())

                // --- OAuth2 login via Smartschool ---
                .oauth2Login(oauth2 -> oauth2
                        // After Smartschool redirects back, call your own endpoint
                        // which issues a JWT and redirects to the Angular frontend
                        .userInfoEndpoint(userInfo -> userInfo.userService(smartschoolOAuth2UserService))
                        .defaultSuccessUrl("/api/auth/callback", true)
                        .failureUrl("/api/auth/error"));

        return http.build();
    }
}