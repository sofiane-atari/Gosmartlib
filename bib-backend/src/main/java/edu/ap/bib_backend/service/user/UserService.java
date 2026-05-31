package edu.ap.bib_backend.service.user;

import java.util.HashMap;
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

import edu.ap.bib_backend.dto.AuthUserDto;
import edu.ap.bib_backend.dto.oneroster.OneRosterClassDTO;
import edu.ap.bib_backend.dto.oneroster.OneRosterEnrollmentDTO;
import edu.ap.bib_backend.dto.TeacherStatsDTO;
import edu.ap.bib_backend.dto.PageDTO;
import edu.ap.bib_backend.dto.PaginatedMeta;
import edu.ap.bib_backend.dto.SchoolSummaryDto;
import edu.ap.bib_backend.exception.ResourceNotFoundException;
import edu.ap.bib_backend.jpa.entity.RoleEntity;
import edu.ap.bib_backend.jpa.enums.Role;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.repository.SchoolRepository;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import edu.ap.bib_backend.mapper.UserMapper;
import edu.ap.bib_backend.options.UserGetOptions;
import edu.ap.bib_backend.service.TokenStorageService;
import edu.ap.bib_backend.service.oneroster.OneRosterClient;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final OneRosterClient oneRosterClient;
    private final TokenStorageService tokenStorageService;
    private final CurrentUserService currentUserService;
    private final SchoolRepository schoolRepository;

    public UserService(UserRepository userRepository,
            UserMapper userMapper, SchoolRepository schoolRepository,
            OneRosterClient oneRosterClient, TokenStorageService tokenStorageService,
            CurrentUserService currentUserService) {

        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.oneRosterClient = oneRosterClient;
        this.tokenStorageService = tokenStorageService;
        this.currentUserService = currentUserService;
        this.schoolRepository = schoolRepository;
    }

    public Optional<User> getUser(String userId) {
        return userRepository.findById(userId);
    }

    /**
     * Finds OneRoster users in the current user's school.
     *
     * @param options firstName, lastName, and id filters (case-insensitive substring match)
     * @return matching users from the OneRoster API mapped to AuthUserDto
     */
    public List<AuthUserDto> getUsers(UserGetOptions options) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = requireUserId(authentication);

        School school = currentUserService.getCurrentUserSchool(authentication);

        String schoolCode = school.getDomainName();
        String smartschoolToken = tokenStorageService.getSmartschoolAccessToken(userId);
        List<Map<String, Object>> rosterUsers = oneRosterClient.fetchUsersWithBearer(schoolCode, smartschoolToken);
        Map<String, String> oneRosterGroupNamesByUserId = getOneRosterGroupNamesByUserId(
                schoolCode,
                smartschoolToken,
                rosterUsers,
                true);

        Map<String, List<String>> enrollmentGroupsByUserId = options.isWithGroups()
                ? buildEnrollmentGroups(schoolCode, smartschoolToken, rosterUsers)
                : Map.of();

        return rosterUsers.stream()
                .map(user -> toAuthUserDto(user, school))
                .filter(Objects::nonNull)
                .map(dto -> withLocalGroupName(dto, oneRosterGroupNamesByUserId.get(dto.id())))
                .map(dto -> withEnrollmentGroups(dto, enrollmentGroupsByUserId.get(dto.id())))
                .filter(dto -> matchesOptions(dto, options))
                .toList();
    }

    /**
     * Counts teachers by effective local teacher, librarian, and admin roles.
     *
     * @param authentication current authenticated user
     * @return teacher role statistics
     * @throws AccessDeniedException if the current user is not a librarian or admin
     */
    public TeacherStatsDTO getTeacherStats(Authentication authentication) {
        String userId = currentUserService.extractCurrentUserId(authentication);

        boolean isLibrarian = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_LIBRARIAN"));
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isLibrarian && !isAdmin) {
            throw new AccessDeniedException("Alleen bibliothecarissen en admins kunnen docenten bekijken.");
        }

        School school = currentUserService.getCurrentUserSchool(authentication);
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

    /**
     * Finds paged OneRoster teacher users for the current user's school, applying local librarian/admin roles and optional exclusions.
     *
     * @param authentication current authenticated user
     * @param excludeLibrarians whether librarian roles should be excluded
     * @param excludeAdmins whether admin roles should be excluded
     * @param page zero-based page index
     * @param pageSize page size
     * @param search optional text search over teacher identity fields
     * @return paged teacher users
     * @throws AccessDeniedException if the current user is not a librarian or admin
     */
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

        School school = currentUserService.getCurrentUserSchool(authentication);
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

    /**
     * Searches external school users who may borrow books.
     *
     * @param authentication current authenticated user
     * @param schoolId external school identifier
     * @param firstName optional first-name filter
     * @param lastName optional last-name filter
     * @return matching external borrowers
     * @throws AccessDeniedException if the current user is not a librarian or admin
     * @throws ResourceNotFoundException if the external school does not exist
     * @throws IllegalArgumentException if the selected school is the current user's school
     */
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

        School currentUserSchool = currentUserService.getCurrentUserSchool(authentication);
        if (currentUserSchool.getId() != null && currentUserSchool.getId().equals(schoolId)) {
            throw new IllegalArgumentException("Kies een andere school voor extern uitlenen.");
        }

        Map<String, Role> localRolesByUserId = userRepository.findBySchoolId(schoolId).stream()
                .collect(Collectors.toMap(User::getId, u -> u.getRole().getRole()));
        List<Map<String, Object>> rosterUsers = oneRosterClient.fetchUsers(school.getDomainName());
        Map<String, String> oneRosterGroupNamesByUserId = getOneRosterGroupNamesByUserId(
                school.getDomainName(),
                null,
                rosterUsers,
                false);

        return rosterUsers.stream()
                .map(user -> toAuthUserDto(user, school))
                .filter(Objects::nonNull)
                .map(dto -> withLocalRole(dto, localRolesByUserId.get(dto.id())))
                .map(dto -> withPrimaryGroupName(dto, oneRosterGroupNamesByUserId.get(dto.id())))
                .filter(this::isAllowedExternalBorrower)
                .filter(dto -> matchesFilter(dto.firstName(), firstName))
                .filter(dto -> matchesFilter(dto.lastName(), lastName))
                .toList();
    }

    /**
     * Finds OneRoster teacher users for a school and returns them with effective local teacher, librarian, or admin roles.
     *
     * @param authentication current authenticated user
     * @param schoolId school identifier
     * @return users for the school with effective local roles
     * @throws AccessDeniedException if the current user is not an admin
     * @throws ResourceNotFoundException if the school does not exist
     */
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

    public User createUser(String userId, RoleEntity role, School school) {
        if (userRepository.existsById(userId)) {
            throw new RuntimeException("Gebruiker bestaat al");
        }

        User user = new User();
        user.setId(userId);

        user.setRole(Objects.requireNonNull(role, "role mag niet null zijn"));
        user.setSchool(Objects.requireNonNull(school, "school mag niet null zijn"));

        return userRepository.save(user);
    }

    public AuthUserDto setReviewBlocked(String userId, boolean blocked) {
        User user = userRepository.findById(userId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("userId", "Gebruiker niet gevonden met id: " + userId));

        user.setReviewBlocked(blocked);
        User updatedUser = userRepository.save(user);
        return userMapper.toDto(updatedUser);
    }

    public List<AuthUserDto> getReviewBlockedUsers(Authentication authentication) {
        boolean isModerator = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_LIBRARIAN"))
                || authentication.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isModerator) {
            throw new AccessDeniedException("Alleen moderators kunnen geblokkeerde leerlingen bekijken.");
        }

        School school = currentUserService.getCurrentUserSchool(authentication);
        String userId = currentUserService.extractCurrentUserId(authentication);

        Set<String> blockedUserIds = userRepository.findBySchoolIdAndReviewBlockedTrue(school.getId())
                .stream()
                .map(User::getId)
                .collect(Collectors.toSet());

        if (blockedUserIds.isEmpty()) {
            return List.of();
        }

        String schoolCode = school.getDomainName();
        String smartschoolToken = tokenStorageService.getSmartschoolAccessToken(userId);
        List<Map<String, Object>> rosterUsers = oneRosterClient.fetchUsersWithBearer(schoolCode, smartschoolToken);

        return rosterUsers.stream()
                .map(user -> toAuthUserDto(user, school))
                .filter(Objects::nonNull)
                .filter(dto -> blockedUserIds.contains(dto.id()))
                .map(dto -> new AuthUserDto(
                        dto.id(), dto.firstName(), dto.lastName(), dto.fullName(),
                        dto.role(), dto.email(), dto.username(),
                        dto.smartschoolId(), dto.platform(), dto.primaryGroupName(),
                        dto.parentGroupNames(), true,
                        dto.schoolId(), dto.schoolName(), dto.campusId(), dto.campusName(),
                        dto.school()))
                .toList();
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

    /**
     * Maps a raw OneRoster user to an auth user DTO in the supplied school context.
     *
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
        String primaryGroupName = resolveGroupName(rosterUser);

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
                primaryGroupName,
                List.of(),
                false,
                null,
                null,
                null,
                null,
                schoolSummary);
    }

    /**
     * Maps a OneRoster role string to the local role enum.
     *
     * @param roleStr OneRoster role value
     * @return matching local role, or null when unknown
     */
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

    /**
     * Checks whether a user DTO matches all supplied user filters.
     *
     * @param dto user DTO to inspect
     * @param options filters to apply
     * @return true when all filters match or no options are supplied
     */
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

    /**
     * Checks whether a value contains the supplied filter text.
     *
     * @param value value to inspect
     * @param filter optional filter text
     * @return true when the filter is blank or contained in the value
     */
    private boolean matchesFilter(String value, String filter) {
        if (!StringUtils.hasText(filter)) {
            return true;
        }

        if (!StringUtils.hasText(value)) {
            return false;
        }

        return value.toLowerCase().contains(filter.trim().toLowerCase());
    }

    /**
     * Resolves a displayable group name from OneRoster metadata.
     *
     * @param rosterUser raw OneRoster user map
     * @return resolved group name, or null when unavailable
     */
    private String resolveGroupName(Map<String, Object> rosterUser) {
        String direct = readMetadataString(rosterUser, "smsc.classInfo");
        if (StringUtils.hasText(direct)) {
            return direct;
        }

        String classLevel = readMetadataString(rosterUser, "smsc.classLevel");
        String classNumber = readMetadataString(rosterUser, "smsc.classNumber");
        if (StringUtils.hasText(classLevel) && StringUtils.hasText(classNumber)) {
            return classLevel + classNumber;
        }

        String classYear = readMetadataString(rosterUser, "smsc.classYear");
        if (StringUtils.hasText(classYear)) {
            return classYear;
        }

        return null;
    }

    /**
     * Loads the first OneRoster class or group name for each user in a school.
     *
     * @param schoolCode school identifier
     * @return group names keyed by user id
     */
    private Map<String, String> getOneRosterGroupNamesByUserId(
            String schoolCode,
            String bearerToken,
            List<Map<String, Object>> rosterUsers,
            boolean useBearer
    ) {
        if (rosterUsers == null || rosterUsers.isEmpty()) {
            return Map.of();
        }

        Map<String, String> appUserIdsByOneRosterUserId = new HashMap<>();
        for (Map<String, Object> rosterUser : rosterUsers) {
            String appUserId = readMetadataString(rosterUser, "smsc.legacyIdentifier");
            String oneRosterUserId = firstNonBlank(rosterUser, "sourcedId", "userId");

            if (StringUtils.hasText(appUserId) && StringUtils.hasText(oneRosterUserId)) {
                appUserIdsByOneRosterUserId.put(oneRosterUserId, appUserId);
            }
        }

        if (appUserIdsByOneRosterUserId.isEmpty()) {
            return Map.of();
        }

        List<OneRosterClassDTO> classes = useBearer
                ? oneRosterClient.fetchClassesWithBearer(schoolCode, bearerToken)
                : oneRosterClient.fetchClasses(schoolCode);

        Map<String, String> classNamesBySourcedId = safeList(classes).stream()
                .filter(oneRosterClass -> StringUtils.hasText(oneRosterClass.sourcedId()))
                .collect(Collectors.toMap(
                        OneRosterClassDTO::sourcedId,
                        this::resolveOneRosterClassName,
                        (left, right) -> left));

        if (classNamesBySourcedId.isEmpty()) {
            return Map.of();
        }

        List<OneRosterEnrollmentDTO> enrollments = useBearer
                ? oneRosterClient.fetchEnrollmentsWithBearer(schoolCode, bearerToken)
                : oneRosterClient.fetchEnrollments(schoolCode);

        Map<String, String> groupNamesByUserId = new HashMap<>();
        for (OneRosterEnrollmentDTO enrollment : safeList(enrollments)) {
            String appUserId = appUserIdsByOneRosterUserId.get(enrollment.userSourcedId());
            String className = classNamesBySourcedId.get(enrollment.classSourcedId());

            if (StringUtils.hasText(appUserId) && StringUtils.hasText(className)) {
                groupNamesByUserId.putIfAbsent(appUserId, className);
            }
        }

        return groupNamesByUserId;
    }

    private <T> List<T> safeList(List<T> items) {
        return items == null ? List.of() : items;
    }

    private String resolveOneRosterClassName(OneRosterClassDTO oneRosterClass) {
        if (StringUtils.hasText(oneRosterClass.title())) {
            return oneRosterClass.title();
        }

        if (StringUtils.hasText(oneRosterClass.classCode())) {
            return oneRosterClass.classCode();
        }

        return oneRosterClass.sourcedId();
    }

    private Map<String, List<String>> buildEnrollmentGroups(
            String schoolCode, String token, List<Map<String, Object>> rosterUsers) {
        Map<String, String> sourcedIdToLegacyId = rosterUsers.stream()
                .filter(u -> readMetadataString(u, "smsc.legacyIdentifier") != null)
                .collect(Collectors.toMap(
                        u -> firstNonBlank(u, "sourcedId"),
                        u -> readMetadataString(u, "smsc.legacyIdentifier"),
                        (a, b) -> a));

        List<OneRosterEnrollmentDTO> enrollments = oneRosterClient.fetchEnrollmentsWithBearer(schoolCode, token);
        List<OneRosterClassDTO> classes = oneRosterClient.fetchClassesWithBearer(schoolCode, token);

        Map<String, String> classNames = classes.stream()
                .filter(c -> c.sourcedId() != null && c.title() != null)
                .collect(Collectors.toMap(
                        OneRosterClassDTO::sourcedId,
                        OneRosterClassDTO::title,
                        (a, b) -> a));

        return enrollments.stream()
                .filter(e -> e.userSourcedId() != null && e.classSourcedId() != null)
                .collect(Collectors.groupingBy(
                        e -> sourcedIdToLegacyId.getOrDefault(e.userSourcedId(), e.userSourcedId()),
                        Collectors.mapping(
                                e -> classNames.getOrDefault(e.classSourcedId(), e.classSourcedId()),
                                Collectors.toList())));
    }

    private AuthUserDto withEnrollmentGroups(AuthUserDto dto, List<String> groupNames) {
        if (groupNames == null || groupNames.isEmpty()) {
            return dto;
        }

        String primary = StringUtils.hasText(dto.primaryGroupName())
                ? dto.primaryGroupName()
                : groupNames.get(0);
        List<String> parents = (dto.parentGroupNames() != null && !dto.parentGroupNames().isEmpty())
                ? dto.parentGroupNames()
                : (groupNames.size() > 1 ? groupNames.subList(1, groupNames.size()) : List.of());

        return new AuthUserDto(
                dto.id(), dto.firstName(), dto.lastName(), dto.fullName(),
                dto.role(), dto.email(), dto.username(),
                dto.smartschoolId(), dto.platform(),
                primary,
                parents,
                dto.reviewBlocked(),
                dto.schoolId(), dto.schoolName(), dto.campusId(), dto.campusName(),
                dto.school());
    }

    private AuthUserDto withLocalGroupName(AuthUserDto dto, String localGroupName) {
        if (StringUtils.hasText(dto.primaryGroupName()) || !StringUtils.hasText(localGroupName)) {
            return dto;
        }

        return new AuthUserDto(
                dto.id(), dto.firstName(), dto.lastName(), dto.fullName(),
                dto.role(), dto.email(), dto.username(),
                dto.smartschoolId(), dto.platform(), localGroupName,
                dto.parentGroupNames(), dto.reviewBlocked(),
                dto.schoolId(), dto.schoolName(), dto.campusId(), dto.campusName(),
                dto.school());
    }

    /**
     * Adds a primary group name when the DTO has no primary group name.
     *
     * @param dto user DTO to update
     * @param primaryGroupName primary group name
     * @return original or updated DTO
     */
    private AuthUserDto withPrimaryGroupName(AuthUserDto dto, String primaryGroupName) {
        if (StringUtils.hasText(dto.primaryGroupName()) || !StringUtils.hasText(primaryGroupName)) {
            return dto;
        }

        return new AuthUserDto(
                dto.id(), dto.firstName(), dto.lastName(), dto.fullName(),
                dto.role(), dto.email(), dto.username(),
                dto.smartschoolId(), dto.platform(), primaryGroupName,
                dto.parentGroupNames(), dto.reviewBlocked(),
                dto.schoolId(), dto.schoolName(), dto.campusId(), dto.campusName(),
                dto.school());
    }

    /**
     * Replaces the DTO role with an effective local role when present.
     *
     * @param dto user DTO to update
     * @param actualRole effective local role
     * @return original or updated DTO
     */
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

    /**
     * Checks whether an external user role may borrow books.
     *
     * @param dto user DTO to inspect
     * @return true for teachers, librarians, and admins
     */
    private boolean isAllowedExternalBorrower(AuthUserDto dto) {
        Role role = dto.role();
        return role == Role.TEACHER || role == Role.LIBRARIAN || role == Role.ADMIN;
    }

    /**
     * Returns the first non-blank string value for the supplied keys.
     *
     * @param source source map
     * @param keys keys to inspect in order
     * @return first trimmed non-blank value, or null
     */
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

    /**
     * Reads a non-blank string value from a user's metadata map.
     *
     * @param user raw OneRoster user map
     * @param key metadata key
     * @return trimmed metadata value, or null
     */
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