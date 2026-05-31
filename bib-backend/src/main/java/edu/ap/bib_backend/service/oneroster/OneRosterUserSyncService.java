package edu.ap.bib_backend.service.oneroster;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import edu.ap.bib_backend.dto.oneroster.OneRosterClassDTO;
import edu.ap.bib_backend.dto.oneroster.OneRosterEnrollmentDTO;
import edu.ap.bib_backend.jpa.entity.Group;
import edu.ap.bib_backend.jpa.entity.OneRosterCredential;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.enums.SchoolStatus;
import edu.ap.bib_backend.jpa.repository.OneRosterCredentialRepository;
import edu.ap.bib_backend.jpa.repository.SchoolRepository;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import edu.ap.bib_backend.service.GroupService;
import edu.ap.bib_backend.service.RoleService;

@Service
public class OneRosterUserSyncService {

    private static final Logger logger = LoggerFactory.getLogger(OneRosterUserSyncService.class);

    private final OneRosterClient oneRosterClient;
    private final OneRosterCredentialRepository credentialRepository;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final RoleService roleService;
    private final GroupService groupService;

    public OneRosterUserSyncService(
            OneRosterClient oneRosterClient,
            OneRosterCredentialRepository credentialRepository,
            SchoolRepository schoolRepository,
            UserRepository userRepository,
            RoleService roleService,
            GroupService groupService) {
        this.oneRosterClient = oneRosterClient;
        this.credentialRepository = credentialRepository;
        this.schoolRepository = schoolRepository;
        this.userRepository = userRepository;
        this.roleService = roleService;
        this.groupService = groupService;
    }

    @Async
    @Transactional
    public void syncSchoolAsync(School school) {
        try {
            syncSchoolUsers(school);
        } catch (Exception e) {
            logger.error("Async user sync failed for school id={} name='{}'",
                    school.getId(), school.getName(), e);
        }
    }

    @Transactional
    public SyncResult syncSchoolUsers(School school) {
        String schoolCode = school.getDomainName();
        logger.info("Fetching OneRoster users for school id={} name='{}' domain='{}'",
                school.getId(),
                school.getName(),
                schoolCode);

        List<Map<String, Object>> rosterUsers = oneRosterClient.fetchUsers(schoolCode);

        List<OneRosterEnrollmentDTO> enrollments = oneRosterClient.fetchEnrollments(schoolCode);
        List<OneRosterClassDTO> classes = oneRosterClient.fetchClasses(schoolCode);

        Map<String, String> classNames = classes.stream()
                .filter(c -> c.sourcedId() != null && c.title() != null)
                .collect(Collectors.toMap(
                        OneRosterClassDTO::sourcedId,
                        OneRosterClassDTO::title,
                        (a, b) -> a));

        Map<String, Set<String>> enrollmentGroups = enrollments.stream()
                .filter(e -> e.userSourcedId() != null && e.classSourcedId() != null)
                .collect(Collectors.groupingBy(
                        OneRosterEnrollmentDTO::userSourcedId,
                        Collectors.mapping(
                                e -> classNames.getOrDefault(e.classSourcedId(), e.classSourcedId()),
                                Collectors.toSet())));

        logger.info("Fetched {} enrollments across {} classes for school id={}",
                enrollments.size(), classes.size(), school.getId());

        int saved = 0;
        int skipped = 0;

        for (Map<String, Object> rosterUser : rosterUsers) {
            if (upsertUserFromRoster(school, rosterUser, enrollmentGroups)) {
                saved++;
            } else {
                skipped++;
            }
        }

        return new SyncResult(rosterUsers.size(), saved, skipped);
    }

    @Transactional
    public void syncAllSchools() {
        List<OneRosterCredential> credentials = credentialRepository.findAll();

        logger.info("Starting OneRoster user sync for {} configured school credential(s)", credentials.size());

        int successCount = 0;
        int failureCount = 0;
        int skippedCount = 0;

        for (OneRosterCredential credential : credentials) {
            School school = resolveSchool(credential);
                if (school == null) {
                    skippedCount++;
                    logger.warn("Skipping OneRoster user sync because credential {} does not resolve to a school",
                            credential == null ? null : credential.getId());
                    continue;
                }

                if (school.getStatus() != SchoolStatus.ACTIVE) {
                    skippedCount++;
                    logger.info("Skipping OneRoster user sync for non-active school id={} name='{}'",
                            school.getId(), school.getName());
                    continue;
                }

            try {
                SyncResult result = syncSchoolUsers(school);
                successCount++;
                school.setLastSyncAt(LocalDateTime.now());
                schoolRepository.save(school);

                logger.info(
                        "OneRoster user sync succeeded for school id={} name='{}' domain='{}': fetched={}, saved={}, skipped={}",
                        school.getId(),
                        school.getName(),
                        school.getDomainName(),
                        result.fetched(),
                        result.saved(),
                        result.skipped());
            } catch (RuntimeException ex) {
                failureCount++;
                logger.error(
                        "OneRoster user sync failed for school id={} name='{}' domain='{}'",
                        school.getId(),
                        school.getName(),
                        school.getDomainName(),
                        ex);
            }
        }

        logger.info(
                "Finished OneRoster user sync: success={}, failed={}, skippedCredentials={}",
                successCount,
                failureCount,
                skippedCount);
    }

    private boolean upsertUserFromRoster(School school, Map<String, Object> rosterUser,
                                         Map<String, Set<String>> enrollmentGroups) {
        String legacyIdentifier = readMetadataString(rosterUser, "smsc.legacyIdentifier");
        String sourcedId = firstNonBlank(rosterUser, "sourcedId");
        String roleName = firstNonBlank(rosterUser, "role");
        String groupName = resolveGroupName(rosterUser);

        logger.info("OneRoster sync - SourcedId: {}, LegacyIdentifier: {}, Role: {}, Resolved Class: {}",
                sourcedId, legacyIdentifier, roleName, groupName);

        if (!StringUtils.hasText(legacyIdentifier)) {
            logger.warn("Skipping OneRoster user for school id={} because smsc.legacyIdentifier is missing (sourcedId={})",
                    school.getId(), sourcedId);
            return false;
        }

        User user = userRepository.findById(legacyIdentifier)
                .orElseGet(() -> createUser(legacyIdentifier, roleName, school));

        if (user.getSchool() == null) {
            user.setSchool(school);
        }

        user.getGroups().clear();

        boolean assignedFromEnrollment = false;
        if (sourcedId != null && enrollmentGroups.containsKey(sourcedId)) {
            Set<String> userClasses = enrollmentGroups.get(sourcedId);
            for (String className : userClasses) {
                Group group = resolveGroup(school.getId(), className);
                if (group != null) {
                    user.getGroups().add(group);
                    assignedFromEnrollment = true;
                    logger.info("Assigned user {} to class {} (from enrollment)", legacyIdentifier, className);
                } else {
                    logger.warn("Failed to resolve or create class {} for user {} (from enrollment)", className, legacyIdentifier);
                }
            }
        }

        if (!assignedFromEnrollment && StringUtils.hasText(groupName)) {
            Group group = resolveGroup(school.getId(), groupName);
            if (group != null) {
                user.getGroups().add(group);
                logger.info("Assigned user {} to class {} (from metadata)", legacyIdentifier, groupName);
            } else {
                logger.warn("Failed to resolve or create class {} for user {}", groupName, legacyIdentifier);
            }
        }

        if (!assignedFromEnrollment && !StringUtils.hasText(groupName)) {
            logger.info("User {} has no class associated (no enrollment, no metadata)", legacyIdentifier);
        }

        if (StringUtils.hasText(sourcedId)) {
            user.setOneRosterSourcedId(sourcedId);
        }

        userRepository.save(user);
        return true;
    }

    private User createUser(String userId, String roleName, School school) {
        User user = new User();
        user.setId(userId);
        user.setRole(roleService.getRoleEntity(resolveRoleName(roleName)));
        user.setSchool(school);
        return user;
    }

    private String resolveRoleName(String roleName) {
        if (!StringUtils.hasText(roleName)) {
            return "STUDENT";
        }
        return switch (roleName.toLowerCase()) {
            case "teacher" -> "TEACHER";
            case "student" -> "STUDENT";
            case "administrator" -> "TEACHER";
            default -> "STUDENT";
        };
    }

    private Group resolveGroup(Long schoolId, String groupName) {
        try {
            return groupService.getGroup(schoolId, groupName);
        } catch (RuntimeException ex) {
            try {
                return groupService.createGroup(schoolId, groupName);
            } catch (RuntimeException ignored) {
                return null;
            }
        }
    }

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

    private School resolveSchool(OneRosterCredential credential) {
        if (credential == null || credential.getSchool() == null) {
            return null;
        }

        School school = credential.getSchool();
        if (school.getId() != null) {
            return schoolRepository.findById(school.getId()).orElse(null);
        }
        if (StringUtils.hasText(school.getDomainName())) {
            return schoolRepository.findByDomainName(school.getDomainName()).orElse(null);
        }
        return null;
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

    public record SyncResult(int fetched, int saved, int skipped) {
    }
}
