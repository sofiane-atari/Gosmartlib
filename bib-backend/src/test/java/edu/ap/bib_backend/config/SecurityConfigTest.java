package edu.ap.bib_backend.config;

import edu.ap.bib_backend.service.AdminOverrideService;
import edu.ap.bib_backend.service.JwtService;
import edu.ap.bib_backend.service.TokenStorageService;
import edu.ap.bib_backend.service.smartschool.SmartschoolOAuth2UserService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SecurityConfigTestController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class
})
@TestPropertySource(properties = {
        "frontend.redirect-url=http://localhost:4200/login"
})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SecurityConfig securityConfig;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private TokenStorageService tokenStorageService;

    @MockitoBean
    private AdminOverrideService adminOverrideService;

    @MockitoBean
    private SmartschoolOAuth2UserService smartschoolOAuth2UserService;

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    private OAuth2AuthorizedClientService authorizedClientService;

    @Test
    void givenNoAuthentication_whenGetAuthMeEndpoint_thenReturnRedirectToOAuthLogin() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void givenNoAuthentication_whenGetBooksEndpoint_thenReturnRedirectToOAuthLogin() throws Exception {
        mockMvc.perform(get("/books/test"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void givenStudentRoleAuthentication_whenGetBooksEndpoint_thenReturnOk() throws Exception {
        mockMvc.perform(get("/books/test")
                .with(user("student-user").roles("STUDENT")))
                .andExpect(status().isOk());
    }

    @Test
    void givenAdminRoleAuthentication_whenGetGenresEndpoint_thenReturnOk() throws Exception {
        mockMvc.perform(get("/genres/test")
                .with(user("admin-user").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void givenSecurityConfig_whenAccessTokenResponseClientCreated_thenReturnClientInstance() {
        OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> client = securityConfig
                .accessTokenResponseClient();

        assertNotNull(client);
    }
}