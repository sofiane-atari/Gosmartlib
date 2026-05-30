package edu.ap.bib_backend.config;

import edu.ap.bib_backend.interceptor.LoggingInterceptor;
import edu.ap.bib_backend.jpa.enums.Role;
import edu.ap.bib_backend.service.smartschool.SmartschoolOAuth2UserService;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

        private final SmartschoolOAuth2UserService smartschoolOAuth2UserService;
        private final JwtAuthenticationFilter jwtAuthenticationFilter;
        private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
        private final RestAccessDeniedHandler restAccessDeniedHandler;
        private final boolean mockModeEnabled;
        private final boolean devModeEnabled;

        public SecurityConfig(
                        JwtAuthenticationFilter jwtAuthenticationFilter,
                        SmartschoolOAuth2UserService smartschoolOAuth2UserService,
                        RestAuthenticationEntryPoint restAuthenticationEntryPoint,
                        RestAccessDeniedHandler restAccessDeniedHandler,
                        @Value("${app.mock-mode-enabled}") boolean mockModeEnabled,
                        @Value("${app.dev-mode-enabled:false}") boolean devModeEnabled) {
                this.jwtAuthenticationFilter = jwtAuthenticationFilter;
                this.smartschoolOAuth2UserService = smartschoolOAuth2UserService;
                this.restAuthenticationEntryPoint = restAuthenticationEntryPoint;
                this.restAccessDeniedHandler = restAccessDeniedHandler;
                this.mockModeEnabled = mockModeEnabled;
                this.devModeEnabled = devModeEnabled;
        }

        @Bean
        public SecurityFilterChain filterChain(
                        HttpSecurity http,
                        OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient,
                        ClientRegistrationRepository clientRegistrationRepository)
                        throws Exception {
                http
                                .csrf(AbstractHttpConfigurer::disable)
                                .cors(Customizer.withDefaults())

                                // Nodig voor OAuth2 login flow (state handling)
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))

                                // JWT filter
                                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                                .authorizeHttpRequests(auth -> {
                                        auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();

                                        auth.requestMatchers(
                                                        "/auth/error",
                                                        "/auth/callback",
                                                        "/auth/initiate",
                                                        "/auth/initiate-login",
                                                        "/oauth2/**",
                                                        "/login/**")
                                                        .permitAll();

                                        if (mockModeEnabled && devModeEnabled) {
                                                auth.requestMatchers(
                                                                "/OAuth/**",
                                                                "/Api/V1/**",
                                                                "/ims/oneroster/**",
                                                                "/mock/schools/**")
                                                                .permitAll();
                                        } else {
                                                auth.requestMatchers(
                                                                "/OAuth/**",
                                                                "/Api/V1/**",
                                                                "/ims/oneroster/**",
                                                                "/mock/schools/**")
                                                                .denyAll();
                                        }

                                        auth.requestMatchers("/auth/me", "/auth/logout", "/auth/refresh",
                                                        "/auth/admin-override/**")
                                                        .authenticated();

                                        auth.requestMatchers("/reviews/**")
                                                        .hasAnyRole(Role.STUDENT.name(), Role.TEACHER.name(),
                                                                        Role.LIBRARIAN.name(), Role.ADMIN.name());

                                        auth.requestMatchers("/books/**")
                                                        .hasAnyRole(Role.STUDENT.name(), Role.TEACHER.name(),
                                                                        Role.LIBRARIAN.name(), Role.ADMIN.name());
                                        auth.requestMatchers(
                                                        "/books/template",
                                                        "/books/upload/**")
                                                        .hasAnyRole(Role.LIBRARIAN.name(), Role.ADMIN.name());

                                        auth.requestMatchers(HttpMethod.POST, "/genres/**")
                                                        .hasRole(Role.ADMIN.name());
                                        auth.requestMatchers(HttpMethod.PUT, "/genres/**")
                                                        .hasRole(Role.ADMIN.name());
                                        auth.requestMatchers(HttpMethod.DELETE, "/genres/**")
                                                        .hasRole(Role.ADMIN.name());
                                        auth.requestMatchers(HttpMethod.GET, "/genres/**")
                                                        .hasAnyRole(Role.STUDENT.name(), Role.TEACHER.name(),
                                                                        Role.LIBRARIAN.name(), Role.ADMIN.name());

                                        auth.requestMatchers(HttpMethod.POST, "/loans/**")
                                                        .hasAnyRole(Role.LIBRARIAN.name(), Role.ADMIN.name());
                                        auth.requestMatchers(HttpMethod.PATCH, "/loans/**")
                                                        .hasAnyRole(Role.LIBRARIAN.name(), Role.ADMIN.name());
                                        auth.requestMatchers(HttpMethod.GET, "/loans/me/**")
                                                        .hasAnyRole(Role.STUDENT.name(), Role.TEACHER.name(),
                                                                        Role.LIBRARIAN.name(), Role.ADMIN.name());
                                        auth.requestMatchers(HttpMethod.GET, "/loans/**")
                                                        .hasAnyRole(Role.LIBRARIAN.name(), Role.ADMIN.name());

                                        auth.requestMatchers("/schools/dev-mode-enabled")
                                                        .permitAll();

                                        auth.requestMatchers(HttpMethod.GET, "/schools/*/catalog-sidebar-settings")
                                                        .hasAnyRole(Role.STUDENT.name(), Role.TEACHER.name(),
                                                                        Role.LIBRARIAN.name(), Role.ADMIN.name());

                                        auth.requestMatchers(HttpMethod.PUT, "/schools/*/catalog-sidebar-settings")
                                                        .hasAnyRole(Role.LIBRARIAN.name(), Role.ADMIN.name());

                                        auth.requestMatchers("/schools/**")
                                                        .hasRole(Role.ADMIN.name());

                                        auth.requestMatchers(HttpMethod.POST, "/campuses/**")
                                                        .hasRole(Role.ADMIN.name());
                                        auth.requestMatchers(HttpMethod.PUT, "/campuses/**")
                                                        .hasRole(Role.ADMIN.name());
                                        auth.requestMatchers(HttpMethod.GET, "/campuses/**")
                                                        .hasAnyRole(Role.STUDENT.name(), Role.TEACHER.name(),
                                                                        Role.LIBRARIAN.name(), Role.ADMIN.name());

                                        auth.requestMatchers("/tags/**")
                                                        .hasAnyRole(Role.LIBRARIAN.name(), Role.ADMIN.name());

                                        auth.requestMatchers("/reports/**")
                                                        .hasAnyRole(Role.TEACHER.name(),
                                                                        Role.LIBRARIAN.name(), Role.ADMIN.name());

                                        auth.requestMatchers("/roles/admin-promotion-enabled")
                                                        .authenticated();

                                        auth.requestMatchers(HttpMethod.POST, "/roles/assign-admin", "/roles/revoke-admin")
                                                        .hasRole(Role.ADMIN.name());

                                        auth.requestMatchers("/roles/**")
                                                        .hasAnyRole(Role.LIBRARIAN.name(), Role.ADMIN.name());

                                        auth.requestMatchers("/users/teachers")
                                                        .hasAnyRole(Role.LIBRARIAN.name(), Role.ADMIN.name());

                                        auth.requestMatchers("/lesson-tips/**")
                                                        .hasAnyRole(Role.TEACHER.name(), Role.LIBRARIAN.name(),
                                                                        Role.ADMIN.name());

                                        auth.requestMatchers("/subscriptions/**")
                                                        .hasAnyRole(Role.STUDENT.name(), Role.TEACHER.name(),
                                                                        Role.LIBRARIAN.name(), Role.ADMIN.name());

                                        auth.requestMatchers("/oneroster/**")
                                                        .hasRole(Role.ADMIN.name());

                                        auth.requestMatchers("/book-copies/**")
                                                        .hasAnyRole(Role.LIBRARIAN.name(), Role.ADMIN.name());

                                        auth.requestMatchers("/reading-levels/**")
                                                        .hasAnyRole(Role.STUDENT.name(), Role.TEACHER.name(),
                                                                        Role.LIBRARIAN.name(), Role.ADMIN.name());

                                        auth.requestMatchers("/reading-lists/**")
                                                        .hasAnyRole(Role.STUDENT.name(), Role.TEACHER.name(),
                                                                        Role.LIBRARIAN.name(), Role.ADMIN.name());

                                        auth.anyRequest().authenticated();
                                })

                                // Zend 401/403 JSON voor API-aanvragen (geen redirect naar OAuth2)
                                .exceptionHandling(exceptions -> exceptions
                                                .authenticationEntryPoint(restAuthenticationEntryPoint)
                                                .accessDeniedHandler(restAccessDeniedHandler))

                                // OAuth2 login (Smartschool)
                                .oauth2Login(oauth2 -> oauth2
                                                .authorizationEndpoint(auth -> auth
                                                                .authorizationRequestResolver(
                                                                                new SchoolAwareAuthorizationRequestResolver(
                                                                                                clientRegistrationRepository)))
                                                .tokenEndpoint(token -> token
                                                                .accessTokenResponseClient(accessTokenResponseClient))
                                                .userInfoEndpoint(userInfo -> userInfo
                                                                .userService(smartschoolOAuth2UserService))
                                                .defaultSuccessUrl("/auth/callback", true)
                                                .failureUrl("/auth/error"));

                return http.build();
        }

        @Bean
        public OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient() {

                OAuth2AccessTokenResponseHttpMessageConverter tokenConverter = new OAuth2AccessTokenResponseHttpMessageConverter();

                // Smartschool returns text/html
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

                RestClientAuthorizationCodeTokenResponseClient client = new RestClientAuthorizationCodeTokenResponseClient();
                RestClient restClient = RestClient.builder(restTemplate).build();
                client.setRestClient(restClient);

                return client;
        }
}
