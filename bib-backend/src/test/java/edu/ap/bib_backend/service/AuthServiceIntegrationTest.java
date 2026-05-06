package edu.ap.bib_backend.service;

import edu.ap.bib_backend.jpa.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class AuthServiceIntegrationTest {

    @Autowired
    private AuthService authService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private TokenStorageService tokenStorageService;

    @Test
    void Given_NonExistentUser_When_GetUserDetails_Then_ReturnsNullOrIncomplete() {
        // Arrange
        String userId = "non-existent";
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(userId);
        when(auth.getAuthorities()).thenAnswer(i -> Collections.singleton(new SimpleGrantedAuthority("ROLE_STUDENT")));
        
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        when(tokenStorageService.getSmartschoolAccessToken(userId)).thenReturn(null);

        // Act
        // Since no Smartschool token and no user in DB, and auth is not OAuth2AuthenticationToken, it returns null or fallback
        var result = authService.getUserDetails(auth);

        // Assert
        assertNull(result);
    }
}
