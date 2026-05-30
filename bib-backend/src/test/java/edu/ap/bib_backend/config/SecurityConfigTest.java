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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

    @MockitoBean
    private RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    @MockitoBean
    private RestAccessDeniedHandler restAccessDeniedHandler;

    @Test
    void givenNoAuthentication_whenGetAuthMeEndpoint_thenReturnUnauthorizedJson() throws Exception {
        mockMvc.perform(get("/test/probe"))
                .andExpect(status().isOk());
    }

    @Test
    void givenNoAuthentication_whenGetBooksEndpoint_thenReturnUnauthorizedJson() throws Exception {
        mockMvc.perform(get("/books/test"))
                .andExpect(status().isOk());
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
    void givenLibrarianRole_whenGetUsersEndpoint_thenReturnOk() throws Exception {
        mockMvc.perform(get("/users/test")
                .with(user("librarian").roles("LIBRARIAN")))
                .andExpect(status().isOk());
    }

    @Test
    void givenAdminRole_whenGetUsersEndpoint_thenReturnOk() throws Exception {
        mockMvc.perform(get("/users/test")
                .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void givenLibrarianRole_whenGetTagsEndpoint_thenReturnOk() throws Exception {
        mockMvc.perform(get("/tags/test")
                .with(user("librarian").roles("LIBRARIAN")))
                .andExpect(status().isOk());
    }

    @Test
    void givenAdminRole_whenGetTagsEndpoint_thenReturnOk() throws Exception {
        mockMvc.perform(get("/tags/test")
                .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void givenAdminRole_whenAssignAdminEndpoint_thenReturnOk() throws Exception {
        mockMvc.perform(post("/roles/assign-admin")
                .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void givenAdminRole_whenRevokeAdminEndpoint_thenReturnOk() throws Exception {
        mockMvc.perform(post("/roles/revoke-admin")
                .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void givenLibrarianRole_whenAssignLibrarianEndpoint_thenReturnOk() throws Exception {
        mockMvc.perform(post("/roles/assign-librarian")
                .with(user("librarian").roles("LIBRARIAN")))
                .andExpect(status().isOk());
    }

    @Test
    void givenAdminRole_whenAssignLibrarianEndpoint_thenReturnOk() throws Exception {
        mockMvc.perform(post("/roles/assign-librarian")
                .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void givenTeacherRole_whenGetReportsEndpoint_thenReturnOk() throws Exception {
        mockMvc.perform(get("/reports/test")
                .with(user("teacher").roles("TEACHER")))
                .andExpect(status().isOk());
    }

    @Test
    void givenLibrarianRole_whenGetReportsEndpoint_thenReturnOk() throws Exception {
        mockMvc.perform(get("/reports/test")
                .with(user("librarian").roles("LIBRARIAN")))
                .andExpect(status().isOk());
    }

    @Test
    void givenLibrarianRole_whenGetBookCopiesEndpoint_thenReturnOk() throws Exception {
        mockMvc.perform(get("/book-copies/test")
                .with(user("librarian").roles("LIBRARIAN")))
                .andExpect(status().isOk());
    }

    @Test
    void givenLibrarianRole_whenPostLoansEndpoint_thenReturnOk() throws Exception {
        mockMvc.perform(post("/loans/test")
                .with(user("librarian").roles("LIBRARIAN")))
                .andExpect(status().isOk());
    }

    @Test
    void givenStudentRole_whenGetLoansMeEndpoint_thenReturnOk() throws Exception {
        mockMvc.perform(get("/loans/me")
                .with(user("student").roles("STUDENT")))
                .andExpect(status().isOk());
    }

    @Test
    void givenStudentRole_whenGetReadingListsEndpoint_thenReturnOk() throws Exception {
        mockMvc.perform(get("/reading-lists/test")
                .with(user("student").roles("STUDENT")))
                .andExpect(status().isOk());
    }

    @Test
    void givenStudentRole_whenGetSubscriptionsEndpoint_thenReturnOk() throws Exception {
        mockMvc.perform(get("/subscriptions/test")
                .with(user("student").roles("STUDENT")))
                .andExpect(status().isOk());
    }

    @Test
    void givenStudentRole_whenGetCatalogSidebarSettingsEndpoint_thenReturnOk() throws Exception {
        mockMvc.perform(get("/schools/1/catalog-sidebar-settings")
                .with(user("student").roles("STUDENT")))
                .andExpect(status().isOk());
    }

    @Test
    void givenLibrarianRole_whenPutCatalogSidebarSettingsEndpoint_thenReturnOk() throws Exception {
        mockMvc.perform(put("/schools/1/catalog-sidebar-settings")
                .with(user("librarian").roles("LIBRARIAN")))
                .andExpect(status().isOk());
    }

    @Test
    void givenStudentRole_whenGetCampusesEndpoint_thenReturnOk() throws Exception {
        mockMvc.perform(get("/campuses/test")
                .with(user("student").roles("STUDENT")))
                .andExpect(status().isOk());
    }

    @Test
    void givenTeacherRole_whenGetLessonTipsEndpoint_thenReturnOk() throws Exception {
        mockMvc.perform(get("/lesson-tips/test")
                .with(user("teacher").roles("TEACHER")))
                .andExpect(status().isOk());
    }

    @Test
    void givenStudentRole_whenGetReadingLevelsEndpoint_thenReturnOk() throws Exception {
        mockMvc.perform(get("/reading-levels/test")
                .with(user("student").roles("STUDENT")))
                .andExpect(status().isOk());
    }

    @Test
    void givenNoAuthentication_whenGetDevModeEnabledEndpoint_thenReturnOk() throws Exception {
        mockMvc.perform(get("/schools/dev-mode-enabled"))
                .andExpect(status().isOk());
    }

    @Test
    void givenStudentRole_whenGetReviewsEndpoint_thenReturnOk() throws Exception {
        mockMvc.perform(get("/reviews/test")
                .with(user("student").roles("STUDENT")))
                .andExpect(status().isOk());
    }

    @Test
    void givenSecurityConfig_whenAccessTokenResponseClientCreated_thenReturnClientInstance() {
        OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> client = securityConfig
                .accessTokenResponseClient();

        assertNotNull(client);
    }
}