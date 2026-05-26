package edu.ap.bib_backend.controller;

import edu.ap.bib_backend.dto.oneroster.OneRosterCredentialValidationDTO;
import edu.ap.bib_backend.jpa.entity.OneRosterCredential;
import edu.ap.bib_backend.jpa.repository.OneRosterCredentialRepository;
import edu.ap.bib_backend.service.oneroster.OneRosterClient;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/schools")
public class OneRosterCredentialController {

    private final OneRosterClient oneRosterClient;
    private final OneRosterCredentialRepository credentialRepository;

    public OneRosterCredentialController(
            OneRosterClient oneRosterClient,
            OneRosterCredentialRepository credentialRepository) {
        this.oneRosterClient = oneRosterClient;
        this.credentialRepository = credentialRepository;
    }

    @PostMapping("/validate-credentials")
    public ResponseEntity<Void> validateCredentials(@Valid @RequestBody OneRosterCredentialValidationDTO dto) {
        oneRosterClient.validateCredentials(
                dto.clientId(),
                dto.clientSecret(),
                dto.domainName()
        );

        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/validate-domain")
    public ResponseEntity<Void> validateDomainChange(
            @PathVariable Long id,
            @RequestBody Map<String, @NotBlank String> body) {
        String domainName = body.get("domainName");
        if (domainName == null || domainName.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        OneRosterCredential credential = credentialRepository.findBySchoolId(id)
                .orElseThrow(() -> new IllegalArgumentException("Geen OneRoster-credentials gevonden voor deze school."));

        oneRosterClient.validateCredentials(
                credential.getClientId(),
                credential.getClientSecret(),
                domainName
        );

        return ResponseEntity.ok().build();
    }
}
