package edu.ap.bib_backend.service;

import edu.ap.bib_backend.dto.SmartschoolGroupDto;
import edu.ap.bib_backend.jpa.entity.Group;
import edu.ap.bib_backend.jpa.entity.RoleEntity;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.enums.Role;
import edu.ap.bib_backend.service.user.UserService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@SpringJUnitConfig
@ContextConfiguration(classes = {
        SmartschoolOAuth2UserService.class,
        SmartschoolAudienceResolverService.class
})
class SmartschoolOAuth2UserServiceTest {

    private static final String USER_INFO_ENDPOINT = "https://smartschool.example.com/userinfo";
    private static final String GROUP_INFO_ENDPOINT = "https://smartschool.example.com/groupinfo";
    private static final String PLATFORM_URL = "https://aphogeschool.smartschool.be";

    @Autowired
    private SmartschoolOAuth2UserService smartschoolOAuth2UserService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private RoleService roleService;

    @MockitoBean
    private GroupService groupService;

    @MockitoBean
    private SmartschoolProfileService smartschoolProfileService;

    private MockRestServiceServer mockRestServiceServer;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        ReflectionTestUtils.setField(smartschoolOAuth2UserService, "restTemplate", restTemplate);
        mockRestServiceServer = MockRestServiceServer.bindTo(restTemplate).build();
    }

    @Test
    @DisplayName("givenExistingUser whenLoadUser thenReturnOAuth2UserWithExistingUserRoleAndSchoolClaims")
    void givenExistingUser_whenLoadUser_thenReturnOAuth2UserWithExistingUserRoleAndSchoolClaims() {
        // Arrange
        String accessTokenValue = "access-token-123";
        OAuth2UserRequest userRequest = createUserRequest(accessTokenValue);

        User existingUser = createUser("existing-user", Role.TEACHER);
        Group existingGroup = createGroup("2ITSOF1");

        when(userService.getUser("existing-user")).thenReturn(Optional.of(existingUser));
        when(smartschoolProfileService.fetchGroups(accessTokenValue))
                .thenReturn(new SmartschoolGroupDto("2ITSOF1", List.of("Leerlingen")));
        when(groupService.getGroup("2ITSOF1")).thenReturn(existingGroup);

        expectUserInfoRequest(accessTokenValue, """
            {
              "userID": "existing-user",
              "basisrol": "leerling",
              "platform": "https://aphogeschool.smartschool.be"
            }
            """);

        expectGroupInfoRequest(accessTokenValue, """
            {
              "groups": [
                {
                  "groupID": "7868b96OFO4=",
                  "name": "2ITSOF1",
                  "description": "2ITSOF1",
                  "platform": "https://aphogeschool.smartschool.be"
                }
              ],
              "parentGroups": [
                {
                  "groupID": "rvLPM/sIwrg=",
                  "name": "Leerlingen",
                  "description": "Leerlingen",
                  "platform": "https://aphogeschool.smartschool.be"
                }
              ]
            }
            """);

        // Act
        OAuth2User result = smartschoolOAuth2UserService.loadUser(userRequest);

        // Assert
        assertEquals("existing-user", result.getAttribute("userID"));
        assertEquals("2ITSOF1", result.getAttribute("classCode"));
        assertEquals("2", result.getAttribute("yearCode"));
        assertEquals(PLATFORM_URL, result.getAttribute("schoolCode"));
        assertTrue(result.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_TEACHER")));

        verify(userService, times(1)).getUser("existing-user");
        verify(userService, never()).createUser(anyString(), org.mockito.ArgumentMatchers.any());
        verify(roleService, never()).getRoleEntity(anyString());
        verify(smartschoolProfileService, times(1)).fetchGroups(accessTokenValue);
        verify(groupService, times(1)).getGroup("2ITSOF1");
        verify(groupService, never()).createGroup(anyString());

        mockRestServiceServer.verify();
    }

    @Test
    @DisplayName("givenNonExistingUser whenLoadUser thenCreateUserAndReturnOAuth2UserWithCreatedUserRole")
    void givenNonExistingUser_whenLoadUser_thenCreateUserAndReturnOAuth2UserWithCreatedUserRole() {
        // Arrange
        String accessTokenValue = "access-token-456";
        OAuth2UserRequest userRequest = createUserRequest(accessTokenValue);

        RoleEntity studentRole = createRoleEntity(Role.STUDENT);
        User createdUser = createUser("new-user", Role.STUDENT);
        Group existingGroup = createGroup("2ITSOF1");

        when(userService.getUser("new-user")).thenReturn(Optional.empty());
        when(roleService.getRoleEntity("leerling")).thenReturn(studentRole);
        when(userService.createUser("new-user", studentRole)).thenReturn(createdUser);
        when(smartschoolProfileService.fetchGroups(accessTokenValue))
                .thenReturn(new SmartschoolGroupDto("2ITSOF1", List.of("Leerlingen")));
        when(groupService.getGroup("2ITSOF1")).thenReturn(existingGroup);

        expectUserInfoRequest(accessTokenValue, """
            {
              "userID": "new-user",
              "basisrol": "leerling",
              "platform": "https://aphogeschool.smartschool.be"
            }
            """);

        expectGroupInfoRequest(accessTokenValue, """
            {
              "groups": [
                {
                  "groupID": "7868b96OFO4=",
                  "name": "2ITSOF1",
                  "description": "2ITSOF1",
                  "platform": "https://aphogeschool.smartschool.be"
                }
              ],
              "parentGroups": [
                {
                  "groupID": "rvLPM/sIwrg=",
                  "name": "Leerlingen",
                  "description": "Leerlingen",
                  "platform": "https://aphogeschool.smartschool.be"
                }
              ]
            }
            """);

        // Act
        OAuth2User result = smartschoolOAuth2UserService.loadUser(userRequest);

        // Assert
        assertEquals("new-user", result.getAttribute("userID"));
        assertEquals("2ITSOF1", result.getAttribute("classCode"));
        assertEquals("2", result.getAttribute("yearCode"));
        assertEquals(PLATFORM_URL, result.getAttribute("schoolCode"));
        assertTrue(result.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_STUDENT")));

        verify(userService, times(1)).getUser("new-user");
        verify(roleService, times(1)).getRoleEntity("leerling");
        verify(userService, times(1)).createUser("new-user", studentRole);
        verify(smartschoolProfileService, times(1)).fetchGroups(accessTokenValue);
        verify(groupService, times(1)).getGroup("2ITSOF1");
        verify(groupService, never()).createGroup(anyString());

        mockRestServiceServer.verify();
    }

    @Test
    @DisplayName("givenMissingGroupInDatabase whenLoadUser thenCreateGroupAndAttachItToUser")
    void givenMissingGroupInDatabase_whenLoadUser_thenCreateGroupAndAttachItToUser() {
        // Arrange
        String accessTokenValue = "access-token-789";
        OAuth2UserRequest userRequest = createUserRequest(accessTokenValue);

        User existingUser = createUser("group-create-user", Role.STUDENT);
        Group createdGroup = createGroup("2ITSOF1");

        when(userService.getUser("group-create-user")).thenReturn(Optional.of(existingUser));
        when(smartschoolProfileService.fetchGroups(accessTokenValue))
                .thenReturn(new SmartschoolGroupDto("2ITSOF1", List.of("Leerlingen")));
        when(groupService.getGroup("2ITSOF1")).thenThrow(new RuntimeException("Group not found"));
        when(groupService.createGroup("2ITSOF1")).thenReturn(createdGroup);

        expectUserInfoRequest(accessTokenValue, """
            {
              "userID": "group-create-user",
              "basisrol": "leerling",
              "platform": "https://aphogeschool.smartschool.be"
            }
            """);

        expectGroupInfoRequest(accessTokenValue, """
            {
              "groups": [
                {
                  "groupID": "7868b96OFO4=",
                  "name": "2ITSOF1",
                  "description": "2ITSOF1",
                  "platform": "https://aphogeschool.smartschool.be"
                }
              ],
              "parentGroups": [
                {
                  "groupID": "rvLPM/sIwrg=",
                  "name": "Leerlingen",
                  "description": "Leerlingen",
                  "platform": "https://aphogeschool.smartschool.be"
                }
              ]
            }
            """);

        // Act
        OAuth2User result = smartschoolOAuth2UserService.loadUser(userRequest);

        // Assert
        assertEquals("group-create-user", result.getAttribute("userID"));
        assertEquals("2ITSOF1", result.getAttribute("classCode"));
        assertTrue(result.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_STUDENT")));

        verify(userService, times(1)).getUser("group-create-user");
        verify(groupService, times(1)).getGroup("2ITSOF1");
        verify(groupService, times(1)).createGroup("2ITSOF1");

        mockRestServiceServer.verify();
    }

    @Test
    @DisplayName("givenMissingUserIdInUserInfo whenLoadUser thenThrowOAuth2AuthenticationException")
    void givenMissingUserIdInUserInfo_whenLoadUser_thenThrowOAuth2AuthenticationException() {
        // Arrange
        String accessTokenValue = "access-token-invalid";
        OAuth2UserRequest userRequest = createUserRequest(accessTokenValue);

        expectUserInfoRequest(accessTokenValue, """
            {
              "basisrol": "leerling",
              "platform": "https://aphogeschool.smartschool.be"
            }
            """);

        // Act + Assert
        OAuth2AuthenticationException exception = assertThrows(
                OAuth2AuthenticationException.class,
                () -> smartschoolOAuth2UserService.loadUser(userRequest)
        );

        assertTrue(exception.getMessage().contains("Smartschool user info mist veld: userID"));

        verify(userService, never()).getUser(anyString());
        verify(roleService, never()).getRoleEntity(anyString());
        verify(groupService, never()).getGroup(anyString());

        mockRestServiceServer.verify();
    }

    private OAuth2UserRequest createUserRequest(String accessTokenValue) {
        ClientRegistration clientRegistration = ClientRegistration.withRegistrationId("smartschool")
                .clientId("client-id")
                .clientSecret("client-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .authorizationUri("https://smartschool.example.com/oauth/authorize")
                .tokenUri("https://smartschool.example.com/oauth/token")
                .userInfoUri(USER_INFO_ENDPOINT)
                .userNameAttributeName("userID")
                .build();

        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                accessTokenValue,
                Instant.now().minusSeconds(30),
                Instant.now().plusSeconds(3600)
        );

        return new OAuth2UserRequest(clientRegistration, accessToken);
    }

    private void expectUserInfoRequest(String accessTokenValue, String responseBody) {
        mockRestServiceServer.expect(ExpectedCount.once(), requestTo(USER_INFO_ENDPOINT))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + accessTokenValue))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));
    }

    private void expectGroupInfoRequest(String accessTokenValue, String responseBody) {
        mockRestServiceServer.expect(ExpectedCount.once(), requestTo(GROUP_INFO_ENDPOINT))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + accessTokenValue))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));
    }

    private User createUser(String userId, Role role) {
        User user = new User();
        user.setId(userId);
        user.setRole(createRoleEntity(role));
        return user;
    }

    private RoleEntity createRoleEntity(Role role) {
        RoleEntity roleEntity = new RoleEntity();
        roleEntity.setRole(role);
        return roleEntity;
    }

    private Group createGroup(String name) {
        Group group = new Group();
        group.setName(name);
        return group;
    }
}