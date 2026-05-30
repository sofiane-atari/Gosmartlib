package edu.ap.bib_backend.controller;

import edu.ap.bib_backend.dto.SchoolDTO;
import edu.ap.bib_backend.dto.SchoolSummaryDto;
import edu.ap.bib_backend.service.SchoolService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
public class SchoolController {

    private final SchoolService schoolService;
    private final boolean devModeEnabled;

    public SchoolController(
            SchoolService schoolService,
            @Value("${app.dev-mode-enabled:false}") boolean devModeEnabled) {
        this.schoolService = schoolService;
        this.devModeEnabled = devModeEnabled;
    }

    @GetMapping("/accessible")
    public ResponseEntity<List<SchoolSummaryDto>> getAccessibleSchools(Authentication authentication) {
        return ResponseEntity.ok(schoolService.getAccessibleSchools(authentication));
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

    @PutMapping("/{id}/catalog-visibility")
    public ResponseEntity<Void> setCatalogVisibility(
            @PathVariable Long id,
            @RequestBody CatalogVisibilityRequest request,
            Authentication authentication) {
        schoolService.setCatalogVisibility(id, request.visible(), authentication);
        return ResponseEntity.ok().build();
    }

    public record CatalogVisibilityRequest(boolean visible) {
    }

    public record DevModeResponse(boolean enabled) {
    }
}
