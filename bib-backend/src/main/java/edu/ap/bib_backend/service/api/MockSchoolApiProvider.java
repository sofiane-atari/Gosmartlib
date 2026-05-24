package edu.ap.bib_backend.service.api;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import edu.ap.bib_backend.dto.SmartschoolGroupDto;
import edu.ap.bib_backend.dto.SmartschoolProfileDto;
import edu.ap.bib_backend.service.JwtService;
import edu.ap.bib_backend.service.mock.MockSchoolData;
import edu.ap.bib_backend.service.mock.MockSchoolRegistry;

@Service
public class MockSchoolApiProvider implements SchoolApiProvider {

    private final MockSchoolRegistry registry;
    private final JwtService jwtService;

    public MockSchoolApiProvider(MockSchoolRegistry registry, JwtService jwtService) {
        this.registry = registry;
        this.jwtService = jwtService;
    }

    @Override
    public SmartschoolProfileDto fetchProfile(String accessToken) {
        MockSchoolData.MockUser user = resolveUserFromToken(accessToken);
        if (user == null) {
            throw new IllegalStateException("Mock user not found for token");
        }
        return new SmartschoolProfileDto(
                user.legacyIdentifier,
                user.givenName,
                user.familyName,
                user.familyName + " " + user.givenName,
                user.email,
                null
        );
    }

    @Override
    public SmartschoolGroupDto fetchGroups(String accessToken) {
        MockSchoolData.MockUser user = resolveUserFromToken(accessToken);
        if (user == null) {
            throw new IllegalStateException("Mock user not found for token");
        }
        return new SmartschoolGroupDto(user.groupName, user.parentGroupNames);
    }

    @Override
    public SmartschoolGroupDto fetchGroupsForUser(String legacyUserId) {
        MockSchoolData.MockUser user = registry.getUserByLegacyId(legacyUserId);
        if (user == null) {
            return new SmartschoolGroupDto(null, List.of());
        }
        return new SmartschoolGroupDto(user.groupName, user.parentGroupNames);
    }

    @Override
    public List<Map<String, Object>> fetchUsers(String schoolCode) {
        MockSchoolData school = registry.getSchoolByCode(schoolCode);
        if (school == null) {
            school = registry.getSchoolByDomain(schoolCode);
        }
        if (school == null) {
            return List.of();
        }
        String platformId = school.getPlatformId();
        String orgSourcedId = platformId;
        String schoolName = school.getSchoolName();
        return school.getUsers().stream()
                .map(u -> buildOnRosterUserRecord(u, platformId, orgSourcedId, schoolName))
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> fetchClasses(String schoolCode) {
        MockSchoolData school = resolveSchool(schoolCode);
        if (school == null) return List.of();
        return school.getClasses();
    }

    @Override
    public List<Map<String, Object>> fetchEnrollments(String schoolCode) {
        MockSchoolData school = resolveSchool(schoolCode);
        if (school == null) return List.of();
        return school.getEnrollments();
    }

    @Override
    public List<Map<String, Object>> fetchAcademicSessions(String schoolCode) {
        MockSchoolData school = resolveSchool(schoolCode);
        if (school == null) return List.of();
        return school.getAcademicSessions();
    }

    @Override
    public List<Map<String, Object>> fetchOrgs(String schoolCode) {
        MockSchoolData school = resolveSchool(schoolCode);
        if (school == null) return List.of();
        return school.getOrgs();
    }

    @Override
    public Map<String, String> fetchUserDisplayNames(String schoolCode, Set<String> userIds) {
        if (userIds == null || userIds.isEmpty()) return Map.of();
        MockSchoolData school = resolveSchool(schoolCode);
        if (school == null) return Map.of();
        return school.getUsers().stream()
                .filter(u -> userIds.contains(u.legacyIdentifier) || userIds.contains(u.onerosterSourcedId))
                .collect(Collectors.toMap(
                        u -> u.onerosterSourcedId,
                        u -> u.givenName + " " + u.familyName,
                        (a, b) -> a
                ));
    }

    @Override
    public String findSourcedIdByLegacyIdentifier(String schoolCode, String legacyIdentifier) {
        MockSchoolData school = resolveSchool(schoolCode);
        if (school == null) return null;
        MockSchoolData.MockUser user = school.getUserByLegacyId(legacyIdentifier);
        return user != null ? user.onerosterSourcedId : null;
    }

    @Override
    public List<Map<String, Object>> fetchStudentsForClass(String schoolCode, String classSourcedId) {
        MockSchoolData school = resolveSchool(schoolCode);
        if (school == null) return List.of();
        String platformId = school.getPlatformId();
        String orgSourcedId = platformId;
        String schoolName = school.getSchoolName();
        return school.getUsers().stream()
                .filter(u -> "Leerling".equals(u.basisrol) && u.classSourcedId.equals(classSourcedId))
                .map(u -> buildOnRosterUserRecord(u, platformId, orgSourcedId, schoolName))
                .collect(Collectors.toList());
    }

    private MockSchoolData.MockUser resolveUserFromToken(String accessToken) {
        try {
            String legacyId = jwtService.extractUsername(accessToken);
            if (legacyId != null) {
                return registry.getUserByLegacyId(legacyId);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private MockSchoolData resolveSchool(String schoolCode) {
        MockSchoolData school = registry.getSchoolByCode(schoolCode);
        if (school == null) {
            school = registry.getSchoolByDomain(schoolCode);
        }
        return school;
    }

    private Map<String, Object> buildOnRosterUserRecord(MockSchoolData.MockUser user, String platformId, String orgSourcedId, String schoolName) {
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("sourcedId", user.onerosterSourcedId);
        result.put("identifier", user.username);
        result.put("status", "active");
        result.put("username", user.username);
        result.put("givenName", user.givenName);
        result.put("familyName", user.familyName);
        result.put("email", user.email);
        result.put("role", user.getOnRosterRole());
        result.put("sex", user.sex);
        result.put("orgs", List.of(Map.of(
                "sourcedId", orgSourcedId,
                "status", "active",
                "name", schoolName,
                "type", "school",
                "identifier", "platform_" + platformId
        )));
        result.put("metadata", Map.of(
                "smsc.legacyIdentifier", user.legacyIdentifier,
                "smsc.classInfo", user.classCode,
                "smsc.classYear", user.classCode.substring(0, 1),
                "smsc.classLevel", "ASO",
                "smsc.classNumber", "1"
        ));
        return result;
    }
}
