package edu.ap.bib_backend.controller;

import edu.ap.bib_backend.dto.oneroster.OneRosterCredentialValidationDTO;
import edu.ap.bib_backend.service.oneroster.OneRosterClient;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/schools")
public class OneRosterCredentialController {

    private final OneRosterClient oneRosterClient;

    public OneRosterCredentialController(OneRosterClient oneRosterClient) {
        this.oneRosterClient = oneRosterClient;
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
}
