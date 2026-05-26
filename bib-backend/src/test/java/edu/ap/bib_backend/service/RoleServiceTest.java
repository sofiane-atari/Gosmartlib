package edu.ap.bib_backend.service;

import edu.ap.bib_backend.dto.AuthUserDto;
import edu.ap.bib_backend.exception.ResourceNotFoundException;
import edu.ap.bib_backend.jpa.entity.RoleEntity;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.enums.Role;
import edu.ap.bib_backend.jpa.repository.RoleEntityRepository;
import edu.ap.bib_backend.jpa.repository.SchoolRepository;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import edu.ap.bib_backend.mapper.UserMapper;
import edu.ap.bib_backend.service.oneroster.OneRosterClient;
import edu.ap.bib_backend.service.user.CurrentUserService;
import edu.ap.bib_backend.service.user.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private RoleEntityRepository roleEntityRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private TokenStorageService tokenStorageService;

    @Mock
    private OneRosterClient oneRosterClient;

    @Mock
    private UserService userService;

    @Mock
    private SchoolRepository schoolRepository;

    @InjectMocks
    private RoleService roleService;

    @Test
    void Given_EnglishRoleNameUpperCase_When_GetRoleEntity_Then_ReturnsExistingRoleEntity() {
        // Arrange
        String roleName = "ADMIN";
        RoleEntity mockRoleEntity = new RoleEntity();
        mockRoleEntity.setRole(Role.ADMIN);
        when(roleEntityRepository.findByRole(Role.ADMIN)).thenReturn(Optional.of(mockRoleEntity));

        // Act
        RoleEntity result = roleService.getRoleEntity(roleName);

        // Assert
        assertEquals(mockRoleEntity, result);
        verify(roleEntityRepository, times(1)).findByRole(Role.ADMIN);
        verify(roleEntityRepository, never()).save(any(RoleEntity.class));
    }

    @Test
    void Given_EnglishRoleNameLowerCase_When_GetRoleEntity_Then_ReturnsExistingRoleEntity() {
        // Arrange
        String roleName = "student";
        RoleEntity mockRoleEntity = new RoleEntity();
        mockRoleEntity.setRole(Role.STUDENT);
        when(roleEntityRepository.findByRole(Role.STUDENT)).thenReturn(Optional.of(mockRoleEntity));

        // Act
        RoleEntity result = roleService.getRoleEntity(roleName);

        // Assert
        assertEquals(mockRoleEntity, result);
        verify(roleEntityRepository, times(1)).findByRole(Role.STUDENT);
        verify(roleEntityRepository, never()).save(any(RoleEntity.class));
    }

    @Test
    void Given_DutchRoleNameLeerling_When_GetRoleEntity_Then_ReturnsStudentRoleEntity() {
        // Arrange
        String roleName = "leerling";
        RoleEntity mockRoleEntity = new RoleEntity();
        mockRoleEntity.setRole(Role.STUDENT);
        when(roleEntityRepository.findByRole(Role.STUDENT)).thenReturn(Optional.of(mockRoleEntity));

        // Act
        RoleEntity result = roleService.getRoleEntity(roleName);

        // Assert
        assertEquals(Role.STUDENT, result.getRole());
        verify(roleEntityRepository, times(1)).findByRole(Role.STUDENT);
        verify(roleEntityRepository, never()).save(any(RoleEntity.class));
    }

    @Test
    void Given_DutchRoleNameLeerkracht_When_GetRoleEntity_Then_ReturnsTeacherRoleEntity() {
        // Arrange
        String roleName = "leerkracht";
        RoleEntity mockRoleEntity = new RoleEntity();
        mockRoleEntity.setRole(Role.TEACHER);
        when(roleEntityRepository.findByRole(Role.TEACHER)).thenReturn(Optional.of(mockRoleEntity));

        // Act
        RoleEntity result = roleService.getRoleEntity(roleName);

        // Assert
        assertEquals(Role.TEACHER, result.getRole());
        verify(roleEntityRepository, times(1)).findByRole(Role.TEACHER);
        verify(roleEntityRepository, never()).save(any(RoleEntity.class));
    }

    @Test
    void Given_RoleNotFoundInRepo_When_GetRoleEntity_Then_CreatesRoleEntity() {
        // Arrange
        String roleName = "ADMIN";
        RoleEntity newRoleEntity = new RoleEntity();
        newRoleEntity.setRole(Role.ADMIN);
        when(roleEntityRepository.findByRole(Role.ADMIN)).thenReturn(Optional.empty());
        when(roleEntityRepository.save(any(RoleEntity.class))).thenReturn(newRoleEntity);

        // Act
        RoleEntity result = roleService.getRoleEntity(roleName);

        // Assert
        assertEquals(Role.ADMIN, result.getRole());
        verify(roleEntityRepository, times(1)).findByRole(Role.ADMIN);
        verify(roleEntityRepository, times(1)).save(argThat(role -> role.getRole() == Role.ADMIN));
    }

    @Test
    void Given_NullRoleName_When_GetRoleEntity_Then_ReturnsStudentRoleEntity() {
        // Arrange
        RoleEntity mockRoleEntity = new RoleEntity();
        mockRoleEntity.setRole(Role.STUDENT);
        when(roleEntityRepository.findByRole(Role.STUDENT)).thenReturn(Optional.of(mockRoleEntity));

        // Act
        RoleEntity result = roleService.getRoleEntity(null);

        // Assert
        assertEquals(Role.STUDENT, result.getRole());
        verify(roleEntityRepository, times(1)).findByRole(Role.STUDENT);
        verify(roleEntityRepository, never()).save(any(RoleEntity.class));
    }

    @Test
    void Given_InvalidRoleName_When_GetRoleEntity_Then_ThrowsIllegalArgumentException() {
        // Arrange
        String invalidRoleName = "invalid";

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> roleService.getRoleEntity(invalidRoleName));

        // Assert
        assertEquals("The ingegeven rol is ongeldig: invalid", exception.getMessage());
        verifyNoInteractions(roleEntityRepository);
    }

    @Test
    void Given_LibrarianAuthentication_When_AssignLibrarian_Then_PromotesTeacherAndRevokesToken() {
        // Arrange
        Authentication authentication = mock(Authentication.class);
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_LIBRARIAN"));
        when(authentication.getAuthorities()).thenAnswer(invocation -> (Collection<? extends GrantedAuthority>) authorities);

        School school = new School();
        school.setId(1L);
        school.setDomainName("test.smartschool.be");

        User currentUser = new User();
        currentUser.setId("current-user");
        currentUser.setSchool(school);

        User targetUser = new User();
        targetUser.setId("target-user");
        targetUser.setSchool(school);

        RoleEntity teacherRole = new RoleEntity();
        teacherRole.setRole(Role.TEACHER);
        targetUser.setRole(teacherRole);

        RoleEntity librarianRole = new RoleEntity();
        librarianRole.setRole(Role.LIBRARIAN);

        when(currentUserService.getCurrentUser(authentication)).thenReturn(currentUser);
        when(userRepository.findByIdAndSchoolId("target-user", 1L)).thenReturn(Optional.of(targetUser));
        when(roleEntityRepository.findByRole(Role.LIBRARIAN)).thenReturn(Optional.of(librarianRole));
        when(userRepository.save(targetUser)).thenReturn(targetUser);
        when(userMapper.toDto(targetUser)).thenReturn(AuthUserDto.builder().id("target-user").build());

        // Act
        AuthUserDto result = roleService.assignLibrarian(authentication, "target-user", null);

        // Assert
        assertEquals("target-user", result.id());
        assertEquals(Role.LIBRARIAN, targetUser.getRole().getRole());
        verify(tokenStorageService).revokeToken("target-user");
        verify(tokenStorageService, never()).replaceToken(any(), any(), any(), any());
    }

    @Test
    void Given_LibrarianWithSchoolCode_When_AssignLibrarian_Then_ThrowsIllegalArgumentException() {
        // Arrange
        Authentication authentication = mock(Authentication.class);
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_LIBRARIAN"));
        when(authentication.getAuthorities()).thenAnswer(invocation -> (Collection<? extends GrantedAuthority>) authorities);

        School school = new School();
        school.setId(1L);
        school.setDomainName("test.smartschool.be");

        User currentUser = new User();
        currentUser.setSchool(school);
        when(currentUserService.getCurrentUser(authentication)).thenReturn(currentUser);

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> roleService.assignLibrarian(authentication, "target-user", "other.smartschool.be"));

        // Assert
        assertEquals("Alleen beheerders mogen een schoolcode opgeven.", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = { "ROLE_STUDENT", "ROLE_TEACHER" })
    void Given_NonPrivilegedAuthentication_When_AssignLibrarian_Then_ThrowsIllegalState(String role) {
        // Arrange
        Authentication authentication = mock(Authentication.class);
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(role));
        when(authentication.getAuthorities()).thenAnswer(invocation -> (Collection<? extends GrantedAuthority>) authorities);

        // Act
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> roleService.assignLibrarian(authentication, "target-user", null));

        // Assert
        assertEquals("Alleen bibliothecarissen en admins kunnen deze actie uitvoeren.", exception.getMessage());
    }

    @Test
    void Given_NonTeacherTarget_When_AssignLibrarian_Then_ThrowsIllegalArgumentException() {
        // Arrange
        Authentication authentication = mock(Authentication.class);
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        when(authentication.getAuthorities()).thenAnswer(invocation -> (Collection<? extends GrantedAuthority>) authorities);

        School school = new School();
        school.setId(1L);
        school.setDomainName("test.smartschool.be");

        User currentUser = new User();
        currentUser.setSchool(school);

        User targetUser = new User();
        targetUser.setId("target-user");
        targetUser.setSchool(school);

        RoleEntity adminRole = new RoleEntity();
        adminRole.setRole(Role.ADMIN);
        targetUser.setRole(adminRole);

        when(currentUserService.getCurrentUser(authentication)).thenReturn(currentUser);
        when(schoolRepository.findByDomainName("test.smartschool.be")).thenReturn(Optional.of(school));
        when(userRepository.findByIdAndSchoolId("target-user", 1L)).thenReturn(Optional.of(targetUser));

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> roleService.assignLibrarian(authentication, "target-user", "test.smartschool.be"));

        // Assert
        assertEquals("Alleen leerkrachten kunnen gepromoveerd worden tot bibliothecaris.", exception.getMessage());
    }

    @Test
    void Given_AdminAuthentication_When_RevokeLibrarian_Then_DemotesLibrarianAndRevokesToken() {
        // Arrange
        Authentication authentication = mock(Authentication.class);
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        when(authentication.getAuthorities()).thenAnswer(invocation -> (Collection<? extends GrantedAuthority>) authorities);

        School school = new School();
        school.setId(1L);
        school.setDomainName("test.smartschool.be");

        User currentUser = new User();
        currentUser.setSchool(school);

        User targetUser = new User();
        targetUser.setId("target-user");
        targetUser.setSchool(school);

        RoleEntity librarianRole = new RoleEntity();
        librarianRole.setRole(Role.LIBRARIAN);
        targetUser.setRole(librarianRole);

        RoleEntity teacherRole = new RoleEntity();
        teacherRole.setRole(Role.TEACHER);

        when(currentUserService.getCurrentUser(authentication)).thenReturn(currentUser);
        when(schoolRepository.findByDomainName("test.smartschool.be")).thenReturn(Optional.of(school));
        when(userRepository.findByIdAndSchoolId("target-user", 1L)).thenReturn(Optional.of(targetUser));
        when(roleEntityRepository.findByRole(Role.TEACHER)).thenReturn(Optional.of(teacherRole));
        when(userRepository.save(targetUser)).thenReturn(targetUser);
        when(userMapper.toDto(targetUser)).thenReturn(AuthUserDto.builder().id("target-user").build());

        // Act
        AuthUserDto result = roleService.revokeLibrarian(authentication, "target-user", "test.smartschool.be");

        // Assert
        assertEquals("target-user", result.id());
        assertEquals(Role.TEACHER, targetUser.getRole().getRole());
        verify(tokenStorageService).revokeToken("target-user");
        verify(tokenStorageService, never()).replaceToken(any(), any(), any(), any());
    }

    @Test
    void Given_NotFoundUser_When_RevokeLibrarian_Then_ThrowsResourceNotFoundException() {
        // Arrange
        Authentication authentication = mock(Authentication.class);
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        when(authentication.getAuthorities()).thenAnswer(invocation -> (Collection<? extends GrantedAuthority>) authorities);
        User currentUser = new User();
        School school = new School();
        school.setId(1L);
        school.setDomainName("test.smartschool.be");
        currentUser.setSchool(school);
        when(currentUserService.getCurrentUser(authentication)).thenReturn(currentUser);
        when(schoolRepository.findByDomainName("test.smartschool.be")).thenReturn(Optional.of(school));
        when(userRepository.findByIdAndSchoolId("missing-user", 1L)).thenReturn(Optional.empty());

        // Act
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> roleService.revokeLibrarian(authentication, "missing-user", "test.smartschool.be"));

        // Assert
        assertTrue(exception.getMessage().contains("Gebruiker niet gevonden"));
    }

    @Test
    void Given_AdminWithNullSchoolCode_When_AssignAdmin_Then_ThrowsIllegalArgumentException() {
        // Arrange
        Authentication authentication = mock(Authentication.class);
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        when(authentication.getAuthorities()).thenAnswer(invocation -> (Collection<? extends GrantedAuthority>) authorities);

        User currentUser = new User();
        when(currentUserService.getCurrentUser(authentication)).thenReturn(currentUser);

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> roleService.assignAdmin(authentication, "target-user", null));

        // Assert
        assertEquals("Schoolcode is verplicht voor beheerders.", exception.getMessage());
    }

    @Test
    void Given_AdminWithSchoolCode_When_AssignLibrarian_Then_UsesSchoolScopedQuery() {
        // Arrange
        Authentication authentication = mock(Authentication.class);
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        when(authentication.getAuthorities()).thenAnswer(invocation -> (Collection<? extends GrantedAuthority>) authorities);

        School school = new School();
        school.setId(1L);
        school.setDomainName("test.smartschool.be");

        User currentUser = new User();
        currentUser.setSchool(school);

        User targetUser = new User();
        targetUser.setId("target-user");
        targetUser.setSchool(school);

        RoleEntity teacherRole = new RoleEntity();
        teacherRole.setRole(Role.TEACHER);
        targetUser.setRole(teacherRole);

        RoleEntity librarianRole = new RoleEntity();
        librarianRole.setRole(Role.LIBRARIAN);

        when(currentUserService.getCurrentUser(authentication)).thenReturn(currentUser);
        when(schoolRepository.findByDomainName("test.smartschool.be")).thenReturn(Optional.of(school));
        when(userRepository.findByIdAndSchoolId("target-user", 1L)).thenReturn(Optional.of(targetUser));
        when(roleEntityRepository.findByRole(Role.LIBRARIAN)).thenReturn(Optional.of(librarianRole));
        when(userRepository.save(targetUser)).thenReturn(targetUser);
        when(userMapper.toDto(targetUser)).thenReturn(AuthUserDto.builder().id("target-user").build());

        // Act
        AuthUserDto result = roleService.assignLibrarian(authentication, "target-user", "test.smartschool.be");

        // Assert
        assertEquals("target-user", result.id());
        assertEquals(Role.LIBRARIAN, targetUser.getRole().getRole());
        verify(userRepository).findByIdAndSchoolId("target-user", 1L);
        verify(tokenStorageService).revokeToken("target-user");
    }
}
