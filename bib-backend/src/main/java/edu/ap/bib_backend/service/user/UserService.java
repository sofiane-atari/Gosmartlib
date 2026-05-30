package edu.ap.bib_backend.service.user;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import jakarta.transaction.Transactional;

import edu.ap.bib_backend.dto.AuthUserDto;
import edu.ap.bib_backend.dto.TeacherStatsDTO;
import edu.ap.bib_backend.dto.PageDTO;
import edu.ap.bib_backend.dto.PaginatedMeta;
import edu.ap.bib_backend.dto.SchoolSummaryDto;
import edu.ap.bib_backend.exception.ResourceNotFoundException;
import edu.ap.bib_backend.jpa.entity.RoleEntity;
import edu.ap.bib_backend.jpa.enums.Role;
import edu.ap.bib_backend.jpa.entity.Campus;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.repository.SchoolRepository;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import edu.ap.bib_backend.mapper.UserMapper;
import edu.ap.bib_backend.options.UserGetOptions;
import edu.ap.bib_backend.service.GroupService;
import edu.ap.bib_backend.service.review.ProfanityFilterService;
import edu.ap.bib_backend.service.TokenStorageService;
import edu.ap.bib_backend.service.oneroster.OneRosterClient;
import edu.ap.bib_backend.jpa.repository.CampusRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final OneRosterClient oneRosterClient;
    private final TokenStorageService tokenStorageService;
    private final CampusRepository campusRepository;
    private final CurrentUserService currentUserService;
    private final SchoolRepository schoolRepository;

    public UserService(UserRepository userRepository, ProfanityFilterService profanityFilterService,
            UserMapper userMapper, GroupService groupService, SchoolRepository schoolRepository,
            OneRosterClient oneRosterClient, TokenStorageService tokenStorageService,
            CampusRepository campusRepository, CurrentUserService currentUserService) {

        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.oneRosterClient = oneRosterClient;
        this.tokenStorageService = tokenStorageService;
        this.campusRepository = campusRepository;
        this.currentUserService = currentUserService;
        this.schoolRepository = schoolRepository;
    }

    public Optional<User> getUser(String userId) {
        return userRepository.findById(userId);
    }

    /**
     * @param options firstName, lastName, and id filters (case-insensitive substring match)
     * @return matching users from the OneRoster API mapped to AuthUserDto
     */
    public List<AuthUserDto> getUsers(UserGetOptions options) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = requireUserId(authentication);

        School school = getCurrentUserSchool(userId);

        String schoolCode = school.getDomainName();
        String smartschoolToken = tokenStorageService.getSmartschoolAccessToken(userId);
        List<Map<String, Object>> rosterUsers = oneRosterClient.fetchUsersWithBearer(schoolCode, smartschoolToken);

        return rosterUsers.stream()
                .map(user -> toAuthUserDto(user, school))
                .filter(Objects::nonNull)
                .filter(dto -> matchesOptions(dto, options))
                .toList();
    }

    public TeacherStatsDTO getTeacherStats(Authentication authentication) {
        String userId = currentUserService.extractCurrentUserId(authentication);

        boolean isLibrarian = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_LIBRARIAN"));
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isLibrarian && !isAdmin) {
            throw new AccessDeniedException("Alleen bibliothecarissen en admins kunnen docenten bekijken.");
        }

        School school = getCurrentUserSchool(userId);
        String schoolCode = school.getDomainName();
        String smartschoolToken = tokenStorageService.getSmartschoolAccessToken(userId);
        List<Map<String, Object>> rosterUsers = oneRosterClient.fetchUsersWithBearer(schoolCode, smartschoolToken);

        Map<String, Role> localRolesByUserId = userRepository.findBySchoolId(school.getId()).stream()
                .collect(Collectors.toMap(User::getId, u -> u.getRole().getRole()));

        List<AuthUserDto> allUsers = rosterUsers.stream()
                .map(user -> toAuthUserDto(user, school))
                .filter(Objects::nonNull)
                .filter(dto -> dto.role() == Role.TEACHER)
                .toList();

        int totalTeachers = allUsers.size();

        int librarians = (int) allUsers.stream()
                .filter(dto -> {
                    Role localRole = localRolesByUserId.get(dto.id());
                    return localRole == Role.LIBRARIAN;
                })
                .count();

        int admins = (int) allUsers.stream()
                .filter(dto -> {
                    Role localRole = localRolesByUserId.get(dto.id());
                    return localRole == Role.ADMIN;
                })
                .count();

        int activeTeachers = totalTeachers - librarians - admins;

        return new TeacherStatsDTO(totalTeachers, activeTeachers, librarians, admins);
    }

    public PageDTO<AuthUserDto> getTeachers(Authentication authentication,
            boolean excludeLibrarians, boolean excludeAdmins,
            int page, int pageSize, String search) {
        String userId = currentUserService.extractCurrentUserId(authentication);

        boolean isLibrarian = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_LIBRARIAN"));
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isLibrarian && !isAdmin) {
            throw new AccessDeniedException("Alleen bibliothecarissen en admins kunnen docenten bekijken.");
        }

        School school = getCurrentUserSchool(userId);
        String schoolCode = school.getDomainName();
        String smartschoolToken = tokenStorageService.getSmartschoolAccessToken(userId);
        List<Map<String, Object>> rosterUsers = oneRosterClient.fetchUsersWithBearer(schoolCode, smartschoolToken);

        Map<String, Role> localRolesByUserId = userRepository.findBySchoolId(school.getId()).stream()
                .collect(Collectors.toMap(User::getId, u -> u.getRole().getRole()));

        List<AuthUserDto> allUsers = rosterUsers.stream()
                .map(user -> toAuthUserDto(user, school))
                .filter(Objects::nonNull)
                .filter(dto -> dto.role() == Role.TEACHER)
                .map(dto -> {
                    Role actualRole = localRolesByUserId.get(dto.id());
                    if (actualRole != null) {
                        return new AuthUserDto(
                                dto.id(), dto.firstName(), dto.lastName(), dto.fullName(),
                                actualRole, dto.email(), dto.username(),
                                dto.smartschoolId(), dto.platform(), dto.primaryGroupName(),
                                dto.parentGroupNames(), dto.reviewBlocked(),
                                dto.schoolId(), dto.schoolName(), dto.campusId(), dto.campusName(),
                                dto.school());
                    }
                    return dto;
                })
                .filter(dto -> !excludeLibrarians || dto.role() != Role.LIBRARIAN)
                .filter(dto -> !excludeAdmins || dto.role() != Role.ADMIN)
                .toList();

        List<AuthUserDto> filtered = allUsers;
        if (StringUtils.hasText(search)) {
            String query = search.trim().toLowerCase();
            filtered = allUsers.stream()
                    .filter(dto -> (dto.fullName() != null && dto.fullName().toLowerCase().contains(query))
                            || (dto.firstName() != null && dto.firstName().toLowerCase().contains(query))
                            || (dto.lastName() != null && dto.lastName().toLowerCase().contains(query))
                            || (dto.email() != null && dto.email().toLowerCase().contains(query)))
                    .toList();
        }

        int totalItems = filtered.size();
        int totalPages = (int) Math.ceil((double) totalItems / pageSize);
        int fromIndex = page * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, totalItems);

        List<AuthUserDto> pageData = fromIndex >= totalItems ? List.of() : filtered.subList(fromIndex, toIndex);

        return new PageDTO<>(pageData, new PaginatedMeta(page, pageSize, totalItems, totalPages));
    }

    public List<AuthUserDto> searchExternalUsers(Authentication authentication, Long schoolId, String firstName, String lastName) {
        boolean isLibrarian = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_LIBRARIAN"));
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isLibrarian && !isAdmin) {
            throw new AccessDeniedException("Alleen bibliothecarissen en admins kunnen externe gebruikers zoeken.");
        }

        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("schoolId", "School niet gevonden met id: " + schoolId));

        School currentUserSchool = getCurrentUserSchool(currentUserService.extractCurrentUserId(authentication));
        if (currentUserSchool.getId() != null && currentUserSchool.getId().equals(schoolId)) {
            throw new IllegalArgumentException("Kies een andere school voor extern uitlenen.");
        }

        Map<String, Role> localRolesByUserId = userRepository.findBySchoolId(schoolId).stream()
                .collect(Collectors.toMap(User::getId, u -> u.getRole().getRole()));

        return oneRosterClient.fetchUsers(school.getDomainName()).stream()
                .map(user -> toAuthUserDto(user, school))
                .filter(Objects::nonNull)
                .map(dto -> withLocalRole(dto, localRolesByUserId.get(dto.id())))
                .filter(this::isAllowedExternalBorrower)
                .filter(dto -> matchesFilter(dto.firstName(), firstName))
                .filter(dto -> matchesFilter(dto.lastName(), lastName))
                .toList();
    }

    public List<AuthUserDto> getUsersBySchoolId(Authentication authentication, Long schoolId) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            throw new AccessDeniedException("Alleen platformbeheerders kunnen gebruikers van een school opvragen.");
        }

        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("schoolId", "School niet gevonden met id: " + schoolId));

        String userId = authentication.getName();
        String smartschoolToken = tokenStorageService.getSmartschoolAccessToken(userId);
        String schoolCode = school.getDomainName();

        List<Map<String, Object>> rosterUsers = oneRosterClient.fetchUsersWithBearer(schoolCode, smartschoolToken);

        Map<String, Role> localRolesByUserId = userRepository.findBySchoolId(schoolId).stream()
                .collect(Collectors.toMap(User::getId, u -> u.getRole().getRole()));

        return rosterUsers.stream()
                .map(user -> toAuthUserDto(user, school))
                .filter(Objects::nonNull)
                .filter(dto -> dto.role() == Role.TEACHER)
                .map(dto -> {
                    Role actualRole = localRolesByUserId.get(dto.id());
                    if (actualRole != null) {
                        return new AuthUserDto(
                                dto.id(), dto.firstName(), dto.lastName(), dto.fullName(),
                                actualRole, dto.email(), dto.username(),
                                dto.smartschoolId(), dto.platform(), dto.primaryGroupName(),
                                dto.parentGroupNames(), dto.reviewBlocked(),
                                dto.schoolId(), dto.schoolName(), dto.campusId(), dto.campusName(),
                                dto.school());
                    }
                    return dto;
                })
                .filter(dto -> {
                    Role role = dto.role();
                    return role == Role.TEACHER || role == Role.LIBRARIAN || role == Role.ADMIN;
                })
                .toList();
    }

    public User createUser(String userId, RoleEntity role, School school, Campus campus) {
        if (userRepository.existsById(userId)) {
            throw new RuntimeException("Gebruiker bestaat al");
        }

        User user = new User();
        user.setId(userId);

        user.setRole(Objects.requireNonNull(role, "role mag niet null zijn"));
        user.setSchool(Objects.requireNonNull(school, "school mag niet null zijn"));
        user.setCampus(campus);

        return userRepository.save(user);
    }

    public User createUser(String userId, RoleEntity role, School school) {
        return createUser(userId, role, school, null);
    }

    public AuthUserDto setReviewBlocked(String userId, boolean blocked) {
        User user = userRepository.findById(userId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("userId", "Gebruiker niet gevonden met id: " + userId));

        user.setReviewBlocked(blocked);
        User updatedUser = userRepository.save(user);
        return userMapper.toDto(updatedUser);
    }

    /**
     * @param authentication authenticated user's security context
     * @param campusId       target campus id
     * @return updated AuthUserDto with the new campus
     */
    @Transactional
    public AuthUserDto switchCurrentUserCampus(Authentication authentication, Long campusId) {
        if (campusId == null) {
            throw new IllegalArgumentException("campusId is verplicht.");
        }

        String currentUserId = currentUserService.extractCurrentUserId(authentication);

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "userId",
                        "Gebruiker niet gevonden met id: " + currentUserId
                ));

        if (user.getSchool() == null) {
            throw new IllegalStateException("Gebruiker heeft geen school gekoppeld.");
        }

        Campus campus = campusRepository.findById(campusId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "campusId",
                        "Campus niet gevonden met id: " + campusId
                ));

        if (campus.getSchool() == null || !campus.getSchool().getId().equals(user.getSchool().getId())) {
            throw new IllegalArgumentException("Deze campus hoort niet bij jouw school.");
        }

        user.setCampus(campus);

        return userMapper.toDto(userRepository.save(user));
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    private String requireUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new IllegalStateException("Geen geauthenticeerde gebruiker gevonden.");
        }

        return authentication.getName();
    }

    private School getCurrentUserSchool(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException(
                        "Gebruiker niet gevonden: " + userId));

        if (user.getSchool() == null) {
            throw new IllegalStateException("Gebruiker heeft geen school gekoppeld.");
        }

        return user.getSchool();
    }

    /**
     * @param rosterUser raw OneRoster user map
     * @param ctxSchool  pre-resolved school context (avoids per-user DB lookup)
     * @return mapped AuthUserDto, or null if no legacy identifier is found
     */
    private AuthUserDto toAuthUserDto(Map<String, Object> rosterUser, School ctxSchool) {
        String id = readMetadataString(rosterUser, "smsc.legacyIdentifier");
        if (!StringUtils.hasText(id)) {
            return null;
        }

        String firstName = firstNonBlank(rosterUser, "givenName", "firstName");
        String lastName = firstNonBlank(rosterUser, "familyName", "lastName", "surname");
        String fullName = firstNonBlank(rosterUser, "fullName", "name", "displayName");
        if (!StringUtils.hasText(fullName)) {
            String combined = ((firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName))
                    .trim();
            fullName = combined.isBlank() ? null : combined;
        }

        String email = firstNonBlank(rosterUser, "email", "emailAddress", "mail");
        String username = firstNonBlank(rosterUser, "username", "userName");
        Role role = mapOneRosterRole(firstNonBlank(rosterUser, "role"));

        SchoolSummaryDto schoolSummary = ctxSchool.getSchoolSummaryDto();

        return new AuthUserDto(
                id,
                firstName,
                lastName,
                fullName,
                role,
                email,
                username,
                null,
                null,
                null,
                List.of(),
                false,
                null,
                null,
                null,
                null,
                schoolSummary);
    }

    private Role mapOneRosterRole(String roleStr) {
        if (roleStr == null) {
            return null;
        }

        return switch (roleStr.toLowerCase()) {
            case "teacher", "leerkracht" -> Role.TEACHER;
            case "student", "leerling" -> Role.STUDENT;
            case "administrator", "admin", "directie" -> Role.ADMIN;
            case "librarian", "bibliothecaris" -> Role.LIBRARIAN;
            default -> null;
        };
    }

    private boolean matchesOptions(AuthUserDto dto, UserGetOptions options) {
        if (options == null) {
            return true;
        }

        if (!matchesFilter(dto.id(), options.getId())) {
            return false;
        }

        if (!matchesFilter(dto.firstName(), options.getFirstName())) {
            return false;
        }

        return matchesFilter(dto.lastName(), options.getLastName());
    }

    private boolean matchesFilter(String value, String filter) {
        if (!StringUtils.hasText(filter)) {
            return true;
        }

        if (!StringUtils.hasText(value)) {
            return false;
        }

        return value.toLowerCase().contains(filter.trim().toLowerCase());
    }

    private AuthUserDto withLocalRole(AuthUserDto dto, Role actualRole) {
        if (actualRole == null) {
            return dto;
        }

        return new AuthUserDto(
                dto.id(), dto.firstName(), dto.lastName(), dto.fullName(),
                actualRole, dto.email(), dto.username(),
                dto.smartschoolId(), dto.platform(), dto.primaryGroupName(),
                dto.parentGroupNames(), dto.reviewBlocked(),
                dto.schoolId(), dto.schoolName(), dto.campusId(), dto.campusName(),
                dto.school());
    }

    private boolean isAllowedExternalBorrower(AuthUserDto dto) {
        Role role = dto.role();
        return role == Role.TEACHER || role == Role.LIBRARIAN || role == Role.ADMIN;
    }

    private String firstNonBlank(Map<String, Object> source, String... keys) {
        if (source == null) {
            return null;
        }

        for (String key : keys) {
            Object value = source.get(key);
            if (value instanceof String text && !text.isBlank()) {
                return text.trim();
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private String readMetadataString(Map<String, Object> user, String key) {
        if (user == null) {
            return null;
        }

        Object metadataObject = user.get("metadata");
        if (!(metadataObject instanceof Map<?, ?> metadata)) {
            return null;
        }

        Object value = metadata.get(key);
        if (value instanceof String text && !text.isBlank()) {
            return text.trim();
        }

        return null;
    }
}


