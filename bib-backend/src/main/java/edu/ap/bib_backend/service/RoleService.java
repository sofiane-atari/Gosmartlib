package edu.ap.bib_backend.service;

import java.util.Arrays;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ap.bib_backend.dto.AuthUserDto;
import edu.ap.bib_backend.exception.ResourceNotFoundException;
import edu.ap.bib_backend.jpa.entity.RoleEntity;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.enums.Role;
import edu.ap.bib_backend.jpa.repository.RoleEntityRepository;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import edu.ap.bib_backend.mapper.UserMapper;
import edu.ap.bib_backend.service.user.CurrentUserService;

@Service
public class RoleService {

    private final RoleEntityRepository roleEntityRepository;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final CurrentUserService currentUserService;

    public RoleService(
            RoleEntityRepository roleEntityRepository,
            UserRepository userRepository,
            UserMapper userMapper,
            CurrentUserService currentUserService) {
        this.roleEntityRepository = roleEntityRepository;
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public AuthUserDto assignLibrarian(Authentication authentication, String targetUserId) {
        User currentUser = currentUserService.getCurrentUser(authentication);

        boolean isLibrarian = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_LIBRARIAN"));
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isLibrarian && !isAdmin) {
            throw new IllegalStateException("Alleen bibliothecarissen en admins kunnen deze actie uitvoeren.");
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("userId", "Gebruiker niet gevonden met id: " + targetUserId));

        if (targetUser.getRole().getRole() != Role.TEACHER) {
            throw new IllegalArgumentException("Alleen leerkrachten kunnen gepromoveerd worden tot bibliothecaris.");
        }

        if (!currentUser.getSchool().getId().equals(targetUser.getSchool().getId())) {
            throw new IllegalStateException("Je kunt alleen leerkrachten van je eigen school promoten tot bibliothecaris.");
        }

        RoleEntity librarianRole = getRoleEntity(Role.LIBRARIAN.name());
        targetUser.setRole(librarianRole);
        userRepository.save(targetUser);

        return userMapper.toDto(targetUser);
    }

    @Transactional
    public AuthUserDto revokeLibrarian(Authentication authentication, String targetUserId) {
        User currentUser = currentUserService.getCurrentUser(authentication);

        boolean isLibrarian = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_LIBRARIAN"));
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isLibrarian && !isAdmin) {
            throw new IllegalStateException("Alleen bibliothecarissen en admins kunnen deze actie uitvoeren.");
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("userId", "Gebruiker niet gevonden met id: " + targetUserId));

        if (targetUser.getRole().getRole() != Role.LIBRARIAN) {
            throw new IllegalArgumentException("Alleen bibliothecarissen kunnen teruggezet worden naar leerkracht.");
        }

        if (!currentUser.getSchool().getId().equals(targetUser.getSchool().getId())) {
            throw new IllegalStateException("Je kunt alleen bibliothecarissen van je eigen school beheren.");
        }

        RoleEntity teacherRole = getRoleEntity(Role.TEACHER.name());
        targetUser.setRole(teacherRole);
        userRepository.save(targetUser);

        return userMapper.toDto(targetUser);
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
        return roleEntityRepository.save(newRole);
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