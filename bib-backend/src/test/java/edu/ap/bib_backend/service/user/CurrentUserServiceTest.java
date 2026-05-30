package edu.ap.bib_backend.service.user;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import edu.ap.bib_backend.exception.ResourceNotFoundException;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.repository.SchoolRepository;
import edu.ap.bib_backend.jpa.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class CurrentUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SchoolRepository schoolRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private CurrentUserService currentUserService;

    @Test
    void Given_AdminRole_When_IsAdmin_Then_ReturnsTrue() {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        when(authentication.getAuthorities())
                .thenAnswer(invocation -> (Collection<? extends GrantedAuthority>) authorities);

        boolean result = currentUserService.isAdmin(authentication);

        assertTrue(result);
    }

    @Test
    void Given_LibrarianRole_When_IsAdmin_Then_ReturnsFalse() {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_LIBRARIAN"));
        when(authentication.getAuthorities())
                .thenAnswer(invocation -> (Collection<? extends GrantedAuthority>) authorities);

        boolean result = currentUserService.isAdmin(authentication);

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

        when(authentication.getName()).thenReturn("user123");
        when(userRepository.findById("user123")).thenReturn(Optional.of(user));

        School result = currentUserService.resolveSchool(null, authentication);

        assertEquals(1L, result.getId());
        assertFalse(result.getCatalogVisible());
    }

    @Test
    void Given_SameSchool_When_ResolveSchool_Then_ReturnsOwnSchoolRegardlessOfVisibility() {
        School userSchool = schoolEntity(1L, "ap.be", "AP");
        userSchool.setCatalogVisible(false);
        User user = new User();
        user.setSchool(userSchool);

        when(authentication.getName()).thenReturn("user123");
        when(userRepository.findById("user123")).thenReturn(Optional.of(user));

        School result = currentUserService.resolveSchool(1L, authentication);

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

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_TEACHER"));

        when(authentication.getName()).thenReturn("user123");
        when(userRepository.findById("user123")).thenReturn(Optional.of(user));
        when(authentication.getAuthorities())
                .thenAnswer(invocation -> (Collection<? extends GrantedAuthority>) authorities);
        when(schoolRepository.findById(2L)).thenReturn(Optional.of(otherSchool));

        School result = currentUserService.resolveSchool(2L, authentication);

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

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_TEACHER"));

        when(authentication.getName()).thenReturn("user123");
        when(userRepository.findById("user123")).thenReturn(Optional.of(user));
        when(authentication.getAuthorities())
                .thenAnswer(invocation -> (Collection<? extends GrantedAuthority>) authorities);
        when(schoolRepository.findById(2L)).thenReturn(Optional.of(otherSchool));

        assertThrows(AccessDeniedException.class,
                () -> currentUserService.resolveSchool(2L, authentication));
    }

    @Test
    void Given_OtherSchoolNotFound_When_ResolveSchool_Then_ThrowsResourceNotFoundException() {
        School userSchool = schoolEntity(1L, "own.be", "Own");
        userSchool.setCatalogVisible(true);
        User user = new User();
        user.setSchool(userSchool);

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_TEACHER"));

        when(authentication.getName()).thenReturn("user123");
        when(userRepository.findById("user123")).thenReturn(Optional.of(user));
        when(authentication.getAuthorities())
                .thenAnswer(invocation -> (Collection<? extends GrantedAuthority>) authorities);
        when(schoolRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> currentUserService.resolveSchool(999L, authentication));
    }

    private School schoolEntity(Long id, String domainName, String name) {
        School school = new School();
        school.setId(id);
        school.setDomainName(domainName);
        school.setName(name);
        return school;
    }
}
