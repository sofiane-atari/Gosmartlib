package edu.ap.bib_backend.service.mock;

import edu.ap.bib_backend.service.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ims/oneroster")
public class MockOneRosterController {

    private static final Logger log = LoggerFactory.getLogger(MockOneRosterController.class);

    private final MockSchoolRegistry registry;
    private final JwtService jwtService;

    public MockOneRosterController(MockSchoolRegistry registry, JwtService jwtService) {
        this.registry = registry;
        this.jwtService = jwtService;
    }

    @PostMapping("/token")
    public ResponseEntity<Map<String, Object>> token(
            @RequestParam String grant_type,
            @RequestParam(required = false) String client_id,
            @RequestParam(required = false) String client_secret,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        String resolvedClientId = client_id;
        String resolvedClientSecret = client_secret;

        if (authorization != null && authorization.startsWith("Basic ")) {
            String decoded = new String(java.util.Base64.getDecoder().decode(authorization.substring(6)));
            String[] parts = decoded.split(":", 2);
            if (parts.length == 2) {
                resolvedClientId = parts[0];
                resolvedClientSecret = parts[1];
            }
        }

        MockSchoolData school = registry.getSchoolByOnRosterClientId(resolvedClientId);
        if (school == null || !school.getOnRosterClientSecret().equals(resolvedClientSecret)) {
            return ResponseEntity.status(401).body(Map.of("error", "invalid_client"));
        }

        Map<String, Object> claims = Map.of(
                "schoolCode", school.getSchoolCode(),
                "domainName", school.getDomainName(),
                "type", "oneroster"
        );

        String token = jwtService.generateToken(resolvedClientId, "ONEROSTER_CLIENT", claims);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("access_token", token);
        body.put("token_type", "Bearer");
        body.put("expires_in", 3600);

        log.info("Mock OneRoster token issued for school: {}", school.getSchoolName());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/v1p1/users")
    public ResponseEntity<Map<String, Object>> users(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        MockSchoolData school = resolveSchool(authorization);
        if (school == null) return unauthorized();

        List<Map<String, Object>> userList = school.getUsers().stream()
                .map(u -> buildUserRecord(u, school))
                .toList();

        return withCount("users", userList, userList.size());
    }

    @GetMapping("/v1p1/students")
    public ResponseEntity<Map<String, Object>> students(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        MockSchoolData school = resolveSchool(authorization);
        if (school == null) return unauthorized();

        List<Map<String, Object>> students = school.getUsers().stream()
                .filter(u -> "Leerling".equals(u.basisrol))
                .map(u -> buildUserRecord(u, school))
                .toList();

        return withCount("users", students, students.size());
    }

    @GetMapping("/v1p1/teachers")
    public ResponseEntity<Map<String, Object>> teachers(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        MockSchoolData school = resolveSchool(authorization);
        if (school == null) return unauthorized();

        List<Map<String, Object>> teachers = school.getUsers().stream()
                .filter(u -> "Leerkracht".equals(u.basisrol) || "Beheerder".equals(u.basisrol))
                .map(u -> buildUserRecord(u, school))
                .toList();

        return withCount("users", teachers, teachers.size());
    }

    @GetMapping("/v1p1/classes")
    public ResponseEntity<Map<String, Object>> classes(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        MockSchoolData school = resolveSchool(authorization);
        if (school == null) return unauthorized();

        return withCount("classes", school.getClasses(), school.getClasses().size());
    }

    @GetMapping("/v1p1/enrollments")
    public ResponseEntity<Map<String, Object>> enrollments(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        MockSchoolData school = resolveSchool(authorization);
        if (school == null) return unauthorized();

        return withCount("enrollments", school.getEnrollments(), school.getEnrollments().size());
    }

    @GetMapping("/v1p1/classes/{classId}/students")
    public ResponseEntity<Map<String, Object>> classStudents(
            @PathVariable String classId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        MockSchoolData school = resolveSchool(authorization);
        if (school == null) return unauthorized();

        List<Map<String, Object>> students = school.getUsers().stream()
                .filter(u -> u.classSourcedId.equals(classId) && "Leerling".equals(u.basisrol))
                .map(u -> buildUserRecord(u, school))
                .toList();

        return withCount("users", students, students.size());
    }

    @GetMapping("/v1p1/academicSessions")
    public ResponseEntity<Map<String, Object>> academicSessions(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        MockSchoolData school = resolveSchool(authorization);
        if (school == null) return unauthorized();

        return withCount("academicSessions", school.getAcademicSessions(), school.getAcademicSessions().size());
    }

    @GetMapping("/v1p1/schools")
    public ResponseEntity<Map<String, Object>> schools(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        MockSchoolData school = resolveSchool(authorization);
        if (school == null) return unauthorized();

        return withCount("orgs", school.getOrgs(), school.getOrgs().size());
    }

    @GetMapping("/v1p1/orgs")
    public ResponseEntity<Map<String, Object>> orgs(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        MockSchoolData school = resolveSchool(authorization);
        if (school == null) return unauthorized();

        return withCount("orgs", school.getOrgs(), school.getOrgs().size());
    }

    @GetMapping("/v1p1/courses")
    public ResponseEntity<Map<String, Object>> courses() {
        return ResponseEntity.ok(Map.of("courses", List.of()));
    }

    private MockSchoolData resolveSchool(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        String token = authorization.substring(7);
        if (!jwtService.isValid(token)) {
            return null;
        }
        String schoolCode = (String) jwtService.extractClaim(token, "schoolCode");
        return registry.getSchoolByCode(schoolCode);
    }

    private ResponseEntity<Map<String, Object>> unauthorized() {
        return ResponseEntity.status(401).body(Map.of("error", "invalid_token"));
    }

    private ResponseEntity<Map<String, Object>> withCount(String key, List<?> items, int count) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Total-Count", String.valueOf(count));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put(key, items);

        return ResponseEntity.ok().headers(headers).body(body);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildUserRecord(MockSchoolData.MockUser user, MockSchoolData school) {
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
        result.put("orgs", school.getOrgs());
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