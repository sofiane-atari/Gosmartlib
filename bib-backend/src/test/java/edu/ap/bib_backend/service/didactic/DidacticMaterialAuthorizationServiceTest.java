package edu.ap.bib_backend.service.didactic;

import edu.ap.bib_backend.jpa.entity.lessontips.LessonTip;
import edu.ap.bib_backend.service.lessontips.LessonTipAuthorizationService;
import edu.ap.bib_backend.service.user.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class DidacticMaterialAuthorizationServiceTest {

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private LessonTipAuthorizationService didacticMaterialAuthorizationService;

    @Test
    void givenAdminUser_whenAssertCanModify_thenAllowModification() {
        // Arrange
        LessonTip material = createMaterial("owner-1");

        given(currentUserService.extractCurrentUserId(authentication)).willReturn("admin-1");
        doReturn(authorities("ROLE_ADMIN")).when(authentication).getAuthorities();

        // Act
        assertDoesNotThrow(() -> didacticMaterialAuthorizationService.assertCanModify(material, authentication));

        // Assert
    }

    @Test
    void givenOwnerTeacher_whenAssertCanModify_thenAllowModification() {
        // Arrange
        LessonTip material = createMaterial("teacher-1");

        given(currentUserService.extractCurrentUserId(authentication)).willReturn("teacher-1");
        doReturn(authorities("ROLE_TEACHER")).when(authentication).getAuthorities();

        // Act
        assertDoesNotThrow(() -> didacticMaterialAuthorizationService.assertCanModify(material, authentication));

        // Assert
    }

    @Test
    void givenOwnerLibrarian_whenAssertCanModify_thenAllowModification() {
        // Arrange
        LessonTip material = createMaterial("librarian-1");

        given(currentUserService.extractCurrentUserId(authentication)).willReturn("librarian-1");
        doReturn(authorities("ROLE_LIBRARIAN")).when(authentication).getAuthorities();

        // Act
        assertDoesNotThrow(() -> didacticMaterialAuthorizationService.assertCanModify(material, authentication));

        // Assert
    }

    @Test
    void givenNonOwnerTeacher_whenAssertCanModify_thenThrowAccessDeniedException() {
        // Arrange
        LessonTip material = createMaterial("teacher-owner");

        given(currentUserService.extractCurrentUserId(authentication)).willReturn("teacher-other");
        doReturn(authorities("ROLE_TEACHER")).when(authentication).getAuthorities();

        // Act
        assertThrows(
                AccessDeniedException.class,
                () -> didacticMaterialAuthorizationService.assertCanModify(material, authentication)
        );

        // Assert
    }

    @Test
    void givenOwnerWithoutTeacherLibrarianOrAdminRole_whenAssertCanModify_thenThrowAccessDeniedException() {
        // Arrange
        LessonTip material = createMaterial("student-1");

        given(currentUserService.extractCurrentUserId(authentication)).willReturn("student-1");
        doReturn(authorities("ROLE_STUDENT")).when(authentication).getAuthorities();

        // Act
        assertThrows(
                AccessDeniedException.class,
                () -> didacticMaterialAuthorizationService.assertCanModify(material, authentication)
        );

        // Assert
    }

    private LessonTip createMaterial(String createdByUserId) {
        LessonTip material = new LessonTip();
        material.setCreatedByUserId(createdByUserId);
        return material;
    }

    private Collection<GrantedAuthority> authorities(String... values) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        for (String value : values) {
            authorities.add(() -> value);
        }

        return authorities;
    }
}