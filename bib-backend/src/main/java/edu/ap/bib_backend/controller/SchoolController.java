package edu.ap.bib_backend.controller;

import edu.ap.bib_backend.dto.CatalogSidebarSettingsDTO;
import edu.ap.bib_backend.dto.SchoolDTO;
import edu.ap.bib_backend.dto.oneroster.OneRosterClassDTO;
import edu.ap.bib_backend.jpa.repository.SchoolRepository;
import edu.ap.bib_backend.service.SchoolService;
import edu.ap.bib_backend.service.oneroster.OneRosterClient;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/schools")
@Slf4j
public class SchoolController {

    private final SchoolService schoolService;
    private final SchoolRepository schoolRepository;
    private final OneRosterClient oneRosterClient;
    private final boolean devModeEnabled;

    public SchoolController(
            SchoolService schoolService,
            SchoolRepository schoolRepository,
            OneRosterClient oneRosterClient,
            @Value("${app.dev-mode-enabled:false}") boolean devModeEnabled) {
        this.schoolService = schoolService;
        this.schoolRepository = schoolRepository;
        this.oneRosterClient = oneRosterClient;
        this.devModeEnabled = devModeEnabled;
    }

    @GetMapping
    public ResponseEntity<List<SchoolDTO>> getAll() {
        return ResponseEntity.ok(schoolService.getAll());
    }

    @PostMapping
    public ResponseEntity<SchoolDTO> create(@Valid @RequestBody SchoolDTO dto) {
        SchoolDTO createdSchool = schoolService.create(dto);

        return ResponseEntity
                .created(URI.create("/schools/" + createdSchool.id()))
                .body(createdSchool);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SchoolDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody SchoolDTO dto) {
        SchoolDTO updatedSchool = schoolService.update(id, dto);

        return ResponseEntity.ok(updatedSchool);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        schoolService.delete(id);

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/disable")
    public ResponseEntity<SchoolDTO> disable(@PathVariable Long id) {
        SchoolDTO school = schoolService.disable(id);

        return ResponseEntity.ok(school);
    }

    @PutMapping("/{id}/enable")
    public ResponseEntity<SchoolDTO> enable(@PathVariable Long id) {
        SchoolDTO school = schoolService.enable(id);

        return ResponseEntity.ok(school);
    }

    @DeleteMapping("/{id}/purge")
    public ResponseEntity<Void> purge(@PathVariable Long id) {
        if (!devModeEnabled) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Deze actie is alleen beschikbaar in ontwikkelingsmodus.");
        }

        schoolService.purge(id);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/books/count")
    public ResponseEntity<Long> getBookCountBySchool(@PathVariable Long id) {
        return ResponseEntity.ok(schoolService.getBookCountBySchool(id));
    }

    @GetMapping("/books/count")
    public ResponseEntity<Long> getTotalBookCount() {
        return ResponseEntity.ok(schoolService.getTotalBookCount());
    }

    @GetMapping("/dev-mode-enabled")
    public ResponseEntity<DevModeResponse> isDevModeEnabled() {
        return ResponseEntity.ok(new DevModeResponse(devModeEnabled));
    }

    @GetMapping("/check-domain")
    public ResponseEntity<Void> checkDomain(@RequestParam String domainName) {
        schoolService.checkDomain(domainName);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me/catalog-sidebar-settings")
    public ResponseEntity<CatalogSidebarSettingsDTO> getOwnCatalogSidebarSettings() {
        Long schoolId = resolveOwnSchoolId();
        return ResponseEntity.ok(schoolService.getCatalogSidebarSettings(schoolId));
    }

    @PutMapping("/me/catalog-sidebar-settings")
    public ResponseEntity<CatalogSidebarSettingsDTO> updateOwnCatalogSidebarSettings(
            @Valid @RequestBody CatalogSidebarSettingsDTO dto) {
        Long schoolId = resolveOwnSchoolId();
        return ResponseEntity.ok(schoolService.updateCatalogSidebarSettings(schoolId, dto));
    }

    @GetMapping("/{id}/catalog-sidebar-settings")
    public ResponseEntity<CatalogSidebarSettingsDTO> getCatalogSidebarSettings(@PathVariable Long id) {
        return ResponseEntity.ok(schoolService.getCatalogSidebarSettings(id));
    }

    @PutMapping("/{id}/catalog-sidebar-settings")
    public ResponseEntity<CatalogSidebarSettingsDTO> updateCatalogSidebarSettings(
            @PathVariable Long id,
            @Valid @RequestBody CatalogSidebarSettingsDTO dto) {
        return ResponseEntity.ok(schoolService.updateCatalogSidebarSettings(id, dto));
    }

    @GetMapping("/me/classes")
    public ResponseEntity<List<OneRosterClassDTO>> getOwnSchoolClasses() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> details = (Map<String, Object>) auth.getDetails();
        String schoolCode = (String) details.get("schoolCode");

        if (schoolCode == null || schoolCode.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Geen school gekoppeld aan deze gebruiker.");
        }

        log.info("Fetching own school classes for raw schoolCode: {}", schoolCode);
        List<OneRosterClassDTO> classes = oneRosterClient.fetchClasses(schoolCode);
        log.info("Received {} classes for own school", classes.size());

        return ResponseEntity.ok(classes);
    }

    @GetMapping("/{id}/classes")
    public ResponseEntity<List<OneRosterClassDTO>> getClassesBySchool(@PathVariable Long id) {
        String schoolCode = schoolRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "School niet gevonden."))
                .getDomainName();

        List<OneRosterClassDTO> classes = oneRosterClient.fetchClasses(schoolCode);

        return ResponseEntity.ok(classes);
    }

    public record DevModeResponse(boolean enabled) {
    }

    private Long resolveOwnSchoolId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> details = (Map<String, Object>) auth.getDetails();
        String rawCode = (String) details.get("schoolCode");

        if (rawCode == null || rawCode.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Geen school gekoppeld aan deze gebruiker.");
        }

        String domainName = stripProtocol(rawCode);

        return schoolRepository.findByDomainName(domainName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "School niet gevonden."))
                .getId();
    }

    private static String stripProtocol(String schoolCode) {
        String trimmed = schoolCode.trim().toLowerCase();
        String withoutScheme = trimmed.replaceFirst("^https?://", "");
        int slashIndex = withoutScheme.indexOf('/');
        if (slashIndex > -1) {
            return withoutScheme.substring(0, slashIndex);
        }
        return withoutScheme;
    }
}
