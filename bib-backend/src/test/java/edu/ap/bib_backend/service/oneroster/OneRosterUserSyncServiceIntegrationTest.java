package edu.ap.bib_backend.service.oneroster;

import edu.ap.bib_backend.jpa.entity.Campus;
import edu.ap.bib_backend.jpa.entity.OneRosterCredential;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.repository.CampusRepository;
import edu.ap.bib_backend.jpa.repository.OneRosterCredentialRepository;
import edu.ap.bib_backend.jpa.repository.SchoolRepository;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OneRosterUserSyncServiceIntegrationTest {

    @Autowired
    private OneRosterUserSyncService syncService;

    @Autowired
    private OneRosterCredentialRepository credentialRepository;

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CampusRepository campusRepository;

    @MockitoBean
    private OneRosterClient oneRosterClient;

    @Test
    void Given_SchoolWithCredentials_When_SyncAllSchools_Then_UsersAreSynchronized() {
        // Arrange
        School school = new School();
        school.setDomainName("int-sync.smartschool.be");
        school.setName("Sync School");
        school = schoolRepository.save(school);

        Campus campus = new Campus();
        campus.setName("Default Campus");
        campus.setSchool(school);
        campusRepository.save(campus);

        OneRosterCredential cred = new OneRosterCredential();
        cred.setSchool(school);
        cred.setClientId("id");
        cred.setClientSecret("secret");
        credentialRepository.save(cred);

        Map<String, Object> rosterUser = Map.of(
            "metadata", Map.of("smsc.legacyIdentifier", "sync-user-1"),
            "role", "student"
        );
        when(oneRosterClient.fetchUsers("int-sync.smartschool.be")).thenReturn(List.of(rosterUser));

        // Act
        syncService.syncAllSchools();

        // Assert
        assertTrue(userRepository.existsById("sync-user-1"));
    }
}
