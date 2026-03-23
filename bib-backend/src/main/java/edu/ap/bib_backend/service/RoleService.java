package edu.ap.bib_backend.service;

import java.util.Arrays;
import java.util.Optional;

import org.springframework.stereotype.Service;

import edu.ap.bib_backend.jpa.entity.RoleEntity;
import edu.ap.bib_backend.jpa.enums.Role;
import edu.ap.bib_backend.jpa.repository.RoleEntityRepository;

@Service
public class RoleService {

    private final RoleEntityRepository roleEntityRepository;

    public RoleService(RoleEntityRepository roleEntityRepository) {
        this.roleEntityRepository = roleEntityRepository;
    }

    /**
     * Resolves a role name string to a {@link Role} enum value and ensures the
     * corresponding
     * {@link RoleEntity} exists in the database, creating it if it does not.
     *
     * <p>
     * Resolution order:
     * <ol>
     * <li>Case-insensitive match against {@link Role} enum values (e.g.
     * {@code "admin"} → {@link Role#ADMIN})</li>
     * <li>Dutch role name mapping (e.g. {@code "leerling"} →
     * {@link Role#STUDENT})</li>
     * </ol>
     *
     * @param roleName the role name to resolve, may be an English enum name or a
     *                 Dutch role name
     * @return the resolved {@link Role} enum value, guaranteed to exist in the
     *         database
     * @throws IllegalArgumentException if {@code roleName} does not match any known
     *                                  role
     * @see #mapDutchRole(String)
     */
    public RoleEntity getRoleEntity(String roleName) {
        Role role = Arrays.stream(Role.values())
                .filter(r -> r.name().equalsIgnoreCase(roleName))
                .findFirst()
                .orElseGet(() -> mapDutchRole(roleName)
                        .orElseThrow(() -> new IllegalArgumentException("The ingegeven rol is ongeldig: " + roleName)));

        return roleEntityRepository.findByRole(role)
                .orElseGet(() -> createRoleEntity(role));
    }

    private RoleEntity createRoleEntity(Role role) {
        RoleEntity newRole = new RoleEntity();
        newRole.setRole(role);
        roleEntityRepository.save(newRole);
        return newRole;
    }

    private Optional<Role> mapDutchRole(String basisrol) {
        if (basisrol == null)
            return Optional.of(Role.STUDENT);
        return switch (basisrol.toLowerCase()) {
            case "leerling" -> Optional.of(Role.STUDENT);
            case "leerkracht" -> Optional.of(Role.TEACHER);
            case "directie" -> Optional.of(Role.ADMIN);
            case "bibliothecaris" -> Optional.of(Role.LIBRARIAN);
            default -> Optional.empty();
        };
    }
}