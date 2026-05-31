package edu.ap.bib_backend;

import edu.ap.bib_backend.jpa.repository.SchoolRepository;
import edu.ap.bib_backend.service.ActiveSchoolContext;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class TestSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    public ActiveSchoolContext activeSchoolContext() {
        return mock(ActiveSchoolContext.class);
    }

    @Bean
    public SchoolRepository schoolRepository() {
        return mock(SchoolRepository.class);
    }
}