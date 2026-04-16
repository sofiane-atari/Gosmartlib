package edu.ap.bib_backend.controller;

import edu.ap.bib_backend.config.JwtAuthenticationFilter;
import edu.ap.bib_backend.config.SecurityConfig;
import edu.ap.bib_backend.dto.SmartschoolGroupDto;
import edu.ap.bib_backend.dto.SmartschoolProfileDto;
import edu.ap.bib_backend.service.JwtService;
import edu.ap.bib_backend.service.SmartschoolOAuth2UserService;
import edu.ap.bib_backend.service.SmartschoolProfileService;
import edu.ap.bib_backend.service.TokenStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {"FRONTEND_REDIRECT_URL=http://localhost:4200/login"})
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private TokenStorageService tokenStorageService;

    @MockitoBean
    private OAuth2AuthorizedClientService authorizedClientService;

    @MockitoBean
    private SmartschoolProfileService smartschoolProfileService;

    @MockitoBean
    private SmartschoolOAuth2UserService smartschoolOAuth2UserService;

    @Test
    void givenUnauthenticated_whenMeCalled_thenReturn302() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/auth/me"))
                .andExpect(MockMvcResultMatchers.status().is3xxRedirection());
    }

    @Test
    void givenUnauthenticated_whenLogoutCalled_thenReturn302() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/auth/logout").with(csrf()))
                .andExpect(MockMvcResultMatchers.status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "1", authorities = {"ROLE_TEACHER"})
    void givenUserWithToken_whenMeCalled_thenReturnFullProfile() throws Exception {
        String mockToken = "valid-smartschool-token";
        when(tokenStorageService.getSmartschoolAccessToken("1")).thenReturn(mockToken);

        SmartschoolProfileDto profile = new SmartschoolProfileDto("1", "Anna", "Smet", "Anna Smet", "anna@school.be", "platform");
        SmartschoolGroupDto group = new SmartschoolGroupDto("Leerkrachten", List.of("Personeel"));

        when(smartschoolProfileService.fetchProfile(mockToken)).thenReturn(profile);
        when(smartschoolProfileService.fetchGroups(mockToken)).thenReturn(group);

        ResultActions result = mockMvc.perform(MockMvcRequestBuilders.get("/auth/me"));

        result.andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value("1"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.role").value("TEACHER"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.firstName").value("Anna")) // Dit is nu succesvol "Anna"
                .andExpect(MockMvcResultMatchers.jsonPath("$.email").value("anna@school.be"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.primaryGroupName").value("Leerkrachten"));
    }

    @Test
    @WithMockUser(username = "1", authorities = {"ROLE_STUDENT"})
    void givenAuthenticatedUser_whenMeCalled_thenReturnUserInfo() throws Exception {
        when(tokenStorageService.getSmartschoolAccessToken("1")).thenReturn(null);

        ResultActions result = mockMvc.perform(MockMvcRequestBuilders.get("/auth/me"));

        result.andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value("1"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.role").value("STUDENT"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.firstName").doesNotExist());
    }

    @Test
    void givenOAuth2User_whenCallbackCalled_thenRedirectWithToken() throws Exception {
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_STUDENT"));
        OAuth2User principal = new DefaultOAuth2User(authorities, Map.of("sub", "1"), "sub");
        OAuth2AuthenticationToken auth = new OAuth2AuthenticationToken(principal, authorities, "smartschool");

        OAuth2AccessToken smartToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "smart-token", null, null);
        OAuth2AuthorizedClient authorizedClient = mock(OAuth2AuthorizedClient.class);
        when(authorizedClient.getAccessToken()).thenReturn(smartToken);
        when(authorizedClientService.loadAuthorizedClient("smartschool", "1")).thenReturn(authorizedClient);

        when(jwtService.generateToken(any(), any())).thenReturn("jwt-token");

        ResultActions result = mockMvc.perform(MockMvcRequestBuilders.get("/auth/callback")
                .with(authentication(auth)));

        result.andExpect(MockMvcResultMatchers.status().is3xxRedirection())
                .andExpect(MockMvcResultMatchers.header().string("Location", containsString("token=jwt-token")));
        
        verify(jwtService).generateToken("1", "ROLE_STUDENT");
        verify(tokenStorageService).storeToken("1", "jwt-token");
        verify(tokenStorageService).storeUserMetadata("1", "ROLE_STUDENT");
        verify(tokenStorageService).storeSmartschoolAccessToken("1", "smart-token");
    }

    @Test
    void givenOAuth2User_whenLogoutCalled_thenRevokeTokenAndReturnMessage() throws Exception {
        Collection<? extends GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_STUDENT"));
        Authentication auth = new UsernamePasswordAuthenticationToken("1", null, authorities);

        ResultActions result = mockMvc.perform(MockMvcRequestBuilders.post("/auth/logout")
                .with(csrf())
                .with(authentication(auth)));

        result.andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Logged out successfully"));
        
        verify(tokenStorageService).revokeToken("1");
        verify(tokenStorageService).removeUserMetadata("1");
    }
}