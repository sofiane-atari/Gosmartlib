package edu.ap.bib_backend.controller;

import edu.ap.bib_backend.dto.oneroster.OneRosterCredentialValidationDTO;
import edu.ap.bib_backend.jpa.entity.OneRosterCredential;
import edu.ap.bib_backend.jpa.repository.OneRosterCredentialRepository;
import edu.ap.bib_backend.service.oneroster.OneRosterClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OneRosterCredentialControllerTest {

    @Mock
    private OneRosterClient oneRosterClient;

    @Mock
    private OneRosterCredentialRepository credentialRepository;

    @InjectMocks
    private OneRosterCredentialController controller;

    @Test
    void Given_ValidCredentials_When_ValidateCredentials_Then_ReturnsOk() {
        OneRosterCredentialValidationDTO dto = new OneRosterCredentialValidationDTO(
                "school.be", "client-id", "client-secret");

        ResponseEntity<Void> response = controller.validateCredentials(dto);

        assertEquals(200, response.getStatusCode().value());
        verify(oneRosterClient).validateCredentials("client-id", "client-secret", "school.be");
    }

    @Test
    void Given_InvalidCredentials_When_ValidateCredentials_Then_PropagatesException() {
        OneRosterCredentialValidationDTO dto = new OneRosterCredentialValidationDTO(
                "school.be", "bad-client", "bad-secret");
        doThrow(new IllegalArgumentException("Invalid credentials"))
                .when(oneRosterClient).validateCredentials("bad-client", "bad-secret", "school.be");

        assertThrows(IllegalArgumentException.class, () -> controller.validateCredentials(dto));
    }

    @Test
    void Given_ValidDomainChange_When_ValidateDomainChange_Then_ReturnsOk() {
        Long schoolId = 1L;
        Map<String, String> body = Map.of("domainName", "newdomain.be");
        OneRosterCredential credential = new OneRosterCredential();
        credential.setClientId("client-id");
        credential.setClientSecret("client-secret");

        when(credentialRepository.findBySchoolId(schoolId)).thenReturn(Optional.of(credential));

        ResponseEntity<Void> response = controller.validateDomainChange(schoolId, body);

        assertEquals(200, response.getStatusCode().value());
        verify(oneRosterClient).validateCredentials("client-id", "client-secret", "newdomain.be");
    }

    @Test
    void Given_BlankDomainName_When_ValidateDomainChange_Then_ReturnsBadRequest() {
        Map<String, String> body = Map.of("domainName", "   ");

        ResponseEntity<Void> response = controller.validateDomainChange(1L, body);

        assertEquals(400, response.getStatusCode().value());
        verifyNoInteractions(oneRosterClient);
    }

    @Test
    void Given_MissingDomainName_When_ValidateDomainChange_Then_ReturnsBadRequest() {
        Map<String, String> body = Map.of();

        ResponseEntity<Void> response = controller.validateDomainChange(1L, body);

        assertEquals(400, response.getStatusCode().value());
        verifyNoInteractions(oneRosterClient);
    }

    @Test
    void Given_NoCredentialsForSchool_When_ValidateDomainChange_Then_ThrowsException() {
        Long schoolId = 1L;
        Map<String, String> body = Map.of("domainName", "newdomain.be");

        when(credentialRepository.findBySchoolId(schoolId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> controller.validateDomainChange(schoolId, body));
    }
}
