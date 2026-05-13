package edu.ap.bib_backend.service;

import edu.ap.bib_backend.dto.AuthUserDto;
import edu.ap.bib_backend.jpa.entity.Campus;
import edu.ap.bib_backend.jpa.entity.RoleEntity;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import edu.ap.bib_backend.mapper.UserMapper;
import edu.ap.bib_backend.options.UserGetOptions;
import edu.ap.bib_backend.service.user.UserService;

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

    @Test
    void givenNonExistingUserId_WhenGetUser_ThenReturnEmptyOptional() {
        String nonExistingUserId = "nonexistent";
        when(userRepository.findById(nonExistingUserId)).thenReturn(Optional.empty());
        Optional<User> result = userService.getUser(nonExistingUserId);
        assertTrue(result.isEmpty());
        verify(userRepository, times(1)).findById(nonExistingUserId);
    }

    @Test
    void givenExistingUserId_WhenGetUser_ThenReturnUserOptional() {
        String existingUserId = "user123";
        User mockUser = new User();
        mockUser.setId(existingUserId);
        RoleEntity mockRole = new RoleEntity();
        mockRole.setRole(edu.ap.bib_backend.jpa.enums.Role.STUDENT);
        mockUser.setRole(mockRole);
        when(userRepository.findById(existingUserId)).thenReturn(Optional.of(mockUser));
        Optional<User> result = userService.getUser(existingUserId);
        assertTrue(result.isPresent());
        assertEquals(existingUserId, result.get().getId());
        verify(userRepository, times(1)).findById(existingUserId);
    }

    @Test
    void givenBlankIdFilter_WhenGetUsers_ThenUseFindAllAndMapToDtos() {
        UserGetOptions options = new UserGetOptions();
        options.setId("");

        User firstUser = new User();
        firstUser.setId("s001");
        User secondUser = new User();
        secondUser.setId("t001");
        List<User> users = List.of(firstUser, secondUser);

        List<AuthUserDto> expectedDtos = List.of(
                AuthUserDto.builder().id("s001").reviewBlocked(false).build(),
                AuthUserDto.builder().id("t001").reviewBlocked(false).build());

        when(userRepository.findAll()).thenReturn(users);
        when(userMapper.toDtoList(users)).thenReturn(expectedDtos);
        List<AuthUserDto> result = userService.getUsers(options);
        assertEquals(expectedDtos, result);
        verify(userRepository, times(1)).findAll();
        verify(userRepository, never()).findByIdContaining(any());
        verify(userMapper, times(1)).toDtoList(users);
    }

    @Test
    void givenIdFilter_WhenGetUsers_ThenUseFindByIdContainingAndMapToDtos() {
        UserGetOptions options = new UserGetOptions();
        options.setId("ab");
        User user = new User();
        user.setId("abc123");
        List<User> users = List.of(user);
        List<AuthUserDto> expectedDtos = List
                .of(AuthUserDto.builder().id("abc123").reviewBlocked(false).build());
        when(userRepository.findByIdContaining("ab")).thenReturn(users);
        when(userMapper.toDtoList(users)).thenReturn(expectedDtos);
        List<AuthUserDto> result = userService.getUsers(options);
        assertEquals(expectedDtos, result);
        verify(userRepository, times(1)).findByIdContaining("ab");
        verify(userRepository, never()).findAll();
        verify(userMapper, times(1)).toDtoList(users);
    }

    @Test
    void givenNewUserIdAndValidRoleAndSchool_WhenCreateUser_ThenSaveAndReturnUser() {
        String newUserId = "newuser";
        RoleEntity mockRole = new RoleEntity();
        mockRole.setRole(edu.ap.bib_backend.jpa.enums.Role.STUDENT);
        School mockSchool = createSchool();
        when(userRepository.existsById(newUserId)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(newUserId);
            user.setRole(mockRole);
            user.setSchool(mockSchool);
            return user;
        });
        User result = userService.createUser(newUserId, mockRole, mockSchool, createCampus());
        assertNotNull(result);
        assertEquals(newUserId, result.getId());
        assertEquals(mockRole, result.getRole());
        assertEquals(mockSchool, result.getSchool());
        verify(userRepository, times(1)).existsById(newUserId);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void givenExistingUserIdAndAnyRoleAndSchool_WhenCreateUser_ThenThrowRuntimeException() {
        String existingUserId = "existinguser";
        RoleEntity mockRole = new RoleEntity();
        mockRole.setRole(edu.ap.bib_backend.jpa.enums.Role.STUDENT);
        School mockSchool = createSchool();
        when(userRepository.existsById(existingUserId)).thenReturn(true);
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.createUser(existingUserId, mockRole, mockSchool, createCampus()));
        assertEquals("Gebruiker bestaat al", exception.getMessage());
        verify(userRepository, times(1)).existsById(existingUserId);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void givenValidUserIdAndNullRoleAndSchool_WhenCreateUser_ThenSaveUserWithNullRole() {
        String validUserId = "validuser";
        RoleEntity nullRole = null;
        School mockSchool = createSchool();
        when(userRepository.existsById(validUserId)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(validUserId);
            user.setRole(nullRole);
            user.setSchool(mockSchool);
            return user;
        });
        User result = userService.createUser(validUserId, nullRole, mockSchool, createCampus());
        assertNotNull(result);
        assertEquals(validUserId, result.getId());
        assertNull(result.getRole());
        assertEquals(mockSchool, result.getSchool());
        verify(userRepository, times(1)).existsById(validUserId);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void givenNullSchool_WhenCreateUser_ThenThrowNullPointerException() {
        String validUserId = "validuser";
        RoleEntity role = new RoleEntity();
        when(userRepository.existsById(validUserId)).thenReturn(false);
        NullPointerException exception = assertThrows(NullPointerException.class,
                () -> userService.createUser(validUserId, role, null, createCampus()));
        assertEquals("school mag niet null zijn", exception.getMessage());
        verify(userRepository, times(1)).existsById(validUserId);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void givenValidUserIdAndBlockedTrue_WhenSetReviewBlocked_ThenReturnUpdatedAuthUserDto() {
        String userId = "user123";
        User user = new User();
        user.setId(userId);
        user.setReviewBlocked(false);
        User updatedUser = new User();
        updatedUser.setId(userId);
        updatedUser.setReviewBlocked(true);
        AuthUserDto expectedDto = AuthUserDto.builder().id(userId).reviewBlocked(true).build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);
        when(userMapper.toDto(updatedUser)).thenReturn(expectedDto);
        AuthUserDto result = userService.setReviewBlocked(userId, true);
        assertNotNull(result);
        assertTrue(result.reviewBlocked());
        assertEquals(userId, result.id());
        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, times(1)).save(any(User.class));
        verify(userMapper, times(1)).toDto(updatedUser);
    }

    @Test
    void givenValidUserIdAndBlockedFalse_WhenSetReviewBlocked_ThenReturnUpdatedAuthUserDto() {
        String userId = "user456";
        User user = new User();
        user.setId(userId);
        user.setReviewBlocked(true);
        User updatedUser = new User();
        updatedUser.setId(userId);
        updatedUser.setReviewBlocked(false);
        AuthUserDto expectedDto = AuthUserDto.builder().id(userId).reviewBlocked(false).build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);
        when(userMapper.toDto(updatedUser)).thenReturn(expectedDto);
        AuthUserDto result = userService.setReviewBlocked(userId, false);
        assertNotNull(result);
        assertFalse(result.reviewBlocked());
        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, times(1)).save(any(User.class));
        verify(userMapper, times(1)).toDto(updatedUser);
    }

    private School createSchool() {
        School school = new School();
        school.setId(1L);
        school.setDomainName("aphogeschool.smartschool.be");
        school.setName("AP Hogeschool");
        return school;
    }

    private Campus createCampus() {
        Campus campus = new Campus();
        campus.setId(1L);
        campus.setName("Test Campus");
        return campus;
    }
}
