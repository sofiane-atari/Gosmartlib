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
    private final Map<String, MockSchoolData.MockUser> usersByToken = new ConcurrentHashMap<>();
    private final Map<String, MockSchoolData> schoolByToken = new ConcurrentHashMap<>();

    private volatile MockSchoolData activeSchool;
    private volatile boolean passthroughMode = false;

    public MockSchoolRegistry() {
        MockSchoolData school1 = MockSchoolData.createTestSchool1();
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

    public MockSchoolData.MockUser authenticateUser(String username, String password) {
        for (MockSchoolData school : schoolsByCode.values()) {
            MockSchoolData.MockUser user = school.getUserByUsername(username);
            if (user != null && user.password.equals(password)) {
                String token = school.generateMockAccessToken(user.username);
                usersByToken.put(token, user);
                schoolByToken.put(token, school);
                return user;
            }
        }
        return null;
    }

    public MockSchoolData.MockUser getUserByToken(String token) {
        return usersByToken.get(token);
    }

    public MockSchoolData getSchoolByToken(String token) {
        return schoolByToken.get(token);
    }

    public void registerToken(String token, MockSchoolData.MockUser user, MockSchoolData school) {
        usersByToken.put(token, user);
        schoolByToken.put(token, school);
    }

    public String generateAuthCode(MockSchoolData.MockUser user) {
        return "mock-code-" + user.username + "-" + System.currentTimeMillis();
    }

    public MockSchoolData.MockUser resolveUserFromAuthCode(String code) {
        if (code == null || !code.startsWith("mock-code-")) {
            return null;
        }
        String[] parts = code.split("-");
        if (parts.length < 3) {
            return null;
        }
        String username = parts[1] + "." + parts[2];
        for (MockSchoolData school : schoolsByCode.values()) {
            MockSchoolData.MockUser user = school.getUserByUsername(username);
            if (user != null) {
                return user;
            }
        }
        return null;
    }
}
