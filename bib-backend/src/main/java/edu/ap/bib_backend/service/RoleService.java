package edu.ap.bib_backend.service;

import java.util.Arrays;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ap.bib_backend.dto.AuthUserDto;
import edu.ap.bib_backend.exception.ResourceNotFoundException;
import edu.ap.bib_backend.jpa.entity.RoleEntity;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.enums.Role;
import edu.ap.bib_backend.jpa.repository.RoleEntityRepository;
import edu.ap.bib_backend.jpa.repository.SchoolRepository;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import edu.ap.bib_backend.mapper.UserMapper;
import edu.ap.bib_backend.service.oneroster.OneRosterClient;
import edu.ap.bib_backend.service.user.CurrentUserService;
import edu.ap.bib_backend.service.user.UserService;

@Service
public class RoleService {

    private final RoleEntityRepository roleEntityRepository;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final CurrentUserService currentUserService;
    private final TokenStorageService tokenStorageService;
    private final OneRosterClient oneRosterClient;
    private final UserService userService;
    private final SchoolRepository schoolRepository;
    private final boolean devModeEnabled;

    public RoleService(
            RoleEntityRepository roleEntityRepository,
            UserRepository userRepository,
            UserMapper userMapper,
            CurrentUserService currentUserService,
            TokenStorageService tokenStorageService,
            OneRosterClient oneRosterClient,
            UserService userService,
            SchoolRepository schoolRepository,
            @Value("${app.dev-mode-enabled:false}") boolean devModeEnabled) {
        this.roleEntityRepository = roleEntityRepository;
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.currentUserService = currentUserService;
        this.tokenStorageService = tokenStorageService;
        this.oneRosterClient = oneRosterClient;
        this.userService = userService;
        this.schoolRepository = schoolRepository;
        this.devModeEnabled = devModeEnabled;
    }

    @Transactional
    public AuthUserDto assignLibrarian(Authentication authentication, String targetUserId, String schoolCode) {
        User currentUser = currentUserService.getCurrentUser(authentication);

        boolean isLibrarian = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_LIBRARIAN"));
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isLibrarian && !isAdmin) {
            throw new IllegalStateException("Alleen bibliothecarissen en admins kunnen deze actie uitvoeren.");
        }

        if (!isAdmin && schoolCode != null && !schoolCode.isBlank()) {
            throw new IllegalArgumentException("Alleen beheerders mogen een schoolcode opgeven.");
        }

        School school = resolveTargetSchool(isAdmin, schoolCode, currentUser);

        User targetUser = userRepository.findByIdAndSchoolId(targetUserId, school.getId()).orElse(null);
        if (targetUser == null) {
            String sourcedId = oneRosterClient.findSourcedIdByLegacyIdentifier(school.getDomainName(), targetUserId);
            if (sourcedId == null) {
                throw new ResourceNotFoundException("userId", "Gebruiker niet gevonden met id: " + targetUserId);
            }
            RoleEntity teacherRole = getRoleEntity(Role.TEACHER.name());
            targetUser = userService.createUser(targetUserId, teacherRole, school);
        }

        if (targetUser.getRole().getRole() != Role.TEACHER) {
            throw new IllegalArgumentException("Alleen leerkrachten kunnen gepromoveerd worden tot bibliothecaris.");
        }

        RoleEntity librarianRole = getRoleEntity(Role.LIBRARIAN.name());
        targetUser.setRole(librarianRole);
        userRepository.save(targetUser);

        tokenStorageService.revokeToken(targetUserId);

        return userMapper.toDto(targetUser);
    }

    @Transactional
    public AuthUserDto revokeLibrarian(Authentication authentication, String targetUserId, String schoolCode) {
        User currentUser = currentUserService.getCurrentUser(authentication);

        boolean isLibrarian = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_LIBRARIAN"));
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isLibrarian && !isAdmin) {
            throw new IllegalStateException("Alleen bibliothecarissen en admins kunnen deze actie uitvoeren.");
        }

        if (!isAdmin && schoolCode != null && !schoolCode.isBlank()) {
            throw new IllegalArgumentException("Alleen beheerders mogen een schoolcode opgeven.");
        }

        School school = resolveTargetSchool(isAdmin, schoolCode, currentUser);

        User targetUser = userRepository.findByIdAndSchoolId(targetUserId, school.getId())
                .orElseThrow(() -> new ResourceNotFoundException("userId", "Gebruiker niet gevonden met id: " + targetUserId));

        if (targetUser.getRole().getRole() != Role.LIBRARIAN) {
            throw new IllegalArgumentException("Alleen bibliothecarissen kunnen teruggezet worden naar leerkracht.");
        }

        RoleEntity teacherRole = getRoleEntity(Role.TEACHER.name());
        targetUser.setRole(teacherRole);
        userRepository.save(targetUser);

        tokenStorageService.revokeToken(targetUserId);

        return userMapper.toDto(targetUser);
    }

    @Transactional
    public AuthUserDto assignAdmin(Authentication authentication, String targetUserId, String schoolCode) {
        User currentUser = currentUserService.getCurrentUser(authentication);

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            throw new IllegalStateException("Alleen platformbeheerders kunnen deze actie uitvoeren.");
        }

        if (!devModeEnabled) {
            throw new IllegalStateException("Admin promotion is alleen beschikbaar in development mode.");
        }

        School school = resolveTargetSchool(true, schoolCode, currentUser);

        User targetUser = userRepository.findByIdAndSchoolId(targetUserId, school.getId()).orElse(null);
        if (targetUser == null) {
            String sourcedId = oneRosterClient.findSourcedIdByLegacyIdentifier(school.getDomainName(), targetUserId);
            if (sourcedId == null) {
                throw new ResourceNotFoundException("userId", "Gebruiker niet gevonden met id: " + targetUserId);
            }
            RoleEntity teacherRole = getRoleEntity(Role.TEACHER.name());
            targetUser = userService.createUser(targetUserId, teacherRole, school);
        }

        if (targetUser.getRole().getRole() != Role.TEACHER && targetUser.getRole().getRole() != Role.LIBRARIAN) {
            throw new IllegalArgumentException("Alleen leerkrachten en bibliothecarissen kunnen gepromoveerd worden tot platformbeheerder.");
        }

        RoleEntity adminRole = getRoleEntity(Role.ADMIN.name());
        targetUser.setRole(adminRole);
        userRepository.save(targetUser);

        tokenStorageService.revokeToken(targetUserId);

        return userMapper.toDto(targetUser);
    }

    @Transactional
    public AuthUserDto revokeAdmin(Authentication authentication, String targetUserId, String schoolCode) {
        User currentUser = currentUserService.getCurrentUser(authentication);

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            throw new IllegalStateException("Alleen platformbeheerders kunnen deze actie uitvoeren.");
        }

        if (!devModeEnabled) {
            throw new IllegalStateException("Admin revocation is alleen beschikbaar in development mode.");
        }

        School school = resolveTargetSchool(true, schoolCode, currentUser);

        User targetUser = userRepository.findByIdAndSchoolId(targetUserId, school.getId())
                .orElseThrow(() -> new ResourceNotFoundException("userId", "Gebruiker niet gevonden met id: " + targetUserId));

        if (targetUser.getRole().getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("Alleen platformbeheerders kunnen teruggezet worden naar leerkracht.");
        }

        RoleEntity teacherRole = getRoleEntity(Role.TEACHER.name());
        targetUser.setRole(teacherRole);
        userRepository.save(targetUser);

        tokenStorageService.revokeToken(targetUserId);

        return userMapper.toDto(targetUser);
    }

    private School resolveTargetSchool(boolean isAdmin, String schoolCode, User currentUser) {
        if (isAdmin) {
            if (schoolCode == null || schoolCode.isBlank()) {
                throw new IllegalArgumentException("Schoolcode is verplicht voor beheerders.");
            }
            return schoolRepository.findByDomainName(schoolCode)
                    .orElseThrow(() -> new ResourceNotFoundException("schoolCode", "School niet gevonden: " + schoolCode));
        }
        return currentUser.getSchool();
    }

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
            default -> Optional.empty();
        };
    }
}
