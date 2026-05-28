package edu.ap.bib_backend.service.mock;

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

    public MockSchoolRegistry() {
        MockSchoolData goa = MockSchoolData.createTestSchool1();
        MockSchoolData kul = MockSchoolData.createTestSchool2();

        schoolsByCode.put(goa.getSchoolCode(), goa);
        schoolsByCode.put(kul.getSchoolCode(), kul);

        schoolsByOnRosterClientId.put(goa.getOnRosterClientId(), goa);
        schoolsByOnRosterClientId.put(kul.getOnRosterClientId(), kul);

        schoolsByDomain.put(goa.getDomainName(), goa);
        schoolsByDomain.put(kul.getDomainName(), kul);

        this.activeSchool = goa;
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
        }
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
