package edu.ap.bib_backend.config;

import edu.ap.bib_backend.interceptor.LoggingInterceptor;
import edu.ap.bib_backend.jpa.enums.Role;
import edu.ap.bib_backend.service.SmartschoolOAuth2UserService;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final SmartschoolOAuth2UserService smartschoolOAuth2UserService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            SmartschoolOAuth2UserService smartschoolOAuth2UserService
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.smartschoolOAuth2UserService = smartschoolOAuth2UserService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())

                // Nodig voor OAuth2 login flow (state handling)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))

                // JWT filter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                .authorizeHttpRequests(auth -> auth
                        // Preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 🔓 Public endpoints
                        .requestMatchers(
                                "/auth/error",
                                "/auth/callback",
                                "/oauth2/**",
                                "/login/**"
                        ).permitAll()

                        // 🔓 Tijdelijke / dev endpoints (optioneel)
                        .requestMatchers(
                                "/books/template",
                                "/books/upload/validate-stream/**"
                        ).permitAll()

                        // 🔐 Auth endpoints
                        .requestMatchers("/auth/me", "/auth/logout").authenticated()

                        // 🔐 Business endpoints
                        .requestMatchers("/reviews/**")
                        .hasAnyRole(Role.STUDENT.name(), Role.TEACHER.name(),
                                Role.LIBRARIAN.name(), Role.ADMIN.name())

                        .requestMatchers("/books/**")
                        .hasAnyRole(Role.STUDENT.name(), Role.TEACHER.name(),
                                Role.LIBRARIAN.name(), Role.ADMIN.name())

                        .requestMatchers("/genres/**")
                        .hasAnyRole(Role.STUDENT.name(), Role.ADMIN.name())

                        .anyRequest().authenticated()
                )

                // OAuth2 login (Smartschool)
                .oauth2Login(oauth2 -> oauth2
                        .tokenEndpoint(token -> token
                                .accessTokenResponseClient(accessTokenResponseClient()))
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(smartschoolOAuth2UserService))
                        .defaultSuccessUrl("/auth/callback", true)
                        .failureUrl("/auth/error")
                );

        return http.build();
    }

    @Bean
    public OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient() {

        OAuth2AccessTokenResponseHttpMessageConverter tokenConverter =
                new OAuth2AccessTokenResponseHttpMessageConverter();

        // Smartschool returns text/html
        tokenConverter.setSupportedMediaTypes(List.of(
                MediaType.APPLICATION_JSON,
                MediaType.APPLICATION_FORM_URLENCODED,
                new MediaType("text", "html")
        ));

        List<HttpMessageConverter<?>> converters = new ArrayList<>();
        converters.add(new FormHttpMessageConverter());
        converters.add(tokenConverter);

        RestTemplate restTemplate = new RestTemplate(converters);

        restTemplate.setRequestFactory(
                new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory()));

        restTemplate.setInterceptors(List.of(new LoggingInterceptor()));
        restTemplate.setErrorHandler(new OAuth2ErrorResponseErrorHandler());

        DefaultAuthorizationCodeTokenResponseClient client =
                new DefaultAuthorizationCodeTokenResponseClient();

        client.setRestOperations(restTemplate);

        return client;
    }
}