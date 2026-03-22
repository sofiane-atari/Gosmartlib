package edu.ap.bib_backend.config;

import edu.ap.bib_backend.interceptor.LoggingInterceptor;
import edu.ap.bib_backend.jpa.entity.Role;
import edu.ap.bib_backend.security.DevMockRoleFilter;

import edu.ap.bib_backend.service.SmartschoolOAuth2UserService;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
import org.springframework.http.MediaType;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

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
                                                .requestMatchers("/auth/me").permitAll()
                                                .requestMatchers("/reviews/**")
                                                .hasAnyRole(Role.STUDENT.name(), Role.TEACHER.name(),
                                                                Role.LIBRARIAN.name(), Role.ADMIN.name())
                                                .requestMatchers("/books/**")
                                                .hasAnyRole(Role.STUDENT.name(), Role.TEACHER.name(),
                                                                Role.LIBRARIAN.name(), Role.ADMIN.name())
                                                .requestMatchers("/genres/**")
                                                .hasAnyRole(Role.STUDENT.name(), Role.ADMIN.name())
                                                .anyRequest().authenticated())

                                // --- OAuth2 login via Smartschool ---
                                .oauth2Login(oauth2 -> oauth2
                                                // After Smartschool redirects back, call your own endpoint
                                                // which issues a JWT and redirects to the Angular frontend
                                                .tokenEndpoint(token -> token
                                                                .accessTokenResponseClient(accessTokenResponseClient()))
                                                .userInfoEndpoint(userInfo -> userInfo
                                                                .userService(smartschoolOAuth2UserService))
                                                .defaultSuccessUrl("/auth/callback", true)
                                                .failureUrl("/auth/error"));

                return http.build();
        }

        @Bean
        public OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient() {
                OAuth2AccessTokenResponseHttpMessageConverter tokenConverter = new OAuth2AccessTokenResponseHttpMessageConverter();

                // Smartschool returns text/html — add it to accepted types
                tokenConverter.setSupportedMediaTypes(List.of(
                                MediaType.APPLICATION_JSON,
                                MediaType.APPLICATION_FORM_URLENCODED,
                                new MediaType("text", "html")));

                List<HttpMessageConverter<?>> converters = new ArrayList<>();
                converters.add(new FormHttpMessageConverter());
                converters.add(tokenConverter);

                RestTemplate restTemplate = new RestTemplate(converters);

                restTemplate.setRequestFactory(
                                new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory()));
                restTemplate.setInterceptors(List.of(new LoggingInterceptor()));

                restTemplate.setErrorHandler(new OAuth2ErrorResponseErrorHandler());

                DefaultAuthorizationCodeTokenResponseClient client = new DefaultAuthorizationCodeTokenResponseClient();
                client.setRestOperations(restTemplate);

                return client;
        }
}