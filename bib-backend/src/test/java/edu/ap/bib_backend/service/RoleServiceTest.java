package edu.ap.bib_backend.service;

import edu.ap.bib_backend.jpa.entity.RoleEntity;
import edu.ap.bib_backend.jpa.enums.Role;
import edu.ap.bib_backend.jpa.repository.RoleEntityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private RoleEntityRepository roleEntityRepository;

    @InjectMocks
    private RoleService roleService;

    @Test
    void givenEnglishRoleNameUpperCase_WhenGetRoleEntity_ThenReturnExistingRoleEntity() {
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
    void givenEnglishRoleNameLowerCase_WhenGetRoleEntity_ThenReturnExistingRoleEntity() {
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
    void givenDutchRoleNameLeerling_WhenGetRoleEntity_ThenReturnStudentRoleEntity() {
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
    void givenDutchRoleNameLeerkracht_WhenGetRoleEntity_ThenReturnTeacherRoleEntity() {
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
    void givenRoleNotFoundInRepo_WhenGetRoleEntity_ThenCreateNewRoleEntity() {
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
    void givenNullRoleName_WhenGetRoleEntity_ThenReturnStudentRoleEntity() {
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
    void givenInvalidRoleName_WhenGetRoleEntity_ThenThrowIllegalArgumentException() {
        // Arrange
        String invalidRoleName = "invalid";

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> roleService.getRoleEntity(invalidRoleName));
        assertEquals("The ingegeven rol is ongeldig: invalid", exception.getMessage());
        verifyNoInteractions(roleEntityRepository);
    }
}
