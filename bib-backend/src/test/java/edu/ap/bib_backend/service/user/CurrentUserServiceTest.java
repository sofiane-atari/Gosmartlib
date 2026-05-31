package edu.ap.bib_backend.service.user;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;

import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.repository.SchoolRepository;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import edu.ap.bib_backend.service.ActiveSchoolContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import edu.ap.bib_backend.exception.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class CurrentUserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private SchoolRepository schoolRepository;
    @Mock
    private ActiveSchoolContext activeSchoolContext;

    @InjectMocks
    private CurrentUserService currentUserService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void givenNullAuthentication_whenExtractCurrentUserId_thenThrowsIllegalStateException() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> currentUserService.extractCurrentUserId(null));

        assertEquals("Geen geauthenticeerde gebruiker gevonden.", exception.getMessage());
    }

    @Test
    void givenAuthenticationWithNullName_whenExtractCurrentUserId_thenThrowsIllegalStateException() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(null);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> currentUserService.extractCurrentUserId(auth));

        assertEquals("Geen geauthenticeerde gebruiker gevonden.", exception.getMessage());
    }

    @Test
    void givenAuthenticationWithBlankName_whenExtractCurrentUserId_thenThrowsIllegalStateException() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("   ");

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> currentUserService.extractCurrentUserId(auth));

        assertEquals("Geen geauthenticeerde gebruiker gevonden.", exception.getMessage());
    }

    @Test
    void givenValidAuthentication_whenExtractCurrentUserId_thenReturnsUserId() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("user-123");

        String result = currentUserService.extractCurrentUserId(auth);

        assertEquals("user-123", result);
    }

    @Test
    void givenExistingUser_whenGetCurrentUser_thenReturnsUser() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("user-123");

        User user = new User();
        user.setId("user-123");
        when(userRepository.findById("user-123")).thenReturn(Optional.of(user));

        User result = currentUserService.getCurrentUser(auth);

        assertEquals("user-123", result.getId());
    }

    @Test
    void givenNonExistingUser_whenGetCurrentUser_thenThrowsIllegalStateException() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("missing-user");
        when(userRepository.findById("missing-user")).thenReturn(Optional.empty());

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> currentUserService.getCurrentUser(auth));

        assertTrue(exception.getMessage().contains("Gebruiker niet gevonden"));
    }

    @Test
    void givenUserWithSchool_whenGetCurrentUserSchool_thenReturnsSchool() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("user-123");

        School school = new School();
        school.setId(1L);
        school.setDomainName("test.smartschool.be");

        User user = new User();
        user.setId("user-123");
        user.setSchool(school);
        when(userRepository.findById("user-123")).thenReturn(Optional.of(user));

        School result = currentUserService.getCurrentUserSchool(auth);

        assertEquals(1L, result.getId());
        assertEquals("test.smartschool.be", result.getDomainName());
    }

    @Test
    void givenUserWithoutSchool_whenGetCurrentUserSchool_thenThrowsIllegalStateException() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("user-123");

        User user = new User();
        user.setId("user-123");
        user.setSchool(null);
        when(userRepository.findById("user-123")).thenReturn(Optional.of(user));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> currentUserService.getCurrentUserSchool(auth));

        assertEquals("Gebruiker heeft geen school gekoppeld.", exception.getMessage());
    }

    @Test
    void givenAdminWithActiveSchoolOverride_whenGetCurrentUserSchool_thenReturnsOverrideSchool() {
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn("admin-user");
        when(auth.getAuthorities()).thenReturn(
                (java.util.Collection) Collections.singleton(new SimpleGrantedAuthority("ROLE_ADMIN")));

        when(activeSchoolContext.getActiveSchoolId()).thenReturn(99L);

        School overrideSchool = new School();
        overrideSchool.setId(99L);
        overrideSchool.setDomainName("override.smartschool.be");
        when(schoolRepository.findById(99L)).thenReturn(Optional.of(overrideSchool));

        School result = currentUserService.getCurrentUserSchool(auth);

        assertEquals(99L, result.getId());
        assertEquals("override.smartschool.be", result.getDomainName());
    }

    @Test
    void givenAdminWithoutDbUserAndWithoutOverride_whenGetCurrentUserSchool_thenThrowsWithHeaderMessage() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("admin-no-db");
        when(auth.getAuthorities()).thenReturn(
                (java.util.Collection) Collections.singleton(new SimpleGrantedAuthority("ROLE_ADMIN")));

        when(activeSchoolContext.getActiveSchoolId()).thenReturn(null);
        lenient().when(userRepository.findById("admin-no-db")).thenReturn(Optional.empty());

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> currentUserService.getCurrentUserSchool(auth));

        assertTrue(exception.getMessage().contains("X-Active-School-Id"));
    }

    @Test
    void givenAdminWithoutDbUserButWithOverride_whenGetCurrentUserSchool_thenReturnsOverrideSchool() {
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn("admin-no-db");
        when(auth.getAuthorities()).thenReturn(
                (java.util.Collection) Collections.singleton(new SimpleGrantedAuthority("ROLE_ADMIN")));

        when(activeSchoolContext.getActiveSchoolId()).thenReturn(42L);

        School overrideSchool = new School();
        overrideSchool.setId(42L);
        overrideSchool.setDomainName("override42.smartschool.be");
        when(schoolRepository.findById(42L)).thenReturn(Optional.of(overrideSchool));

        School result = currentUserService.getCurrentUserSchool(auth);

        assertEquals(42L, result.getId());
        assertEquals("override42.smartschool.be", result.getDomainName());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ROLE_LIBRARIAN", "ROLE_ADMIN"})
    void givenModeratorRole_whenIsModerator_thenReturnsTrue(String role) {
        Authentication auth = mock(Authentication.class);
        when(auth.getAuthorities()).thenReturn(
                (java.util.Collection) Collections.singleton(new SimpleGrantedAuthority(role)));

        boolean result = currentUserService.isModerator(auth);

        assertTrue(result);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ROLE_STUDENT", "ROLE_TEACHER"})
    void givenNonModeratorRole_whenIsModerator_thenReturnsFalse(String role) {
        Authentication auth = mock(Authentication.class);
        when(auth.getAuthorities()).thenReturn(
                (java.util.Collection) Collections.singleton(new SimpleGrantedAuthority(role)));

        boolean result = currentUserService.isModerator(auth);

        assertFalse(result);
    }

    @Test
    void givenNullAuthentication_whenIsModerator_thenReturnsFalse() {
        boolean result = currentUserService.isModerator(null);

        assertFalse(result);
    }

    @Test
    void givenNullAuthorities_whenIsModerator_thenReturnsFalse() {
        Authentication auth = mock(Authentication.class);
        when(auth.getAuthorities()).thenReturn(null);

        boolean result = currentUserService.isModerator(auth);

        assertFalse(result);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ROLE_TEACHER", "ROLE_LIBRARIAN", "ROLE_ADMIN"})
    void givenDidacticBookRole_whenCanSeeDidacticBooks_thenReturnsTrue(String role) {
        Authentication auth = mock(Authentication.class);
        when(auth.getAuthorities()).thenReturn(
                (java.util.Collection) Collections.singleton(new SimpleGrantedAuthority(role)));

        boolean result = currentUserService.canSeeDidacticBooks(auth);

        assertTrue(result);
    }

    @Test
    void givenStudentRole_whenCanSeeDidacticBooks_thenReturnsFalse() {
        Authentication auth = mock(Authentication.class);
        when(auth.getAuthorities()).thenReturn(
                (java.util.Collection) Collections.singleton(new SimpleGrantedAuthority("ROLE_STUDENT")));

        boolean result = currentUserService.canSeeDidacticBooks(auth);

        assertFalse(result);
    }

    @Test
    void givenNullAuthentication_whenCanSeeDidacticBooks_thenReturnsFalse() {
        boolean result = currentUserService.canSeeDidacticBooks(null);

        assertFalse(result);
    }

    @Test
    void Given_AdminRole_When_IsAdmin_Then_ReturnsTrue() {
        Authentication auth = mock(Authentication.class);
        when(auth.getAuthorities()).thenReturn(
                (java.util.Collection) Collections.singleton(new SimpleGrantedAuthority("ROLE_ADMIN")));

        boolean result = currentUserService.isAdmin(auth);

        assertTrue(result);
    }

    @Test
    void Given_LibrarianRole_When_IsAdmin_Then_ReturnsFalse() {
        Authentication auth = mock(Authentication.class);
        when(auth.getAuthorities()).thenReturn(
                (java.util.Collection) Collections.singleton(new SimpleGrantedAuthority("ROLE_LIBRARIAN")));

        boolean result = currentUserService.isAdmin(auth);

        assertFalse(result);
    }

    @Test
    void Given_NullAuthentication_When_IsAdmin_Then_ReturnsFalse() {
        boolean result = currentUserService.isAdmin(null);

        assertFalse(result);
    }

    @Test
    void Given_OwnSchoolWithoutCatalogVisible_When_ResolveSchool_Then_ReturnsOwnSchool() {
        School userSchool = schoolEntity(1L, "ap.be", "AP");
        userSchool.setCatalogVisible(false);
        User user = new User();
        user.setSchool(userSchool);
        Authentication auth = mock(Authentication.class);

        when(auth.getName()).thenReturn("user123");
        when(userRepository.findById("user123")).thenReturn(Optional.of(user));

        School result = currentUserService.resolveSchool(null, auth);

        assertEquals(1L, result.getId());
        assertFalse(result.getCatalogVisible());
    }

    @Test
    void Given_SameSchool_When_ResolveSchool_Then_ReturnsOwnSchoolRegardlessOfVisibility() {
        School userSchool = schoolEntity(1L, "ap.be", "AP");
        userSchool.setCatalogVisible(false);
        User user = new User();
        user.setSchool(userSchool);
        Authentication auth = mock(Authentication.class);

        when(auth.getName()).thenReturn("user123");
        when(userRepository.findById("user123")).thenReturn(Optional.of(user));

        School result = currentUserService.resolveSchool(1L, auth);

        assertEquals(1L, result.getId());
        assertEquals("AP", result.getName());
    }

    @Test
    void Given_OtherSchoolWithCatalogVisible_When_ResolveSchool_Then_ReturnsSchool() {
        School userSchool = schoolEntity(1L, "own.be", "Own");
        userSchool.setCatalogVisible(true);
        User user = new User();
        user.setSchool(userSchool);

        School otherSchool = schoolEntity(2L, "other.be", "Other");
        otherSchool.setCatalogVisible(true);

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("user123");
        when(userRepository.findById("user123")).thenReturn(Optional.of(user));
        when(auth.getAuthorities()).thenReturn(
                (java.util.Collection) Collections.singleton(new SimpleGrantedAuthority("ROLE_TEACHER")));
        when(schoolRepository.findById(2L)).thenReturn(Optional.of(otherSchool));

        School result = currentUserService.resolveSchool(2L, auth);

        assertEquals(2L, result.getId());
        assertTrue(result.getCatalogVisible());
    }

    @Test
    void Given_OtherSchoolWithCatalogNotVisible_When_ResolveSchool_Then_ThrowsAccessDeniedException() {
        School userSchool = schoolEntity(1L, "own.be", "Own");
        userSchool.setCatalogVisible(true);
        User user = new User();
        user.setSchool(userSchool);

        School otherSchool = schoolEntity(2L, "other.be", "Other");
        otherSchool.setCatalogVisible(false);

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("user123");
        when(userRepository.findById("user123")).thenReturn(Optional.of(user));
        when(auth.getAuthorities()).thenReturn(
                (java.util.Collection) Collections.singleton(new SimpleGrantedAuthority("ROLE_TEACHER")));
        when(schoolRepository.findById(2L)).thenReturn(Optional.of(otherSchool));

        assertThrows(AccessDeniedException.class,
                () -> currentUserService.resolveSchool(2L, auth));
    }

    @Test
    void Given_OtherSchoolNotFound_When_ResolveSchool_Then_ThrowsResourceNotFoundException() {
        School userSchool = schoolEntity(1L, "own.be", "Own");
        userSchool.setCatalogVisible(true);
        User user = new User();
        user.setSchool(userSchool);

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("user123");
        when(userRepository.findById("user123")).thenReturn(Optional.of(user));
        when(auth.getAuthorities()).thenReturn(
                (java.util.Collection) Collections.singleton(new SimpleGrantedAuthority("ROLE_TEACHER")));
        when(schoolRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> currentUserService.resolveSchool(999L, auth));
    }

    @Test
    void givenMatchingAuthority_whenCurrentUserHasAnyAuthority_thenReturnsTrue() {
        Authentication auth = mock(Authentication.class);
        when(auth.getAuthorities()).thenReturn(
                (java.util.Collection) Collections.singleton(new SimpleGrantedAuthority("ROLE_LIBRARIAN")));

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        boolean result = currentUserService.currentUserHasAnyAuthority("ROLE_LIBRARIAN", "ROLE_ADMIN");

        assertTrue(result);
    }

    @Test
    void givenNonMatchingAuthority_whenCurrentUserHasAnyAuthority_thenReturnsFalse() {
        Authentication auth = mock(Authentication.class);
        when(auth.getAuthorities()).thenReturn(
                (java.util.Collection) Collections.singleton(new SimpleGrantedAuthority("ROLE_STUDENT")));

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        boolean result = currentUserService.currentUserHasAnyAuthority("ROLE_LIBRARIAN", "ROLE_ADMIN");

        assertFalse(result);
    }

    @Test
    void givenNullSecurityContextAuthentication_whenCurrentUserHasAnyAuthority_thenReturnsFalse() {
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(null);
        SecurityContextHolder.setContext(securityContext);

        boolean result = currentUserService.currentUserHasAnyAuthority("ROLE_ADMIN");

        assertFalse(result);
    }

    private School schoolEntity(Long id, String domainName, String name) {
        School school = new School();
        school.setId(id);
        school.setDomainName(domainName);
        school.setName(name);
        return school;
    }
}