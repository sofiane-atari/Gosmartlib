package edu.ap.bib_backend.service;

import edu.ap.bib_backend.dto.AuthUserDto;
import edu.ap.bib_backend.exception.ResourceNotFoundException;
import edu.ap.bib_backend.jpa.entity.AuthSession;
import edu.ap.bib_backend.jpa.entity.RoleEntity;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.enums.Role;
import edu.ap.bib_backend.jpa.repository.RoleEntityRepository;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import edu.ap.bib_backend.mapper.UserMapper;
import edu.ap.bib_backend.service.user.CurrentUserService;
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

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    private JwtService jwtService;

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
    void Given_LibrarianAuthentication_When_AssignLibrarian_Then_PromotesTeacherAndReplacesToken() {
        // Arrange
        Authentication authentication = mock(Authentication.class);
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_LIBRARIAN"));
        when(authentication.getAuthorities()).thenAnswer(invocation -> (Collection<? extends GrantedAuthority>) authorities);

        School school = new School();
        school.setId(1L);

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
        when(userRepository.findById("target-user")).thenReturn(Optional.of(targetUser));
        when(roleEntityRepository.findByRole(Role.LIBRARIAN)).thenReturn(Optional.of(librarianRole));
        when(userRepository.save(targetUser)).thenReturn(targetUser);

        AuthSession session = new AuthSession();
        session.setJwtToken("old-token");
        when(tokenStorageService.getSession("target-user")).thenReturn(Optional.of(session));
        when(jwtService.extractClaim("old-token", edu.ap.bib_backend.service.smartschool.SmartschoolOAuth2UserService.CLAIM_CLASS_CODE))
                .thenReturn("1A");
        when(jwtService.extractClaim("old-token", edu.ap.bib_backend.service.smartschool.SmartschoolOAuth2UserService.CLAIM_YEAR_CODE))
                .thenReturn("2024");
        when(jwtService.extractClaim("old-token", edu.ap.bib_backend.service.smartschool.SmartschoolOAuth2UserService.CLAIM_SCHOOL_CODE))
                .thenReturn("SCHOOL");
        when(jwtService.generateToken(eq("target-user"), eq("ROLE_LIBRARIAN"), any())).thenReturn("new-token");
        when(jwtService.extractExpirationInstant("new-token")).thenReturn(Instant.now());
        when(userMapper.toDto(targetUser)).thenReturn(AuthUserDto.builder().id("target-user").build());

        // Act
        AuthUserDto result = roleService.assignLibrarian(authentication, "target-user");

        // Assert
        assertEquals("target-user", result.id());
        assertEquals(Role.LIBRARIAN, targetUser.getRole().getRole());
        verify(tokenStorageService).replaceToken(eq("target-user"), eq("new-token"), eq("ROLE_LIBRARIAN"), any());
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
                () -> roleService.assignLibrarian(authentication, "target-user"));

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

        User currentUser = new User();
        currentUser.setSchool(school);

        User targetUser = new User();
        targetUser.setId("target-user");
        targetUser.setSchool(school);

        RoleEntity adminRole = new RoleEntity();
        adminRole.setRole(Role.ADMIN);
        targetUser.setRole(adminRole);

        when(currentUserService.getCurrentUser(authentication)).thenReturn(currentUser);
        when(userRepository.findById("target-user")).thenReturn(Optional.of(targetUser));

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> roleService.assignLibrarian(authentication, "target-user"));

        // Assert
        assertEquals("Alleen leerkrachten kunnen gepromoveerd worden tot bibliothecaris.", exception.getMessage());
    }

    @Test
    void Given_DifferentSchoolTarget_When_AssignLibrarian_Then_ThrowsIllegalStateException() {
        // Arrange
        Authentication authentication = mock(Authentication.class);
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        when(authentication.getAuthorities()).thenAnswer(invocation -> (Collection<? extends GrantedAuthority>) authorities);

        School currentSchool = new School();
        currentSchool.setId(1L);
        School targetSchool = new School();
        targetSchool.setId(2L);

        User currentUser = new User();
        currentUser.setSchool(currentSchool);

        User targetUser = new User();
        targetUser.setSchool(targetSchool);

        RoleEntity teacherRole = new RoleEntity();
        teacherRole.setRole(Role.TEACHER);
        targetUser.setRole(teacherRole);

        when(currentUserService.getCurrentUser(authentication)).thenReturn(currentUser);
        when(userRepository.findById("target-user")).thenReturn(Optional.of(targetUser));

        // Act
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> roleService.assignLibrarian(authentication, "target-user"));

        // Assert
        assertEquals("Je kunt alleen leerkrachten van je eigen school promoten tot bibliothecaris.",
                exception.getMessage());
    }

    @Test
    void Given_AdminAuthentication_When_RevokeLibrarian_Then_DemotesLibrarianAndReplacesToken() {
        // Arrange
        Authentication authentication = mock(Authentication.class);
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        when(authentication.getAuthorities()).thenAnswer(invocation -> (Collection<? extends GrantedAuthority>) authorities);

        School school = new School();
        school.setId(1L);

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
        when(userRepository.findById("target-user")).thenReturn(Optional.of(targetUser));
        when(roleEntityRepository.findByRole(Role.TEACHER)).thenReturn(Optional.of(teacherRole));
        when(userRepository.save(targetUser)).thenReturn(targetUser);

        AuthSession session = new AuthSession();
        session.setJwtToken("old-token");
        when(tokenStorageService.getSession("target-user")).thenReturn(Optional.of(session));
        when(jwtService.extractClaim("old-token", edu.ap.bib_backend.service.smartschool.SmartschoolOAuth2UserService.CLAIM_CLASS_CODE))
                .thenReturn("1A");
        when(jwtService.extractClaim("old-token", edu.ap.bib_backend.service.smartschool.SmartschoolOAuth2UserService.CLAIM_YEAR_CODE))
                .thenReturn("2024");
        when(jwtService.extractClaim("old-token", edu.ap.bib_backend.service.smartschool.SmartschoolOAuth2UserService.CLAIM_SCHOOL_CODE))
                .thenReturn("SCHOOL");
        when(jwtService.generateToken(eq("target-user"), eq("ROLE_TEACHER"), any())).thenReturn("new-token");
        when(jwtService.extractExpirationInstant("new-token")).thenReturn(Instant.now());
        when(userMapper.toDto(targetUser)).thenReturn(AuthUserDto.builder().id("target-user").build());

        // Act
        AuthUserDto result = roleService.revokeLibrarian(authentication, "target-user");

        // Assert
        assertEquals("target-user", result.id());
        assertEquals(Role.TEACHER, targetUser.getRole().getRole());
        verify(tokenStorageService).replaceToken(eq("target-user"), eq("new-token"), eq("ROLE_TEACHER"), any());
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
        currentUser.setSchool(school);
        when(currentUserService.getCurrentUser(authentication)).thenReturn(currentUser);
        when(userRepository.findById("missing-user")).thenReturn(Optional.empty());

        // Act
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> roleService.revokeLibrarian(authentication, "missing-user"));

        // Assert
        assertTrue(exception.getMessage().contains("Gebruiker niet gevonden"));
    }
}
