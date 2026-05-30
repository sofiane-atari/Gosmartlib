package edu.ap.bib_backend.service.oneroster;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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

    /**
     * Starts an asynchronous OneRoster user sync for one school.
     *
     * @param school school to sync
     */
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

    /**
     * Fetches OneRoster users for a school and upserts them locally.
     *
     * @param school school to sync
     * @return counts for fetched, saved, and skipped users
     */
    @Transactional
    public SyncResult syncSchoolUsers(School school) {
        String schoolCode = school.getDomainName();
        logger.info("Fetching OneRoster users for school id={} name='{}' domain='{}'",
                school.getId(),
                school.getName(),
                schoolCode);

        List<Map<String, Object>> rosterUsers = oneRosterClient.fetchUsers(schoolCode);

        int saved = 0;
        int skipped = 0;

        for (Map<String, Object> rosterUser : rosterUsers) {
            if (upsertUserFromRoster(school, rosterUser)) {
                saved++;
            } else {
                skipped++;
            }
        }

        return new SyncResult(rosterUsers.size(), saved, skipped);
    }

    /**
     * Syncs users for all configured active schools.
     */
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

    /**
     * Creates or updates one local user from OneRoster data.
     *
     * @param school school that owns the roster user
     * @param rosterUser raw OneRoster user payload
     * @return true when the user was saved, false when skipped
     */
    private boolean upsertUserFromRoster(School school, Map<String, Object> rosterUser) {
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

        if (StringUtils.hasText(groupName)) {
            Group group = resolveGroup(school.getId(), groupName);
            if (group != null) {
                user.getGroups().add(group);
                logger.info("Assigned user {} to class {}", legacyIdentifier, groupName);
            } else {
                logger.warn("Failed to resolve or create class {} for user {}", groupName, legacyIdentifier);
            }
        } else {
            logger.info("User {} has no class metadata associated", legacyIdentifier);
        }

        if (StringUtils.hasText(sourcedId)) {
            user.setOneRosterSourcedId(sourcedId);
        }

        userRepository.save(user);
        return true;
    }

    /**
     * Creates a new local user with the resolved role and school.
     *
     * @param userId local user identifier
     * @param roleName OneRoster role name
     * @param school school assigned to the user
     * @return unsaved user entity
     */
    private User createUser(String userId, String roleName, School school) {
        User user = new User();
        user.setId(userId);
        user.setRole(roleService.getRoleEntity(resolveRoleName(roleName)));
        user.setSchool(school);
        return user;
    }

    /**
     * Maps a OneRoster role name to a local role name.
     *
     * @param roleName OneRoster role name
     * @return local role name
     */
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

    /**
     * Finds or creates a group for a school.
     *
     * @param schoolId school identifier
     * @param groupName group name
     * @return resolved group, or null when it cannot be resolved
     */
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

    /**
     * Resolves a class or group name from OneRoster metadata.
     *
     * @param rosterUser raw OneRoster user payload
     * @return group name, or null when no class metadata is present
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
     * Resolves the school referenced by a OneRoster credential.
     *
     * @param credential OneRoster credential
     * @return persisted school, or null when unresolved
     */
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

    /**
     * Returns the first non-blank string value for the given keys.
     *
     * @param source source map
     * @param keys keys to inspect in order
     * @return trimmed value, or null when none is present
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
     * Reads a non-blank string value from the metadata map.
     *
     * @param user raw OneRoster user payload
     * @param key metadata key
     * @return trimmed metadata value, or null when absent
     */
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
