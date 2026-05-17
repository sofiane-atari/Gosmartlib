package edu.ap.bib_backend.service.oneroster;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import edu.ap.bib_backend.jpa.entity.Group;
import edu.ap.bib_backend.jpa.entity.OneRosterCredential;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.repository.OneRosterCredentialRepository;
import edu.ap.bib_backend.jpa.repository.SchoolRepository;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import edu.ap.bib_backend.service.GroupService;
import edu.ap.bib_backend.service.RoleService;

@Service
public class OneRosterUserSyncService {

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

    @Transactional
    public void syncAllSchools() {
        List<OneRosterCredential> credentials = credentialRepository.findAll();
        for (OneRosterCredential credential : credentials) {
            School school = resolveSchool(credential);
            if (school == null) {
                continue;
            }
            syncSchoolUsers(school);
        }
    }

    private void syncSchoolUsers(School school) {
        String schoolCode = school.getDomainName();
        List<Map<String, Object>> rosterUsers = oneRosterClient.fetchUsers(schoolCode);
        for (Map<String, Object> rosterUser : rosterUsers) {
            upsertUserFromRoster(school, rosterUser);
        }
    }

    private void upsertUserFromRoster(School school, Map<String, Object> rosterUser) {
        String legacyIdentifier = readMetadataString(rosterUser, "smsc.legacyIdentifier");
        if (!StringUtils.hasText(legacyIdentifier)) {
            return;
        }

        String roleName = firstNonBlank(rosterUser, "role");
        User user = userRepository.findById(legacyIdentifier)
                .orElseGet(() -> createUser(legacyIdentifier, roleName, school));

        if (user.getSchool() == null) {
            user.setSchool(school);
        }

        String groupName = resolveGroupName(rosterUser);
        if (StringUtils.hasText(groupName)) {
            Group group = resolveGroup(groupName);
            if (group != null) {
                user.setGroup(group);
            }
        }

        String sourcedId = firstNonBlank(rosterUser, "sourcedId");
        if (StringUtils.hasText(sourcedId)) {
            user.setOneRosterSourcedId(sourcedId);
        }

        userRepository.save(user);
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

    private Group resolveGroup(String groupName) {
        try {
            return groupService.getGroup(groupName);
        } catch (RuntimeException ex) {
            try {
                return groupService.createGroup(groupName);
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
}
