package edu.ap.bib_backend.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import edu.ap.bib_backend.dto.AuthUserDto;
import edu.ap.bib_backend.jpa.entity.RoleEntity;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.enums.Role;

@Mapper(componentModel = "spring")
public interface UserMapper {

    // Maps a User entity to an AuthUserDto, using a custom converter for the role
    // field
    @Mapping(target = "role", source = "role", qualifiedByName = "roleEntityToRole")
    @Mapping(target = "primaryGroupName", source = "group.name")
    @Mapping(target = "reviewBlocked", source = "reviewBlocked")
    AuthUserDto toDto(User user);

    List<AuthUserDto> toDtoList(List<User> users);

    // Extracts the Role enum from a RoleEntity
    @Named("roleEntityToRole")
    default Role roleEntityToRole(RoleEntity roleEntity) {
        if (roleEntity == null) {
            return null;
        }
        return roleEntity.getRole();
    }
}