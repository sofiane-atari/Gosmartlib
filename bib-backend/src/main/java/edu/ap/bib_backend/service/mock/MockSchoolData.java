package edu.ap.bib_backend.service.mock;

import java.util.List;
import java.util.Map;

public class MockSchoolData {

    private final String schoolCode;
    private final String domainName;
    private final String platformUrl;
    private final String schoolName;
    private final String platformId;
    private final String onerosterClientId;
    private final String onerosterClientSecret;
    private final List<MockUser> users;
    private final List<Map<String, Object>> classes;
    private final List<Map<String, Object>> enrollments;
    private final List<Map<String, Object>> academicSessions;
    private final List<Map<String, Object>> orgs;

    public MockSchoolData(
            String schoolCode,
            String domainName,
            String platformUrl,
            String schoolName,
            String platformId,
            String onerosterClientId,
            String onerosterClientSecret,
            List<MockUser> users,
            List<Map<String, Object>> classes,
            List<Map<String, Object>> enrollments,
            List<Map<String, Object>> academicSessions,
            List<Map<String, Object>> orgs
    ) {
        this.schoolCode = schoolCode;
        this.domainName = domainName;
        this.platformUrl = platformUrl;
        this.schoolName = schoolName;
        this.platformId = platformId;
        this.onerosterClientId = onerosterClientId;
        this.onerosterClientSecret = onerosterClientSecret;
        this.users = users;
        this.classes = classes;
        this.enrollments = enrollments;
        this.academicSessions = academicSessions;
        this.orgs = orgs;
    }

    public String getSchoolCode() { return schoolCode; }
    public String getDomainName() { return domainName; }
    public String getPlatformUrl() { return platformUrl; }
    public String getSchoolName() { return schoolName; }
    public String getPlatformId() { return platformId; }
    public String getOnRosterClientId() { return onerosterClientId; }
    public String getOnRosterClientSecret() { return onerosterClientSecret; }
    public List<MockUser> getUsers() { return users; }
    public List<Map<String, Object>> getClasses() { return classes; }
    public List<Map<String, Object>> getEnrollments() { return enrollments; }
    public List<Map<String, Object>> getAcademicSessions() { return academicSessions; }
    public List<Map<String, Object>> getOrgs() { return orgs; }

    public MockUser getUserByUsername(String username) {
        return users.stream()
                .filter(u -> u.username.equals(username))
                .findFirst()
                .orElse(null);
    }

    public MockUser getUserByLegacyId(String legacyId) {
        return users.stream()
                .filter(u -> u.legacyIdentifier.equals(legacyId))
                .findFirst()
                .orElse(null);
    }

    public static class MockUser {
        public final String username;
        public final String password;
        public final String legacyIdentifier;
        public final String onerosterSourcedId;
        public final String givenName;
        public final String familyName;
        public final String email;
        public final String basisrol;
        public final String sex;
        public final String groupName;
        public final String groupDescription;
        public final String classCode;
        public final String classTitle;
        public final String classSourcedId;
        public final List<String> parentGroupNames;

        public MockUser(String username, String password, String legacyIdentifier, String onerosterSourcedId,
                        String givenName, String familyName, String email, String basisrol, String sex,
                        String groupName, String groupDescription, String classCode, String classTitle,
                        String classSourcedId, List<String> parentGroupNames) {
            this.username = username;
            this.password = password;
            this.legacyIdentifier = legacyIdentifier;
            this.onerosterSourcedId = onerosterSourcedId;
            this.givenName = givenName;
            this.familyName = familyName;
            this.email = email;
            this.basisrol = basisrol;
            this.sex = sex;
            this.groupName = groupName;
            this.groupDescription = groupDescription;
            this.classCode = classCode;
            this.classTitle = classTitle;
            this.classSourcedId = classSourcedId;
            this.parentGroupNames = parentGroupNames;
        }

        public String getOnRosterRole() {
            return switch (basisrol) {
                case "Leerling" -> "student";
                case "Leerkracht" -> "teacher";
                case "Beheerder" -> "teacher";
                default -> "student";
            };
        }
    }

    public static MockSchoolData createTestSchool1() {
        String platformId = "6001";
        String domainName = "goatheneum.smartschool.be";
        String platformUrl = "https://" + domainName;
        String schoolName = "GO! Atheneum Antwerpen";
        String schoolCode = "goatheneum";
        String classSourcedId = platformId + "_usermanagement_100";
        String termSourcedId = platformId + "_2025-09-01";
        String orgSourcedId = platformId;

        MockUser student1 = new MockUser(
                "sof3.jan.janssens", "janjanssens",
                "T1s1LegacyId0000000000000000000000==",
                platformId + "_8001_0",
                "Jan", "Janssens", "jan.janssens@student.goatheneum.be",
                "Leerling", "male",
                "3ITSOF1", "Bachelor IT Software - 1",
                "3ITSOF1", "3ITSOF1", classSourcedId,
                List.of("Leerlingen", "3de jaar")
        );

        MockUser student2 = new MockUser(
                "sof3.lisa.peeters", "lisapeeters",
                "T1s2LegacyId0000000000000000000000==",
                platformId + "_8002_0",
                "Lisa", "Peeters", "lisa.peeters@student.goatheneum.be",
                "Leerling", "female",
                "3ITSOF1", "Bachelor IT Software - 1",
                "3ITSOF1", "3ITSOF1", classSourcedId,
                List.of("Leerlingen", "3de jaar")
        );

        MockUser teacher1 = new MockUser(
                "it02_1.marc.devries", "marcdevries",
                "T1t1LegacyId0000000000000000000000==",
                platformId + "_8101_0",
                "Marc", "De Vries", "marc.devries@goatheneum.be",
                "Leerkracht", "male",
                "IT-Team", "IT Department",
                "IT-TEAM", "IT-TEAM", platformId + "_usermanagement_101",
                List.of("Leerkrachten", "Technisch")
        );

        MockUser teacher2 = new MockUser(
                "it02_2.sara.bakker", "sarabakker",
                "T1t2LegacyId0000000000000000000000==",
                platformId + "_8102_0",
                "Sara", "Bakker", "sara.bakker@goatheneum.be",
                "Leerkracht", "female",
                "IT-Team", "IT Department",
                "IT-TEAM", "IT-TEAM", platformId + "_usermanagement_101",
                List.of("Leerkrachten", "Technisch")
        );

        MockUser admin1 = new MockUser(
                "beheer.koen.admin", "koenadmin",
                "T1a1LegacyId0000000000000000000000==",
                platformId + "_8201_0",
                "Koen", "Admin", "koen.admin@goatheneum.be",
                "Beheerder", "male",
                "Beheer", "Administratie",
                "BEHEER", "BEHEER", platformId + "_usermanagement_102",
                List.of("Beheer")
        );

        List<MockUser> users = List.of(student1, student2, teacher1, teacher2, admin1);

        Map<String, Object> class1 = Map.of(
                "sourcedId", classSourcedId,
                "status", "active",
                "title", "3ITSOF1",
                "classCode", "3ITSOF1",
                "classType", "scheduled",
                "school", Map.of(
                        "sourcedId", orgSourcedId,
                        "status", "active",
                        "name", schoolName,
                        "type", "school",
                        "identifier", "platform_" + platformId
                ),
                "terms", List.of(Map.of(
                        "sourcedId", termSourcedId,
                        "status", "active",
                        "title", "2025-2026",
                        "startDate", "2025-09-01",
                        "endDate", "2026-08-31",
                        "type", "schoolYear"
                )),
                "metadata", Map.of(
                        "smsc.classType", "class",
                        "smsc.legacyIdentifier", "T1classLegacy00000000=="
                )
        );

        Map<String, Object> class2 = Map.of(
                "sourcedId", platformId + "_usermanagement_101",
                "status", "active",
                "title", "IT-TEAM",
                "classCode", "IT-TEAM",
                "classType", "scheduled",
                "school", Map.of(
                        "sourcedId", orgSourcedId,
                        "status", "active",
                        "name", schoolName,
                        "type", "school",
                        "identifier", "platform_" + platformId
                ),
                "terms", List.of(Map.of(
                        "sourcedId", termSourcedId,
                        "status", "active",
                        "title", "2025-2026",
                        "startDate", "2025-09-01",
                        "endDate", "2026-08-31",
                        "type", "schoolYear"
                )),
                "metadata", Map.of(
                        "smsc.classType", "class",
                        "smsc.legacyIdentifier", "T1classLegacy00000001=="
                )
        );

        List<Map<String, Object>> classes = List.of(class1, class2);

        List<Map<String, Object>> enrollments = List.of(
                buildEnrollment(student1, class1, platformId, orgSourcedId, schoolName, termSourcedId),
                buildEnrollment(student2, class1, platformId, orgSourcedId, schoolName, termSourcedId),
                buildEnrollment(teacher1, class2, platformId, orgSourcedId, schoolName, termSourcedId),
                buildEnrollment(teacher2, class2, platformId, orgSourcedId, schoolName, termSourcedId),
                buildEnrollment(admin1, Map.of(
                        "sourcedId", platformId + "_usermanagement_102",
                        "status", "active",
                        "title", "BEHEER",
                        "classCode", "BEHEER",
                        "classType", "scheduled",
                        "school", Map.of(
                                "sourcedId", orgSourcedId,
                                "status", "active",
                                "name", schoolName,
                                "type", "school",
                                "identifier", "platform_" + platformId
                        ),
                        "terms", List.of(Map.of(
                                "sourcedId", termSourcedId,
                                "status", "active",
                                "title", "2025-2026",
                                "startDate", "2025-09-01",
                                "endDate", "2026-08-31",
                                "type", "schoolYear"
                        )),
                        "metadata", Map.of(
                                "smsc.classType", "class",
                                "smsc.legacyIdentifier", "T1classLegacy00000002=="
                        )
                ), platformId, orgSourcedId, schoolName, termSourcedId)
        );

        List<Map<String, Object>> academicSessions = List.of(Map.of(
                "sourcedId", termSourcedId,
                "status", "active",
                "title", "2025-2026",
                "startDate", "2025-09-01",
                "endDate", "2026-08-31",
                "type", "schoolYear",
                "schoolYear", "2026"
        ));

        List<Map<String, Object>> orgs = List.of(Map.of(
                "sourcedId", orgSourcedId,
                "status", "active",
                "name", schoolName,
                "type", "school",
                "identifier", "platform_" + platformId
        ));

        return new MockSchoolData(
                schoolCode, domainName, platformUrl, schoolName, platformId,
                "ts1-client", "ts1-secret",
                users, classes, enrollments, academicSessions, orgs
        );
    }

    public static MockSchoolData createTestSchool2() {
        String platformId = "6002";
        String domainName = "kuleuven.smartschool.be";
        String platformUrl = "https://" + domainName;
        String schoolName = "Katholieke Universiteit Leuven";
        String schoolCode = "kuleuven";
        String classSourcedId = platformId + "_usermanagement_200";
        String termSourcedId = platformId + "_2025-09-01";
        String orgSourcedId = platformId;

        MockUser student1 = new MockUser(
                "lok2.emma.claes", "emmaclaes",
                "T2s1LegacyId0000000000000000000000==",
                platformId + "_9001_0",
                "Emma", "Claes", "emma.claes@student.kuleuven.be",
                "Leerling", "female",
                "2GRADLOK2", "2de Graad Lokale Dienstverlening - 2",
                "2GRADLOK2", "2GRADLOK2", classSourcedId,
                List.of("Leerlingen", "2de graad")
        );

        MockUser student2 = new MockUser(
                "lok2.thomas.willems", "thomaswillems",
                "T2s2LegacyId0000000000000000000000==",
                platformId + "_9002_0",
                "Thomas", "Willems", "thomas.willems@student.kuleuven.be",
                "Leerling", "male",
                "2GRADLOK2", "2de Graad Lokale Dienstverlening - 2",
                "2GRADLOK2", "2GRADLOK2", classSourcedId,
                List.of("Leerlingen", "2de graad")
        );

        MockUser teacher1 = new MockUser(
                "lok02_1.an.verhoeven", "anverhoeven",
                "T2t1LegacyId0000000000000000000000==",
                platformId + "_9101_0",
                "An", "Verhoeven", "an.verhoeven@kuleuven.be",
                "Leerkracht", "female",
                "LOK-Team", "Lokale Dienstverlening Department",
                "LOK-TEAM", "LOK-TEAM", platformId + "_usermanagement_201",
                List.of("Leerkrachten", "Zorg")
        );

        MockUser teacher2 = new MockUser(
                "lok02_2.pieter.maes", "pietermaes",
                "T2t2LegacyId0000000000000000000000==",
                platformId + "_9102_0",
                "Pieter", "Maes", "pieter.maes@kuleuven.be",
                "Leerkracht", "male",
                "LOK-Team", "Lokale Dienstverlening Department",
                "LOK-TEAM", "LOK-TEAM", platformId + "_usermanagement_201",
                List.of("Leerkrachten", "Zorg")
        );

        MockUser admin1 = new MockUser(
                "directie.inge.directeur", "ingedirecteur",
                "T2a1LegacyId0000000000000000000000==",
                platformId + "_9201_0",
                "Inge", "Directeur", "inge.directeur@kuleuven.be",
                "Beheerder", "female",
                "Directie", "School Management",
                "DIRECTIE", "DIRECTIE", platformId + "_usermanagement_202",
                List.of("Beheer", "Directie")
        );

        List<MockUser> users = List.of(student1, student2, teacher1, teacher2, admin1);

        Map<String, Object> class1 = Map.of(
                "sourcedId", classSourcedId,
                "status", "active",
                "title", "2GRADLOK2",
                "classCode", "2GRADLOK2",
                "classType", "scheduled",
                "school", Map.of(
                        "sourcedId", orgSourcedId,
                        "status", "active",
                        "name", schoolName,
                        "type", "school",
                        "identifier", "platform_" + platformId
                ),
                "terms", List.of(Map.of(
                        "sourcedId", termSourcedId,
                        "status", "active",
                        "title", "2025-2026",
                        "startDate", "2025-09-01",
                        "endDate", "2026-08-31",
                        "type", "schoolYear"
                )),
                "metadata", Map.of(
                        "smsc.classType", "class",
                        "smsc.legacyIdentifier", "T2classLegacy00000000=="
                )
        );

        Map<String, Object> class2 = Map.of(
                "sourcedId", platformId + "_usermanagement_201",
                "status", "active",
                "title", "LOK-TEAM",
                "classCode", "LOK-TEAM",
                "classType", "scheduled",
                "school", Map.of(
                        "sourcedId", orgSourcedId,
                        "status", "active",
                        "name", schoolName,
                        "type", "school",
                        "identifier", "platform_" + platformId
                ),
                "terms", List.of(Map.of(
                        "sourcedId", termSourcedId,
                        "status", "active",
                        "title", "2025-2026",
                        "startDate", "2025-09-01",
                        "endDate", "2026-08-31",
                        "type", "schoolYear"
                )),
                "metadata", Map.of(
                        "smsc.classType", "class",
                        "smsc.legacyIdentifier", "T2classLegacy00000001=="
                )
        );

        List<Map<String, Object>> classes = List.of(class1, class2);

        List<Map<String, Object>> enrollments = List.of(
                buildEnrollment(student1, class1, platformId, orgSourcedId, schoolName, termSourcedId),
                buildEnrollment(student2, class1, platformId, orgSourcedId, schoolName, termSourcedId),
                buildEnrollment(teacher1, class2, platformId, orgSourcedId, schoolName, termSourcedId),
                buildEnrollment(teacher2, class2, platformId, orgSourcedId, schoolName, termSourcedId),
                buildEnrollment(admin1, Map.of(
                        "sourcedId", platformId + "_usermanagement_202",
                        "status", "active",
                        "title", "DIRECTIE",
                        "classCode", "DIRECTIE",
                        "classType", "scheduled",
                        "school", Map.of(
                                "sourcedId", orgSourcedId,
                                "status", "active",
                                "name", schoolName,
                                "type", "school",
                                "identifier", "platform_" + platformId
                        ),
                        "terms", List.of(Map.of(
                                "sourcedId", termSourcedId,
                                "status", "active",
                                "title", "2025-2026",
                                "startDate", "2025-09-01",
                                "endDate", "2026-08-31",
                                "type", "schoolYear"
                        )),
                        "metadata", Map.of(
                                "smsc.classType", "class",
                                "smsc.legacyIdentifier", "T2classLegacy00000002=="
                        )
                ), platformId, orgSourcedId, schoolName, termSourcedId)
        );

        List<Map<String, Object>> academicSessions = List.of(Map.of(
                "sourcedId", termSourcedId,
                "status", "active",
                "title", "2025-2026",
                "startDate", "2025-09-01",
                "endDate", "2026-08-31",
                "type", "schoolYear",
                "schoolYear", "2026"
        ));

        List<Map<String, Object>> orgs = List.of(Map.of(
                "sourcedId", orgSourcedId,
                "status", "active",
                "name", schoolName,
                "type", "school",
                "identifier", "platform_" + platformId
        ));

        return new MockSchoolData(
                schoolCode, domainName, platformUrl, schoolName, platformId,
                "ts2-client", "ts2-secret",
                users, classes, enrollments, academicSessions, orgs
        );
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> buildEnrollment(
            MockUser user, Map<String, Object> classData,
            String platformId, String orgSourcedId, String schoolName, String termSourcedId) {

        Map<String, Object> userWithType = new java.util.HashMap<>((Map<String, Object>) buildOnRosterUser(user, platformId, orgSourcedId, schoolName));
        userWithType.put("type", "user");

        Map<String, Object> classWithType = new java.util.HashMap<>(classData);
        classWithType.put("type", "class");

        return Map.of(
                "sourcedId", user.onerosterSourcedId + "." + classData.get("sourcedId"),
                "status", "active",
                "role", user.getOnRosterRole(),
                "primary", true,
                "user", userWithType,
                "class", classWithType,
                "school", Map.of(
                        "sourcedId", orgSourcedId,
                        "status", "active",
                        "name", schoolName,
                        "type", "school",
                        "identifier", "platform_" + platformId
                )
        );
    }

    private static Map<String, Object> buildOnRosterUser(MockUser user, String platformId, String orgSourcedId, String schoolName) {
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

    public Map<String, Object> buildFullUserInfo(MockUser user) {
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("userID", user.legacyIdentifier);
        result.put("name", user.givenName);
        result.put("surname", user.familyName);
        result.put("fullname", user.familyName + " " + user.givenName);
        result.put("username", user.username);
        result.put("extranames", "");
        result.put("initials", "");
        result.put("nickname", "");
        result.put("sex", "male".equals(user.sex) ? "Mannelijk" : "Vrouwelijk");
        result.put("birthday", "0000-00-00");
        result.put("birthplace", "");
        result.put("birthcountry", "");
        result.put("street", "");
        result.put("streetnr", "");
        result.put("busnr", "");
        result.put("postalCode", "");
        result.put("location", "");
        result.put("country", "");
        result.put("email", user.email);
        result.put("website", "");
        result.put("mobilePhone", "");
        result.put("homePhone", "");
        result.put("fax", "");
        result.put("im", "");
        result.put("stamboeknummer", 0);
        result.put("koppelingsveldschoolagenda", "");
        result.put("internalnumber", null);
        result.put("basisrol", user.basisrol);
        result.put("status", "actief");
        result.put("sorteerveld", (user.familyName + "-" + user.givenName).toLowerCase());
        result.put("platform", platformUrl);
        result.put("mainAccountUserID", user.legacyIdentifier);
        result.put("mainAccountReferenceID", user.onerosterSourcedId);
        result.put("isMainAccount", 1);
        result.put("isCoAccount", 0);
        result.put("nrCoAccount", 0);
        result.put("typeCoAccount", "");
        result.put("actualUserName", user.givenName);
        result.put("actualUserSurname", user.familyName);
        return result;
    }

    public Map<String, Object> buildUserInfo(MockUser user) {
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("userID", user.legacyIdentifier);
        result.put("name", user.givenName);
        result.put("surname", user.familyName);
        result.put("fullname", user.familyName + " " + user.givenName);
        result.put("username", user.username);
        result.put("platform", platformUrl);
        result.put("isMainAccount", 1);
        result.put("isCoAccount", 0);
        result.put("nrCoAccount", 0);
        result.put("typeCoAccount", "");
        result.put("actualUserName", user.givenName);
        result.put("actualUserSurname", user.familyName);
        return result;
    }

    public Map<String, Object> buildGroupInfo(MockUser user) {
        List<Map<String, Object>> groups = List.of(Map.of(
                "groupID", user.legacyIdentifier.substring(0, Math.min(16, user.legacyIdentifier.length())) + "=",
                "name", user.groupName,
                "description", user.groupDescription,
                "code", "",
                "platform", platformUrl,
                "oneRosterSourcedId", user.classSourcedId
        ));

        List<Map<String, Object>> parentGroups = user.parentGroupNames.stream()
                .map(name -> Map.<String, Object>of(
                        "groupID", "parent" + name.hashCode() + "=",
                        "name", name,
                        "description", name,
                        "code", "",
                        "platform", platformUrl,
                        "oneRosterSourcedId", user.classSourcedId
                ))
                .toList();

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("groups", groups);
        result.put("parentGroups", parentGroups);
        return result;
    }

    public String generateMockAccessToken(String userId) {
        return "mock-oauth-token-" + schoolCode + "-" + userId + "-" + System.currentTimeMillis();
    }

}
