package edu.ap.bib_backend.service;

import edu.ap.bib_backend.dto.AuthUserDto;
import edu.ap.bib_backend.jpa.entity.RoleEntity;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import edu.ap.bib_backend.mapper.UserMapper;
import edu.ap.bib_backend.options.UserGetOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
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
        mockRole.setRole(edu.ap.bib_backend.jpa.enums.Role.STUDENT);
        mockUser.setRole(mockRole);
        when(userRepository.findById(existingUserId)).thenReturn(Optional.of(mockUser));

        // Act
        Optional<User> result = userService.getUser(existingUserId);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(existingUserId, result.get().getId());
        verify(userRepository, times(1)).findById(existingUserId);
    }

    // ==================== getUsers Tests ====================

    @Test
    void givenBlankIdFilter_WhenGetUsers_ThenUseFindAllAndMapToDtos() {
        // Arrange
        UserGetOptions options = new UserGetOptions();
        options.setId("");

        User firstUser = new User();
        firstUser.setId("s001");
        User secondUser = new User();
        secondUser.setId("t001");
        List<User> users = List.of(firstUser, secondUser);

        List<AuthUserDto> expectedDtos = List.of(
                new AuthUserDto("s001", null, null, null, null, null, null, null, null, null, false),
                new AuthUserDto("t001", null, null, null, null, null, null, null, null, null, false));

        when(userRepository.findAll()).thenReturn(users);
        when(userMapper.toDtoList(users)).thenReturn(expectedDtos);

        // Act
        List<AuthUserDto> result = userService.getUsers(options);

        // Assert
        assertEquals(expectedDtos, result);
        verify(userRepository, times(1)).findAll();
        verify(userRepository, never()).findByIdContaining(any());
        verify(userMapper, times(1)).toDtoList(users);
    }

    @Test
    void givenIdFilter_WhenGetUsers_ThenUseFindByIdContainingAndMapToDtos() {
        // Arrange
        UserGetOptions options = new UserGetOptions();
        options.setId("ab");

        User user = new User();
        user.setId("abc123");
        List<User> users = List.of(user);

        List<AuthUserDto> expectedDtos = List
                .of(new AuthUserDto("abc123", null, null, null, null, null, null, null, null, null, false));

        when(userRepository.findByIdContaining("ab")).thenReturn(users);
        when(userMapper.toDtoList(users)).thenReturn(expectedDtos);

        // Act
        List<AuthUserDto> result = userService.getUsers(options);

        // Assert
        assertEquals(expectedDtos, result);
        verify(userRepository, times(1)).findByIdContaining("ab");
        verify(userRepository, never()).findAll();
        verify(userMapper, times(1)).toDtoList(users);
    }

    // ==================== createUser Tests ====================

    @Test
    void givenNewUserIdAndValidRole_WhenCreateUser_ThenSaveAndReturnUser() {
        // Arrange
        String newUserId = "newuser";
        RoleEntity mockRole = new RoleEntity();
        mockRole.setRole(edu.ap.bib_backend.jpa.enums.Role.STUDENT);
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
        mockRole.setRole(edu.ap.bib_backend.jpa.enums.Role.STUDENT);
        when(userRepository.existsById(existingUserId)).thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.createUser(existingUserId, mockRole));
        assertEquals("Gebruiker bestaat al", exception.getMessage());
        verify(userRepository, times(1)).existsById(existingUserId);
        verify(userRepository, never()).save(any(User.class));
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

    // ==================== setReviewBlocked Tests ====================

    @Test
    void givenValidUserIdAndBlockedTrue_WhenSetReviewBlocked_ThenReturnUpdatedAuthUserDto() {
        // Arrange
        String userId = "user123";
        User user = new User();
        user.setId(userId);
        user.setReviewBlocked(false);

        User updatedUser = new User();
        updatedUser.setId(userId);
        updatedUser.setReviewBlocked(true);

        AuthUserDto expectedDto = new AuthUserDto(userId, null, null, null, null, null, null, null, null, null, true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);
        when(userMapper.toDto(updatedUser)).thenReturn(expectedDto);

        // Act
        AuthUserDto result = userService.setReviewBlocked(userId, true);

        // Assert
        assertNotNull(result);
        assertTrue(result.reviewBlocked());
        assertEquals(userId, result.id());
        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, times(1)).save(any(User.class));
        verify(userMapper, times(1)).toDto(updatedUser);
    }

    @Test
    void givenValidUserIdAndBlockedFalse_WhenSetReviewBlocked_ThenReturnUpdatedAuthUserDto() {
        // Arrange
        String userId = "user456";
        User user = new User();
        user.setId(userId);
        user.setReviewBlocked(true);

        User updatedUser = new User();
        updatedUser.setId(userId);
        updatedUser.setReviewBlocked(false);

        AuthUserDto expectedDto = new AuthUserDto(userId, null, null, null, null, null, null, null, null, null, false);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);
        when(userMapper.toDto(updatedUser)).thenReturn(expectedDto);

        // Act
        AuthUserDto result = userService.setReviewBlocked(userId, false);

        // Assert
        assertNotNull(result);
        assertFalse(result.reviewBlocked());
        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, times(1)).save(any(User.class));
        verify(userMapper, times(1)).toDto(updatedUser);
    }
}
