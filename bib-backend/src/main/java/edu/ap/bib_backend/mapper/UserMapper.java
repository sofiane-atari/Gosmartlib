package edu.ap.bib_backend.mapper;

import java.util.List;
import java.util.Set;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import edu.ap.bib_backend.dto.AuthUserDto;
import edu.ap.bib_backend.jpa.entity.Group;
import edu.ap.bib_backend.jpa.entity.RoleEntity;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.enums.Role;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "role", source = "role", qualifiedByName = "roleEntityToRole")
    @Mapping(target = "primaryGroupName", source = "groups", qualifiedByName = "groupsToPrimaryGroupName")
    @Mapping(target = "parentGroupNames", expression = "java(java.util.List.of())")
    @Mapping(target = "firstName", ignore = true)
    @Mapping(target = "lastName", ignore = true)
    @Mapping(target = "fullName", source = "id")
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "smartschoolId", ignore = true)
    @Mapping(target = "platform", ignore = true)
    @Mapping(target = "reviewBlocked", source = "reviewBlocked")
    @Mapping(target = "schoolId", source = "school.id")
    @Mapping(target = "schoolName", source = "school.name")
    @Mapping(target = "campusId", source = "campus.id")
    @Mapping(target = "campusName", source = "campus.name")
    AuthUserDto toDto(User user);

    List<AuthUserDto> toDtoList(List<User> users);

    @Named("roleEntityToRole")
    default Role roleEntityToRole(RoleEntity roleEntity) {
        if (roleEntity == null) {
            return null;
        }
        return roleEntity.getRole();
    }

    @Named("groupsToPrimaryGroupName")
    default String groupsToPrimaryGroupName(Set<Group> groups) {
        if (groups == null || groups.isEmpty()) {
            return null;
        }
        return groups.iterator().next().getName();
    }
}
