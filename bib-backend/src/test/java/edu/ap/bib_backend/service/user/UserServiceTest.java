package edu.ap.bib_backend.service.user;

import edu.ap.bib_backend.dto.AuthUserDto;
import edu.ap.bib_backend.dto.PageDTO;
import edu.ap.bib_backend.dto.TeacherStatsDTO;
import edu.ap.bib_backend.jpa.entity.Campus;
import edu.ap.bib_backend.jpa.entity.RoleEntity;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.enums.Role;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import edu.ap.bib_backend.jpa.repository.CampusRepository;
import edu.ap.bib_backend.jpa.repository.SchoolRepository;
import edu.ap.bib_backend.mapper.UserMapper;
import edu.ap.bib_backend.options.UserGetOptions;
import edu.ap.bib_backend.service.GroupService;
import edu.ap.bib_backend.service.review.ProfanityFilterService;
import edu.ap.bib_backend.service.TokenStorageService;
import edu.ap.bib_backend.service.oneroster.OneRosterClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.ArrayList;
import java.util.Collection;
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
    private ProfanityFilterService profanityFilterService;
    @Mock
    private GroupService groupService;
    @Mock
    private OneRosterClient oneRosterClient;
    @Mock
    private TokenStorageService tokenStorageService;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private SchoolRepository schoolRepository;
    @Mock
    private CampusRepository campusRepository;

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

        // Act
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.createUser(userId, new RoleEntity(), new School(), new Campus()));

        // Assert
        assertNotNull(exception.getMessage());
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
                "fullName", "Other User");
        when(oneRosterClient.fetchUsersWithBearer("test.smartschool.be", "token"))
                .thenReturn(List.of(rosterUser));

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

    @Test
    void Given_LibrarianAuthentication_When_GetTeacherStats_Then_ReturnsCounts() {
        // Arrange
        Authentication authentication = mock(Authentication.class);
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_LIBRARIAN"));
        when(authentication.getAuthorities())
                .thenAnswer(invocation -> (Collection<? extends GrantedAuthority>) authorities);
        when(currentUserService.extractCurrentUserId(authentication)).thenReturn(userId);

        School school = new School();
        school.setId(1L);
        school.setDomainName("school.smartschool.be");

        User currentUser = new User();
        currentUser.setId(userId);
        currentUser.setSchool(school);
        when(userRepository.findById(userId)).thenReturn(Optional.of(currentUser));

        when(tokenStorageService.getSmartschoolAccessToken(userId)).thenReturn("token");

        Map<String, Object> rosterUser1 = Map.of(
                "metadata", Map.of("smsc.legacyIdentifier", "t1"),
                "fullName", "Alice Teacher",
                "role", "teacher");
        Map<String, Object> rosterUser2 = Map.of(
                "metadata", Map.of("smsc.legacyIdentifier", "t2"),
                "fullName", "Bob Librarian",
                "role", "teacher");
        Map<String, Object> rosterUser3 = Map.of(
                "metadata", Map.of("smsc.legacyIdentifier", "t3"),
                "fullName", "Cara Admin",
                "role", "teacher");

        when(oneRosterClient.fetchUsersWithBearer("school.smartschool.be", "token"))
                .thenReturn(List.of(rosterUser1, rosterUser2, rosterUser3));

        RoleEntity librarianRole = new RoleEntity();
        librarianRole.setRole(Role.LIBRARIAN);
        User librarianUser = new User();
        librarianUser.setId("t2");
        librarianUser.setRole(librarianRole);

        RoleEntity adminRole = new RoleEntity();
        adminRole.setRole(Role.ADMIN);
        User adminUser = new User();
        adminUser.setId("t3");
        adminUser.setRole(adminRole);

        when(userRepository.findBySchoolId(1L)).thenReturn(List.of(librarianUser, adminUser));

        // Act
        TeacherStatsDTO result = userService.getTeacherStats(authentication);

        // Assert
        assertEquals(3, result.totalTeachers());
        assertEquals(1, result.activeTeachers());
        assertEquals(1, result.librarians());
        assertEquals(1, result.admins());
    }

    @Test
    void Given_LibrarianAuthentication_When_GetTeachers_Then_ReturnsPaginatedTeachers() {
        // Arrange
        Authentication authentication = mock(Authentication.class);
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_LIBRARIAN"));
        when(authentication.getAuthorities())
                .thenAnswer(invocation -> (Collection<? extends GrantedAuthority>) authorities);
        when(currentUserService.extractCurrentUserId(authentication)).thenReturn(userId);

        School school = new School();
        school.setId(1L);
        school.setDomainName("school.smartschool.be");

        User currentUser = new User();
        currentUser.setId(userId);
        currentUser.setSchool(school);
        when(userRepository.findById(userId)).thenReturn(Optional.of(currentUser));

        when(tokenStorageService.getSmartschoolAccessToken(userId)).thenReturn("token");

        Map<String, Object> rosterUser1 = Map.of(
                "metadata", Map.of("smsc.legacyIdentifier", "t1"),
                "fullName", "Alice Teacher",
                "role", "teacher",
                "email", "alice@school.be");
        Map<String, Object> rosterUser2 = Map.of(
                "metadata", Map.of("smsc.legacyIdentifier", "t2"),
                "fullName", "Bob Librarian",
                "role", "teacher");
        Map<String, Object> rosterUser3 = Map.of(
                "metadata", Map.of("smsc.legacyIdentifier", "t3"),
                "fullName", "Cara Admin",
                "role", "teacher");

        when(oneRosterClient.fetchUsersWithBearer("school.smartschool.be", "token"))
                .thenReturn(List.of(rosterUser1, rosterUser2, rosterUser3));

        RoleEntity librarianRole = new RoleEntity();
        librarianRole.setRole(Role.LIBRARIAN);
        User librarianUser = new User();
        librarianUser.setId("t2");
        librarianUser.setRole(librarianRole);

        RoleEntity adminRole = new RoleEntity();
        adminRole.setRole(Role.ADMIN);
        User adminUser = new User();
        adminUser.setId("t3");
        adminUser.setRole(adminRole);

        when(userRepository.findBySchoolId(1L)).thenReturn(List.of(librarianUser, adminUser));

        // Act
        PageDTO<AuthUserDto> result = userService.getTeachers(authentication, true, true, 0, 10, null);

        // Assert
        assertEquals(1, result.data().size());
        assertEquals("t1", result.data().get(0).id());
        assertEquals(1, result.meta().totalPages());
    }

    @Test
    void Given_SearchQuery_When_GetTeachers_Then_FiltersByNameOrEmail() {
        // Arrange
        Authentication authentication = mock(Authentication.class);
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_LIBRARIAN"));
        when(authentication.getAuthorities())
                .thenAnswer(invocation -> (Collection<? extends GrantedAuthority>) authorities);
        when(currentUserService.extractCurrentUserId(authentication)).thenReturn(userId);

        School school = new School();
        school.setId(1L);
        school.setDomainName("school.smartschool.be");

        User currentUser = new User();
        currentUser.setId(userId);
        currentUser.setSchool(school);
        when(userRepository.findById(userId)).thenReturn(Optional.of(currentUser));

        when(tokenStorageService.getSmartschoolAccessToken(userId)).thenReturn("token");

        Map<String, Object> rosterUser1 = Map.of(
                "metadata", Map.of("smsc.legacyIdentifier", "t1"),
                "fullName", "Alice Teacher",
                "role", "teacher",
                "email", "alice@school.be");
        Map<String, Object> rosterUser2 = Map.of(
                "metadata", Map.of("smsc.legacyIdentifier", "t2"),
                "fullName", "Bob Teacher",
                "role", "teacher",
                "email", "bob@school.be");

        when(oneRosterClient.fetchUsersWithBearer("school.smartschool.be", "token"))
                .thenReturn(List.of(rosterUser1, rosterUser2));

        when(userRepository.findBySchoolId(1L)).thenReturn(List.of());

        // Act
        PageDTO<AuthUserDto> result = userService.getTeachers(authentication, false, false, 0, 10, "alice");

        // Assert
        assertEquals(1, result.data().size());
        assertEquals("t1", result.data().get(0).id());
    }

    @Test
    void Given_NonPrivilegedAuthentication_When_GetTeachers_Then_ThrowsIllegalState() {
        // Arrange
        Authentication authentication = mock(Authentication.class);
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_STUDENT"));
        when(authentication.getAuthorities())
                .thenAnswer(invocation -> (Collection<? extends GrantedAuthority>) authorities);

        // Act
        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                () -> userService.getTeachers(authentication, false, false, 0, 10, null));

        // Assert
        assertEquals("Alleen bibliothecarissen en admins kunnen docenten bekijken.", exception.getMessage());
    }

    @Test
    void Given_LibrarianAndExternalSchool_When_SearchExternalUsers_Then_ReturnsAllowedRolesOnly() {
        // Arrange
        Authentication authentication = mock(Authentication.class);
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_LIBRARIAN"));
        when(authentication.getAuthorities())
                .thenAnswer(invocation -> (Collection<? extends GrantedAuthority>) authorities);

        School school = new School();
        school.setId(2L);
        school.setName("Externe school");
        school.setDomainName("extern.smartschool.be");
        when(schoolRepository.findById(2L)).thenReturn(Optional.of(school));

        School currentSchool = new School();
        currentSchool.setId(1L);
        User currentUser = new User();
        currentUser.setId(userId);
        currentUser.setSchool(currentSchool);
        when(currentUserService.extractCurrentUserId(authentication)).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(currentUser));

        Map<String, Object> teacher = Map.of(
                "metadata", Map.of("smsc.legacyIdentifier", "teacher-1"),
                "givenName", "Alice",
                "familyName", "Teacher",
                "role", "teacher");
        Map<String, Object> student = Map.of(
                "metadata", Map.of("smsc.legacyIdentifier", "student-1"),
                "givenName", "Bob",
                "familyName", "Student",
                "role", "student");
        Map<String, Object> localAdmin = Map.of(
                "metadata", Map.of("smsc.legacyIdentifier", "admin-1"),
                "givenName", "Cara",
                "familyName", "Admin",
                "role", "teacher");
        when(oneRosterClient.fetchUsers("extern.smartschool.be"))
                .thenReturn(List.of(teacher, student, localAdmin));

        RoleEntity adminRole = new RoleEntity();
        adminRole.setRole(Role.ADMIN);
        User adminUser = new User();
        adminUser.setId("admin-1");
        adminUser.setRole(adminRole);
        when(userRepository.findBySchoolId(2L)).thenReturn(List.of(adminUser));

        // Act
        List<AuthUserDto> result = userService.searchExternalUsers(authentication, 2L, null, null);

        // Assert
        assertEquals(2, result.size());
        assertEquals(List.of("teacher-1", "admin-1"), result.stream().map(AuthUserDto::id).toList());
        assertEquals(Role.ADMIN, result.get(1).role());
    }

    @Test
    void Given_NonPrivilegedAuthentication_When_SearchExternalUsers_Then_ThrowsIllegalState() {
        // Arrange
        Authentication authentication = mock(Authentication.class);
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_STUDENT"));
        when(authentication.getAuthorities())
                .thenAnswer(invocation -> (Collection<? extends GrantedAuthority>) authorities);

        // Act
        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                () -> userService.searchExternalUsers(authentication, 2L, null, null));

        // Assert
        assertEquals("Alleen bibliothecarissen en admins kunnen externe gebruikers zoeken.", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = { "teacher", "student", "administrator", "librarian" })
    void Given_RosterRole_When_GetUsers_Then_MapsRoleToDto(String rosterRole) {
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
                "metadata", Map.of("smsc.legacyIdentifier", "role-user"),
                "fullName", "Role User",
                "role", rosterRole);
        when(oneRosterClient.fetchUsersWithBearer("test.smartschool.be", "token"))
                .thenReturn(List.of(rosterUser));

        // Act
        List<AuthUserDto> result = userService.getUsers(null);

        // Assert
        assertEquals(1, result.size());
        assertNotNull(result.get(0).role());

        SecurityContextHolder.clearContext();
    }
}
