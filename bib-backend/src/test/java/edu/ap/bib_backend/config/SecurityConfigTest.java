package edu.ap.bib_backend.config;

import edu.ap.bib_backend.security.DevMockRoleFilter;
import edu.ap.bib_backend.service.SmartschoolOAuth2UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SecurityConfigTestController.class)
@Import({ SecurityConfig.class, DevMockRoleFilter.class })
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SecurityConfig securityConfig;

    @MockitoBean
    private SmartschoolOAuth2UserService smartschoolOAuth2UserService;

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    private OAuth2AuthorizedClientService authorizedClientService;

    @Test
    void givenNoAuthentication_WhenGetAuthMeEndpoint_ThenReturnOk() throws Exception {
        // Arrange
        String endpoint = "/auth/me";

        // Act
        var result = mockMvc.perform(get(endpoint));

        // Assert
        result.andExpect(status().isOk());
    }

    @Test
    void givenNoAuthentication_WhenGetBooksEndpoint_ThenReturnRedirectToOAuthLogin() throws Exception {
        // Arrange
        String endpoint = "/books/test";

        // Act
        var result = mockMvc.perform(get(endpoint));

        // Assert
        result.andExpect(status().is3xxRedirection());
    }

    @Test
    void givenStudentRoleAuthentication_WhenGetBooksEndpoint_ThenReturnOk() throws Exception {
        // Arrange
        String endpoint = "/books/test";

        // Act
        var result = mockMvc.perform(get(endpoint)
                .with(user("student-user").roles("STUDENT")));

        // Assert
        result.andExpect(status().isOk());
    }

    @Test
    void givenStudentRoleAuthentication_WhenGetLabelsEndpoint_ThenReturnForbidden() throws Exception {
        // Arrange
        String endpoint = "/labels/test";

        // Act
        var result = mockMvc.perform(get(endpoint)
                .with(user("student-user").roles("STUDENT")));

        // Assert
        result.andExpect(status().isForbidden());
    }

    @Test
    void givenAdminRoleAuthentication_WhenGetLabelsEndpoint_ThenReturnOk() throws Exception {
        // Arrange
        String endpoint = "/labels/test";

        // Act
        var result = mockMvc.perform(get(endpoint)
                .with(user("admin-user").roles("ADMIN")));

        // Assert
        result.andExpect(status().isOk());
    }

    @Test
    void givenSecurityConfig_WhenAccessTokenResponseClientCreated_ThenReturnClientInstance() {
        // Arrange

        // Act
        OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> client = securityConfig
                .accessTokenResponseClient();

        // Assert
        assertNotNull(client);
    }

}