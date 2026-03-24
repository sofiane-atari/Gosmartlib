package edu.ap.bib_backend.service;

import edu.ap.bib_backend.jpa.entity.Role;
import edu.ap.bib_backend.jpa.entity.RoleEntity;
import edu.ap.bib_backend.jpa.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@SpringJUnitConfig
@ContextConfiguration(classes = SmartschoolOAuth2UserService.class)
class SmartschoolOAuth2UserServiceTest {

    @Autowired
    private SmartschoolOAuth2UserService smartschoolOAuth2UserService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private RoleService roleService;

    private MockRestServiceServer mockRestServiceServer;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        ReflectionTestUtils.setField(smartschoolOAuth2UserService, "restTemplate", restTemplate);
        mockRestServiceServer = MockRestServiceServer.bindTo(restTemplate).build();
    }

    @Test
    void givenExistingUser_WhenLoadUser_ThenReturnOAuth2UserWithExistingUserRole() {
        String userInfoEndpoint = "https://smartschool.example.com/userinfo";
        String accessTokenValue = "access-token-123";
        OAuth2UserRequest userRequest = createUserRequest(userInfoEndpoint, accessTokenValue);
        String responseBody = "{\"userID\":\"existing-user\",\"basisrol\":\"leerling\",\"klas\":\"2aICT\"}";

        RoleEntity teacherRole = new RoleEntity();
        teacherRole.setRole(Role.TEACHER);
        User existingUser = new User();
        existingUser.setId("existing-user");
        existingUser.setRole(teacherRole);

        when(userService.getUser("existing-user")).thenReturn(Optional.of(existingUser));
        when(userService.saveWithClassCodeIfChanged(existingUser, "2aICT")).thenReturn(existingUser);

        mockRestServiceServer.expect(requestTo(userInfoEndpoint))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + accessTokenValue))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        OAuth2User result = smartschoolOAuth2UserService.loadUser(userRequest);

        assertEquals("existing-user", result.getAttribute("userID"));
        assertTrue(result.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_TEACHER")));
        verify(userService, times(1)).getUser("existing-user");
        verify(userService, times(1)).saveWithClassCodeIfChanged(existingUser, "2aICT");
        verify(userService, never()).createUser(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString());
        verify(roleService, never()).getRoleEntity(org.mockito.ArgumentMatchers.anyString());
        mockRestServiceServer.verify();
    }

    @Test
    void givenNonExistingUser_WhenLoadUser_ThenCreateUserAndReturnOAuth2UserWithCreatedUserRole() {
        String userInfoEndpoint = "https://smartschool.example.com/userinfo";
        String accessTokenValue = "access-token-456";
        OAuth2UserRequest userRequest = createUserRequest(userInfoEndpoint, accessTokenValue);
        String responseBody = "{\"userID\":\"new-user\",\"basisrol\":\"leerling\",\"klas\":\"2aICT\"}";

        RoleEntity studentRole = new RoleEntity();
        studentRole.setRole(Role.STUDENT);
        User createdUser = new User();
        createdUser.setId("new-user");
        createdUser.setRole(studentRole);

        when(userService.getUser("new-user")).thenReturn(Optional.empty());
        when(roleService.getRoleEntity("leerling")).thenReturn(studentRole);
        when(userService.createUser("new-user", studentRole, "2aICT")).thenReturn(createdUser);

        mockRestServiceServer.expect(requestTo(userInfoEndpoint))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + accessTokenValue))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        OAuth2User result = smartschoolOAuth2UserService.loadUser(userRequest);

        assertEquals("new-user", result.getAttribute("userID"));
        assertTrue(result.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_STUDENT")));
        verify(userService, times(1)).getUser("new-user");
        verify(roleService, times(1)).getRoleEntity("leerling");
        verify(userService, times(1)).createUser("new-user", studentRole, "2aICT");
        mockRestServiceServer.verify();
    }

    private OAuth2UserRequest createUserRequest(String userInfoEndpoint, String accessTokenValue) {
        ClientRegistration clientRegistration = ClientRegistration.withRegistrationId("smartschool")
                .clientId("client-id")
                .clientSecret("client-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .authorizationUri("https://smartschool.example.com/oauth/authorize")
                .tokenUri("https://smartschool.example.com/oauth/token")
                .userInfoUri(userInfoEndpoint)
                .userNameAttributeName("userID")
                .build();

        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                accessTokenValue,
                Instant.now().minusSeconds(30),
                Instant.now().plusSeconds(3600));

        return new OAuth2UserRequest(clientRegistration, accessToken);
    }
}