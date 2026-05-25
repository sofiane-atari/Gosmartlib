package edu.ap.bib_backend.service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ap.bib_backend.dto.AuthUserDto;
import edu.ap.bib_backend.exception.ResourceNotFoundException;
import edu.ap.bib_backend.jpa.entity.AuthSession;
import edu.ap.bib_backend.jpa.entity.RoleEntity;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.enums.Role;
import edu.ap.bib_backend.jpa.repository.RoleEntityRepository;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import edu.ap.bib_backend.mapper.UserMapper;
import edu.ap.bib_backend.service.smartschool.SmartschoolOAuth2UserService;
import edu.ap.bib_backend.service.user.CurrentUserService;

@Service
public class RoleService {

    private final RoleEntityRepository roleEntityRepository;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final CurrentUserService currentUserService;
    private final TokenStorageService tokenStorageService;
    private final JwtService jwtService;

    public RoleService(
            RoleEntityRepository roleEntityRepository,
            UserRepository userRepository,
            UserMapper userMapper,
            CurrentUserService currentUserService,
            TokenStorageService tokenStorageService,
            JwtService jwtService) {
        this.roleEntityRepository = roleEntityRepository;
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.currentUserService = currentUserService;
        this.tokenStorageService = tokenStorageService;
        this.jwtService = jwtService;
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

        if (!isAdmin && !currentUser.getSchool().getId().equals(targetUser.getSchool().getId())) {
            throw new IllegalStateException("Je kunt alleen leerkrachten van je eigen school promoten tot bibliothecaris.");
        }

        RoleEntity librarianRole = getRoleEntity(Role.LIBRARIAN.name());
        targetUser.setRole(librarianRole);
        userRepository.save(targetUser);

        replaceTargetToken(targetUserId, "ROLE_LIBRARIAN");

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

        if (!isAdmin && !currentUser.getSchool().getId().equals(targetUser.getSchool().getId())) {
            throw new IllegalStateException("Je kunt alleen bibliothecarissen van je eigen school beheren.");
        }

        RoleEntity teacherRole = getRoleEntity(Role.TEACHER.name());
        targetUser.setRole(teacherRole);
        userRepository.save(targetUser);

        replaceTargetToken(targetUserId, "ROLE_TEACHER");

        return userMapper.toDto(targetUser);
    }

    @Transactional
    public AuthUserDto assignAdmin(Authentication authentication, String targetUserId) {
        User currentUser = currentUserService.getCurrentUser(authentication);

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            throw new IllegalStateException("Alleen platformbeheerders kunnen deze actie uitvoeren.");
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("userId", "Gebruiker niet gevonden met id: " + targetUserId));

        if (targetUser.getRole().getRole() != Role.TEACHER) {
            throw new IllegalArgumentException("Alleen leerkrachten kunnen gepromoveerd worden tot platformbeheerder.");
        }

        RoleEntity adminRole = getRoleEntity(Role.ADMIN.name());
        targetUser.setRole(adminRole);
        userRepository.save(targetUser);

        replaceTargetToken(targetUserId, "ROLE_ADMIN");

        return userMapper.toDto(targetUser);
    }

    @Transactional
    public AuthUserDto revokeAdmin(Authentication authentication, String targetUserId) {
        User currentUser = currentUserService.getCurrentUser(authentication);

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            throw new IllegalStateException("Alleen platformbeheerders kunnen deze actie uitvoeren.");
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("userId", "Gebruiker niet gevonden met id: " + targetUserId));

        if (targetUser.getRole().getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("Alleen platformbeheerders kunnen teruggezet worden naar leerkracht.");
        }

        RoleEntity teacherRole = getRoleEntity(Role.TEACHER.name());
        targetUser.setRole(teacherRole);
        userRepository.save(targetUser);

        replaceTargetToken(targetUserId, "ROLE_TEACHER");

        return userMapper.toDto(targetUser);
    }

    private void replaceTargetToken(String targetUserId, String newRole) {
        Map<String, Object> extraClaims = new HashMap<>();

        tokenStorageService.getSession(targetUserId)
                .map(AuthSession::getJwtToken)
                .ifPresent(oldToken -> {
                    extraClaims.put(SmartschoolOAuth2UserService.CLAIM_CLASS_CODE,
                            jwtService.extractClaim(oldToken, SmartschoolOAuth2UserService.CLAIM_CLASS_CODE));
                    extraClaims.put(SmartschoolOAuth2UserService.CLAIM_YEAR_CODE,
                            jwtService.extractClaim(oldToken, SmartschoolOAuth2UserService.CLAIM_YEAR_CODE));
                    extraClaims.put(SmartschoolOAuth2UserService.CLAIM_SCHOOL_CODE,
                            jwtService.extractClaim(oldToken, SmartschoolOAuth2UserService.CLAIM_SCHOOL_CODE));
                });

        String newToken = jwtService.generateToken(targetUserId, newRole, extraClaims);
        tokenStorageService.replaceToken(
                targetUserId,
                newToken,
                newRole,
                jwtService.extractExpirationInstant(newToken));
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
            case "directie" -> Optional.of(Role.ADMIN);
            case "bibliothecaris" -> Optional.of(Role.LIBRARIAN);
            default -> Optional.empty();
        };
    }
}
