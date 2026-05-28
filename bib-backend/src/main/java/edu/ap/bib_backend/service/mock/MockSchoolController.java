package edu.ap.bib_backend.service.mock;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/mock/schools")
public class MockSchoolController {

    private final MockSchoolRegistry registry;

    public MockSchoolController(MockSchoolRegistry registry) {
        this.registry = registry;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listSchools() {
        List<Map<String, Object>> schools = registry.getAllSchools().stream()
                .map(s -> Map.<String, Object>of(
                        "code", s.getSchoolCode(),
                        "domain", s.getDomainName(),
                        "name", s.getSchoolName(),
                        "platformId", s.getPlatformId(),
                        "userCount", s.getUsers().size(),
                        "onRosterClientId", s.getOnRosterClientId()
                ))
                .toList();
        return ResponseEntity.ok(schools);
    }

    @GetMapping("/active")
    public ResponseEntity<Map<String, Object>> getActiveSchool() {
        MockSchoolData school = registry.getActiveSchool();
        Map<String, Object> response = Map.of(
                "code", school.getSchoolCode(),
                "domain", school.getDomainName(),
                "name", school.getSchoolName(),
                "users", school.getUsers().stream()
                        .map(u -> Map.of(
                                "username", u.username,
                                "password", u.password,
                                "role", u.basisrol
                        ))
                        .toList()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{schoolCode}")
    public ResponseEntity<Map<String, Object>> switchSchool(@PathVariable String schoolCode) {
        MockSchoolData school = registry.getAllSchools().stream()
                .filter(s -> s.getSchoolCode().equals(schoolCode))
                .findFirst()
                .orElse(null);

        if (school == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "School not found: " + schoolCode,
                    "available", registry.getAllSchools().stream()
                            .map(MockSchoolData::getSchoolCode)
                            .toList()
            ));
        }

        registry.setActiveSchool(schoolCode);

        return ResponseEntity.ok(Map.of(
                "message", "Switched to " + school.getSchoolName(),
                "code", school.getSchoolCode(),
                "domain", school.getDomainName()
        ));
    }
}
