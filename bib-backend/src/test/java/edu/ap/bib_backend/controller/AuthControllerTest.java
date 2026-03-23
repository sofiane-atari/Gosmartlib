// package edu.ap.bib_backend.controller;

// import edu.ap.bib_backend.dto.AuthUserDto;
// import edu.ap.bib_backend.jpa.entity.Role;
// import edu.ap.bib_backend.service.AuthService;
// import edu.ap.bib_backend.service.JwtService;
// import edu.ap.bib_backend.service.TokenStorageService;
// import org.junit.jupiter.api.DisplayName;
// import org.junit.jupiter.api.Test;
// import org.mockito.Mockito;
// import org.springframework.security.test.context.support.WithMockUser;
// import org.springframework.security.core.authority.SimpleGrantedAuthority;
// import org.springframework.security.oauth2.core.user.OAuth2User;
// import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
// import org.springframework.security.core.Authentication;
// import org.springframework.security.core.GrantedAuthority;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
// import org.springframework.test.context.bean.override.mockito.MockitoBean;
// import org.springframework.test.web.servlet.MockMvc;
// import org.springframework.test.web.servlet.ResultActions;
// import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
// import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

// import java.util.Collection;
// import java.util.Collections;

// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.Mockito.doReturn;
// import static org.mockito.Mockito.verify;
// import static org.mockito.Mockito.when;
// import static org.hamcrest.Matchers.containsString;
// import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
// import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

// @WebMvcTest(AuthController.class)
// public class AuthControllerTest {
//     @Autowired
//     private MockMvc mockMvc;

//     @MockitoBean
//     //private AuthService authService;
//     @MockitoBean
//     private JwtService jwtService;
//     @MockitoBean
//     private TokenStorageService tokenStorageService;

//     @Test
//     @WithMockUser
//     @DisplayName("Given authenticated user, When /auth/me called, Then return user info")
//     void givenAuthenticatedUser_whenMeCalled_thenReturnUserInfo() throws Exception {
//         // Arrange
//         AuthUserDto user = new AuthUserDto("1","test name", Role.STUDENT);
//         when(authService.getCurrentUser()).thenReturn(user);

//         // Act
//         ResultActions result = mockMvc.perform(MockMvcRequestBuilders.get("/auth/me"));

//         // Assert
//         result.andExpect(MockMvcResultMatchers.status().isOk())
//                 .andExpect(MockMvcResultMatchers.jsonPath("$.id").value("1"))
//                 .andExpect(MockMvcResultMatchers.jsonPath("$.role").value("STUDENT"));
//         verify(authService).getCurrentUser();
//     }

//     @Test
//     @WithMockUser
//     @DisplayName("Given error in session, When /auth/error called, Then return error message")
//     void givenErrorInSession_whenErrorCalled_thenReturnErrorMessage() throws Exception {
//         // Arrange
//         RuntimeException exception = new RuntimeException("OAuth2 failed");

//         // Act
//         ResultActions result = mockMvc.perform(MockMvcRequestBuilders.get("/auth/error")
//                 .sessionAttr("SPRING_SECURITY_LAST_EXCEPTION", exception));

//         // Assert
//         result.andExpect(MockMvcResultMatchers.status().isUnauthorized())
//                 .andExpect(MockMvcResultMatchers.jsonPath("$.error", containsString("OAuth2 failed")));
//     }

//     @Test
//     @DisplayName("Given OAuth2User, When /auth/callback called, Then redirect with token")
//     void givenOAuth2User_whenCallbackCalled_thenRedirectWithToken() throws Exception {
//         // Arrange
//         OAuth2User principal = Mockito.mock(OAuth2User.class);
//         when(principal.getAttribute("userID")).thenReturn("1");
//         Collection<? extends GrantedAuthority> authorities = Collections
//                 .singletonList(new SimpleGrantedAuthority("ROLE_STUDENT"));
//         doReturn(authorities).when(principal).getAuthorities();
//         when(jwtService.generateToken(any(), any())).thenReturn("jwt-token");
//         Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

//         // Act
//         ResultActions result = mockMvc.perform(MockMvcRequestBuilders.get("/auth/callback")
//                 .with(authentication(auth)));

//         // Assert
//         result.andExpect(MockMvcResultMatchers.status().is3xxRedirection())
//                 .andExpect(MockMvcResultMatchers.header().string("Location", containsString("token=jwt-token")));
//         verify(jwtService).generateToken("1", "ROLE_STUDENT");
//         verify(tokenStorageService).storeToken("1", "jwt-token");
//         verify(tokenStorageService).storeUserMetadata("1", "ROLE_STUDENT");
//     }

//     @Test
//     @DisplayName("Given OAuth2User, When /auth/logout called, Then revoke token and return message")
//     void givenOAuth2User_whenLogoutCalled_thenRevokeTokenAndReturnMessage() throws Exception {
//         // Arrange
//         OAuth2User principal = Mockito.mock(OAuth2User.class);
//         when(principal.getAttribute("userID")).thenReturn("1");
//         Collection<? extends GrantedAuthority> authorities = Collections
//                 .singletonList(new SimpleGrantedAuthority("ROLE_STUDENT"));
//         doReturn(authorities).when(principal).getAuthorities();
//         Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

//         // Act
//         ResultActions result = mockMvc.perform(MockMvcRequestBuilders.post("/auth/logout")
//                 .with(csrf())
//                 .with(authentication(auth)));

//         // Assert
//         result.andExpect(MockMvcResultMatchers.status().isOk())
//                 .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Logged out successfully"));
//         verify(tokenStorageService).revokeToken("1");
//     }
// }
