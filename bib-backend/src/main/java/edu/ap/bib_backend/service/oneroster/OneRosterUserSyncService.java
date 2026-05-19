package edu.ap.bib_backend.service.oneroster;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import edu.ap.bib_backend.jpa.repository.GroupRepository;
import edu.ap.bib_backend.jpa.repository.OneRosterCredentialRepository;
import edu.ap.bib_backend.jpa.repository.SchoolRepository;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import edu.ap.bib_backend.service.RoleService;

@Service
public class OneRosterUserSyncService {

    private final OneRosterClient oneRosterClient;
    private final OneRosterCredentialRepository credentialRepository;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final RoleService roleService;

    public OneRosterUserSyncService(
            OneRosterClient oneRosterClient,
            OneRosterCredentialRepository credentialRepository,
            SchoolRepository schoolRepository,
            UserRepository userRepository,
            GroupRepository groupRepository,
            RoleService roleService) {
        this.oneRosterClient = oneRosterClient;
        this.credentialRepository = credentialRepository;
        this.schoolRepository = schoolRepository;
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.roleService = roleService;
    }

    @Async
    public void syncSchoolAsync(School school) {
        syncSchool(school);
    }

    @Transactional
    public void syncSchool(School school) {
        String schoolCode = school.getDomainName();
        syncClasses(school, schoolCode);
        syncUsers(school, schoolCode);
        syncEnrollments(school, schoolCode);
    }

    public void syncAllSchools() {
        List<OneRosterCredential> credentials = credentialRepository.findAll();
        for (OneRosterCredential credential : credentials) {
            School school = resolveSchool(credential);
            if (school == null) {
                continue;
            }
            syncSchool(school);
        }
    }

    private void syncClasses(School school, String schoolCode) {
        List<OneRosterClassDTO> classes = oneRosterClient.fetchClasses(schoolCode);
        for (OneRosterClassDTO oneRosterClass : classes) {
            String groupName = resolveGroupName(oneRosterClass);
            if (!StringUtils.hasText(groupName)) {
                continue;
            }

            groupRepository.findBySchoolIdAndName(school.getId(), groupName)
                    .orElseGet(() -> {
                        Group group = new Group();
                        group.setName(groupName);
                        group.setSchool(school);
                        return groupRepository.save(group);
                    });
        }
    }

    private void syncUsers(School school, String schoolCode) {
        List<Map<String, Object>> rosterUsers = oneRosterClient.fetchUsers(schoolCode);
        for (Map<String, Object> rosterUser : rosterUsers) {
            String legacyIdentifier = readMetadataString(rosterUser, "smsc.legacyIdentifier");
            if (!StringUtils.hasText(legacyIdentifier)) {
                continue;
            }

            String roleName = firstNonBlank(rosterUser, "role");
            User user = userRepository.findById(legacyIdentifier)
                    .orElseGet(() -> createUser(legacyIdentifier, roleName, school));

            if (user.getSchool() == null) {
                user.setSchool(school);
            }

            String sourcedId = firstNonBlank(rosterUser, "sourcedId");
            if (StringUtils.hasText(sourcedId)) {
                user.setOneRosterSourcedId(sourcedId);
            }

            userRepository.save(user);
        }
    }

    private void syncEnrollments(School school, String schoolCode) {
        List<OneRosterEnrollmentDTO> enrollments = oneRosterClient.fetchEnrollments(schoolCode);

        Map<String, Set<String>> userToClassSourcedIds = new HashMap<>();
        for (OneRosterEnrollmentDTO enrollment : enrollments) {
            userToClassSourcedIds
                    .computeIfAbsent(enrollment.userSourcedId(), k -> new HashSet<>())
                    .add(enrollment.classSourcedId());
        }

        for (Map.Entry<String, Set<String>> entry : userToClassSourcedIds.entrySet()) {
            String userSourcedId = entry.getKey();
            Set<String> classSourcedIds = entry.getValue();

            User user = findUserByOneRosterSourcedId(userSourcedId);
            if (user == null) {
                continue;
            }

            Set<Group> newGroups = new HashSet<>();
            for (String classSourcedId : classSourcedIds) {
                Group group = findGroupBySourcedId(school, classSourcedId);
                if (group != null) {
                    newGroups.add(group);
                }
            }

            user.setGroups(newGroups);
            userRepository.save(user);
        }
    }

    private User findUserByOneRosterSourcedId(String oneRosterSourcedId) {
        return userRepository.findAll().stream()
                .filter(u -> oneRosterSourcedId.equals(u.getOneRosterSourcedId()))
                .findFirst()
                .orElse(null);
    }

    private Group findGroupBySourcedId(School school, String classSourcedId) {
        return groupRepository.findAll().stream()
                .filter(g -> g.getSchool().getId().equals(school.getId()))
                .filter(g -> {
                    String normalizedGroup = normalizeForMatching(g.getName());
                    String normalizedSource = normalizeForMatching(classSourcedId);
                    return normalizedGroup.equals(normalizedSource);
                })
                .findFirst()
                .orElse(null);
    }

    private String normalizeForMatching(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
    }

    private String resolveGroupName(OneRosterClassDTO oneRosterClass) {
        if (StringUtils.hasText(oneRosterClass.classCode())) {
            return oneRosterClass.classCode().trim();
        }
        if (StringUtils.hasText(oneRosterClass.title())) {
            return oneRosterClass.title().trim();
        }
        return null;
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
            return "leerling";
        }
        return roleName;
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
}
