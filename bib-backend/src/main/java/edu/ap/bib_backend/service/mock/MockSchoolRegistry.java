package edu.ap.bib_backend.service.mock;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MockSchoolRegistry {

    private final Map<String, MockSchoolData> schoolsByCode = new ConcurrentHashMap<>();
    private final Map<String, MockSchoolData> schoolsByOnRosterClientId = new ConcurrentHashMap<>();
    private final Map<String, MockSchoolData> schoolsByDomain = new ConcurrentHashMap<>();
    private final Map<String, MockSchoolData.MockUser> usersByAuthCode = new ConcurrentHashMap<>();
    private final Map<String, MockSchoolData> schoolByAuthCode = new ConcurrentHashMap<>();

    private volatile MockSchoolData activeSchool;
    private volatile boolean passthroughMode = false;

    public MockSchoolRegistry(
            @Value("${school.seed.domain:}") String schoolDomain,
            @Value("${school.seed.name:}") String schoolName,
            @Value("${school.seed.oneroster-client-id:}") String onerosterClientId,
            @Value("${school.seed.oneroster-client-secret:}") String onerosterClientSecret
    ) {
        MockSchoolData school1 = MockSchoolData.createFromEnvVars(
                schoolDomain, schoolName, onerosterClientId, onerosterClientSecret
        );
        MockSchoolData school2 = MockSchoolData.createTestSchool2();

        schoolsByCode.put(school1.getSchoolCode(), school1);
        schoolsByCode.put(school2.getSchoolCode(), school2);

        schoolsByOnRosterClientId.put(school1.getOnRosterClientId(), school1);
        schoolsByOnRosterClientId.put(school2.getOnRosterClientId(), school2);

        schoolsByDomain.put(school1.getDomainName(), school1);
        schoolsByDomain.put(school2.getDomainName(), school2);

        this.activeSchool = school1;
    }

    public List<MockSchoolData> getAllSchools() {
        return List.copyOf(schoolsByCode.values());
    }

    public MockSchoolData getActiveSchool() {
        return activeSchool;
    }

    public void setActiveSchool(String schoolCode) {
        MockSchoolData school = schoolsByCode.get(schoolCode);
        if (school != null) {
            activeSchool = school;
            passthroughMode = false;
        }
    }

    public void setPassthroughMode(boolean enabled) {
        passthroughMode = enabled;
        if (enabled) {
            MockSchoolData aph = schoolsByCode.get("aphogeschool");
            if (aph != null) {
                activeSchool = aph;
            }
        }
    }

    public boolean isPassthroughMode() {
        return passthroughMode;
    }

    public MockSchoolData getSchoolByOnRosterClientId(String clientId) {
        return schoolsByOnRosterClientId.get(clientId);
    }

    public MockSchoolData getSchoolByDomain(String domain) {
        return schoolsByDomain.get(domain);
    }

    public MockSchoolData getSchoolByCode(String code) {
        return schoolsByCode.get(code);
    }

    public MockSchoolData.MockUser getUserByLegacyId(String legacyId) {
        for (MockSchoolData school : schoolsByCode.values()) {
            MockSchoolData.MockUser user = school.getUserByLegacyId(legacyId);
            if (user != null) {
                return user;
            }
        }
        return null;
    }

    public MockSchoolData getSchoolByLegacyId(String legacyId) {
        for (MockSchoolData school : schoolsByCode.values()) {
            if (school.getUserByLegacyId(legacyId) != null) {
                return school;
            }
        }
        return null;
    }

    public String generateAuthCode(MockSchoolData.MockUser user, MockSchoolData school) {
        String code = "mock-code-" + user.username + "-" + System.currentTimeMillis();
        usersByAuthCode.put(code, user);
        schoolByAuthCode.put(code, school);
        return code;
    }

    public MockSchoolData.MockUser resolveUserFromAuthCode(String code) {
        return usersByAuthCode.get(code);
    }

    public MockSchoolData getSchoolByAuthCode(String code) {
        return schoolByAuthCode.get(code);
    }

    public record AuthResult(MockSchoolData.MockUser user, MockSchoolData school) {}

    public AuthResult authenticateUserAnySchool(String username, String password) {
        for (MockSchoolData school : schoolsByCode.values()) {
            MockSchoolData.MockUser user = school.getUserByUsername(username);
            if (user != null && user.password.equals(password)) {
                return new AuthResult(user, school);
            }
        }
        return null;
    }
}
