package edu.ap.bib_backend.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import edu.ap.bib_backend.dto.AuthUserDto;
import edu.ap.bib_backend.jpa.entity.Group;
import edu.ap.bib_backend.jpa.entity.RoleEntity;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.enums.Role;

class UserMapperTest {

    private final UserMapper userMapper = Mappers.getMapper(UserMapper.class);

    @Test
    void GivenUserWithRoleAndGroup_WhenToDto_ThenMapExpectedFields() {
        // Arrange
        RoleEntity roleEntity = new RoleEntity();
        roleEntity.setRole(Role.STUDENT);

        Group group = new Group();
        group.setName("3C");

        User user = new User();
        user.setId("s100");
        user.setRole(roleEntity);
        user.setGroup(group);

        // Act
        AuthUserDto result = userMapper.toDto(user);

        // Assert
        assertEquals("s100", result.id());
        assertEquals(Role.STUDENT, result.role());
        assertEquals("3C", result.primaryGroupName());
    }

    @Test
    void GivenUsersList_WhenToDtoList_ThenMapEveryUser() {
        // Arrange
        RoleEntity teacherRole = new RoleEntity();
        teacherRole.setRole(Role.TEACHER);

        User firstUser = new User();
        firstUser.setId("t001");
        firstUser.setRole(teacherRole);

        RoleEntity librarianRole = new RoleEntity();
        librarianRole.setRole(Role.LIBRARIAN);

        User secondUser = new User();
        secondUser.setId("l001");
        secondUser.setRole(librarianRole);

        List<User> users = List.of(firstUser, secondUser);

        // Act
        List<AuthUserDto> result = userMapper.toDtoList(users);

        // Assert
        assertEquals(2, result.size());
        assertEquals("t001", result.get(0).id());
        assertEquals(Role.TEACHER, result.get(0).role());
        assertEquals("l001", result.get(1).id());
        assertEquals(Role.LIBRARIAN, result.get(1).role());
    }

    @Test
    void GivenNullRoleEntity_WhenRoleEntityToRole_ThenReturnNull() {
        // Arrange
        RoleEntity roleEntity = null;

        // Act
        Role result = userMapper.roleEntityToRole(roleEntity);

        // Assert
        assertNull(result);
    }
}