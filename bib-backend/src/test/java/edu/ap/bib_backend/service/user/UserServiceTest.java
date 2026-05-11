package edu.ap.bib_backend.service.user;

import edu.ap.bib_backend.dto.AuthUserDto;
import edu.ap.bib_backend.jpa.entity.Campus;
import edu.ap.bib_backend.jpa.entity.RoleEntity;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import edu.ap.bib_backend.mapper.UserMapper;
import edu.ap.bib_backend.options.UserGetOptions;
import edu.ap.bib_backend.service.TokenStorageService;
import edu.ap.bib_backend.service.oneroster.OneRosterClient;
import edu.ap.bib_backend.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private OneRosterClient oneRosterClient;
    @Mock
    private TokenStorageService tokenStorageService;

    @InjectMocks
    private UserService userService;

    private final String userId = "user123";

    @Test
    void Given_ExistingUserId_When_GetUser_Then_ReturnsUser() {
        // Arrange
        User user = new User();
        user.setId(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act
        Optional<User> result = userService.getUser(userId);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(userId, result.get().getId());
    }

    @Test
    void Given_NewUser_When_CreateUser_Then_SavesAndReturnsUser() {
        // Arrange
        RoleEntity role = new RoleEntity();
        School school = new School();
        when(userRepository.existsById(userId)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        Campus campus = new Campus();
        User result = userService.createUser(userId, role, school, campus);

        // Assert
        assertNotNull(result);
        assertEquals(userId, result.getId());
        assertEquals(role, result.getRole());
        assertEquals(school, result.getSchool());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void Given_ExistingUser_When_CreateUser_Then_ThrowsRuntimeException() {
        // Arrange
        when(userRepository.existsById(userId)).thenReturn(true);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> userService.createUser(userId, new RoleEntity(), new School(), new Campus()));
    }

    @Test
    void Given_ValidUser_When_SetReviewBlocked_Then_UpdatesAndReturnsDto() {
        // Arrange
        User user = new User();
        user.setId(userId);
        user.setReviewBlocked(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        
        AuthUserDto expectedDto = mock(AuthUserDto.class);
        when(userMapper.toDto(user)).thenReturn(expectedDto);

        // Act
        AuthUserDto result = userService.setReviewBlocked(userId, true);

        // Assert
        assertTrue(user.isReviewBlocked());
        assertEquals(expectedDto, result);
        verify(userRepository).save(user);
    }

    @Test
    void Given_AuthenticatedUser_When_GetUsers_Then_FetchesFromOneRosterAndFilters() {
        // Arrange
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(userId);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        User currentUser = new User();
        School school = new School();
        school.setDomainName("test.smartschool.be");
        currentUser.setSchool(school);
        when(userRepository.findById(userId)).thenReturn(Optional.of(currentUser));
        
        when(tokenStorageService.getSmartschoolAccessToken(userId)).thenReturn("token");
        
        Map<String, Object> rosterUser = Map.of(
            "metadata", Map.of("smsc.legacyIdentifier", "other-user"),
            "fullName", "Other User"
        );
        when(oneRosterClient.fetchUsersWithBearer("test.smartschool.be", "token"))
            .thenReturn(List.of(rosterUser));

        // Mock nested call to getCurrentUserSchool for the mapped user
        User otherUser = new User();
        otherUser.setSchool(school);
        when(userRepository.findById("other-user")).thenReturn(Optional.of(otherUser));

        UserGetOptions options = new UserGetOptions();
        options.setId("other");

        // Act
        List<AuthUserDto> result = userService.getUsers(options);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("other-user", result.get(0).id());
        
        SecurityContextHolder.clearContext();
    }
}
