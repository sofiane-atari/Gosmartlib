package edu.ap.bib_backend.service;

import edu.ap.bib_backend.jpa.entity.RoleEntity;
import edu.ap.bib_backend.jpa.entity.Role;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import edu.ap.bib_backend.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    // ==================== getUser Tests ====================

    @Test
    void givenNonExistingUserId_WhenGetUser_ThenReturnEmptyOptional() {
        // Arrange
        String nonExistingUserId = "nonexistent";
        when(userRepository.findById(nonExistingUserId)).thenReturn(Optional.empty());

        // Act
        Optional<User> result = userService.getUser(nonExistingUserId);

        // Assert
        assertTrue(result.isEmpty());
        verify(userRepository, times(1)).findById(nonExistingUserId);
    }

    @Test
    void givenExistingUserId_WhenGetUser_ThenReturnUserOptional() {
        // Arrange
        String existingUserId = "user123";
        User mockUser = new User();
        mockUser.setId(existingUserId);
        RoleEntity mockRole = new RoleEntity();
        mockRole.setRole(edu.ap.bib_backend.jpa.entity.Role.STUDENT);
        mockUser.setRole(mockRole);
        when(userRepository.findById(existingUserId)).thenReturn(Optional.of(mockUser));

        // Act
        Optional<User> result = userService.getUser(existingUserId);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(existingUserId, result.get().getId());
        verify(userRepository, times(1)).findById(existingUserId);
    }

    @Test
    void givenNullUserId_WhenGetUser_ThenReturnEmptyOptional() {
        // Arrange
        String nullUserId = null;
        when(userRepository.findById(null)).thenReturn(Optional.empty());

        // Act
        Optional<User> result = userService.getUser(nullUserId);

        // Assert
        assertTrue(result.isEmpty());
        verify(userRepository, times(1)).findById(null);
    }

    // ==================== createUser Tests ====================

    @Test
    void givenNewUserIdAndValidRole_WhenCreateUser_ThenSaveAndReturnUser() {
        // Arrange
        String newUserId = "newuser";
        RoleEntity mockRole = new RoleEntity();
        mockRole.setRole(edu.ap.bib_backend.jpa.entity.Role.STUDENT);
        when(userRepository.existsById(newUserId)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(newUserId);
            user.setRole(mockRole);
            return user;
        });

        // Act
        User result = userService.createUser(newUserId, mockRole);

        // Assert
        assertNotNull(result);
        assertEquals(newUserId, result.getId());
        assertEquals(mockRole, result.getRole());
        verify(userRepository, times(1)).existsById(newUserId);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void givenExistingUserIdAndAnyRole_WhenCreateUser_ThenThrowRuntimeException() {
        // Arrange
        String existingUserId = "existinguser";
        RoleEntity mockRole = new RoleEntity();
        mockRole.setRole(edu.ap.bib_backend.jpa.entity.Role.STUDENT);
        when(userRepository.existsById(existingUserId)).thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.createUser(existingUserId, mockRole));
        assertEquals("Gebruiker bestaat al", exception.getMessage());
        verify(userRepository, times(1)).existsById(existingUserId);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void givenNullUserIdAndValidRole_WhenCreateUser_ThenThrowIllegalArgumentExceptionFromRepo() {
        // Arrange
        String nullUserId = null;
        RoleEntity mockRole = new RoleEntity();
        mockRole.setRole(edu.ap.bib_backend.jpa.entity.Role.STUDENT);
        when(userRepository.existsById(null)).thenReturn(false);
        when(userRepository.save(any(User.class)))
                .thenThrow(new IllegalArgumentException("Cannot save User with null id"));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.createUser(nullUserId, mockRole));
        assertEquals("Cannot save User with null id", exception.getMessage());
        verify(userRepository, times(1)).existsById(null);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void givenValidUserIdAndNullRole_WhenCreateUser_ThenSaveUserWithNullRole() {
        // Arrange
        String validUserId = "validuser";
        RoleEntity nullRole = null;
        when(userRepository.existsById(validUserId)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(validUserId);
            user.setRole(nullRole);
            return user;
        });

        // Act
        User result = userService.createUser(validUserId, nullRole);

        // Assert
        assertNotNull(result);
        assertEquals(validUserId, result.getId());
        assertNull(result.getRole());
        verify(userRepository, times(1)).existsById(validUserId);
        verify(userRepository, times(1)).save(any(User.class));
    }
}
