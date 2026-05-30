package edu.ap.bib_backend.service.oneroster;

import edu.ap.bib_backend.jpa.entity.OneRosterCredential;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.repository.OneRosterCredentialRepository;
import edu.ap.bib_backend.jpa.repository.SchoolRepository;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import edu.ap.bib_backend.service.GroupService;
import edu.ap.bib_backend.service.RoleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OneRosterUserSyncServiceTest {

    @Mock
    private OneRosterClient oneRosterClient;
    @Mock
    private OneRosterCredentialRepository credentialRepository;
    @Mock
    private SchoolRepository schoolRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private GroupService groupService;
    @Mock
    private RoleService roleService;

    @InjectMocks
    private OneRosterUserSyncService syncService;

    @Test
    void Given_Credentials_When_SyncAllSchools_Then_FetchesUsersAndUpserts() {
        School school = new School();
        school.setId(1L);
        school.setDomainName("test.smartschool.be");

        OneRosterCredential credential = new OneRosterCredential();
        credential.setSchool(school);

        when(credentialRepository.findAll()).thenReturn(List.of(credential));
        when(schoolRepository.findById(1L)).thenReturn(Optional.of(school));

        Map<String, Object> rosterUser = Map.of(
                "metadata", Map.of("smsc.legacyIdentifier", "user1"),
                "role", "teacher",
                "sourcedId", "sourced-1"
        );
        when(oneRosterClient.fetchUsers("test.smartschool.be")).thenReturn(List.of(rosterUser));
        when(userRepository.findById("user1")).thenReturn(Optional.empty());
        when(roleService.getRoleEntity("teacher")).thenReturn(null);

        syncService.syncAllSchools();

        verify(oneRosterClient).fetchUsers("test.smartschool.be");
        verify(userRepository).findById("user1");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void Given_MissingLegacyId_When_SyncSchoolUsers_Then_SkipsUser() {
        School school = new School();
        school.setDomainName("test.smartschool.be");

        Map<String, Object> rosterUser = Map.of(
                "metadata", Map.of("wrongKey", "user1")
        );
        when(oneRosterClient.fetchUsers("test.smartschool.be")).thenReturn(List.of(rosterUser));

        OneRosterCredential cred = new OneRosterCredential();
        cred.setSchool(school);
        when(credentialRepository.findAll()).thenReturn(List.of(cred));
        when(schoolRepository.findByDomainName("test.smartschool.be")).thenReturn(Optional.of(school));

        syncService.syncAllSchools();

        verify(userRepository, never()).save(any());
    }
}
