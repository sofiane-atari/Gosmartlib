package edu.ap.bib_backend.service;

import edu.ap.bib_backend.dto.CampusDTO;
import edu.ap.bib_backend.dto.SchoolDTO;
import edu.ap.bib_backend.dto.SchoolSummaryDto;
import edu.ap.bib_backend.dto.oneroster.OneRosterCredentialDTO;
import edu.ap.bib_backend.jpa.entity.OneRosterCredential;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.enums.SchoolStatus;
import edu.ap.bib_backend.jpa.repository.BookCopyRepository;
import edu.ap.bib_backend.jpa.repository.OneRosterCredentialRepository;
import edu.ap.bib_backend.jpa.repository.SchoolRepository;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import edu.ap.bib_backend.service.oneroster.OneRosterClient;
import edu.ap.bib_backend.service.oneroster.OneRosterUserSyncService;
import edu.ap.bib_backend.service.user.CurrentUserService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolServiceTest {

    @Mock
    private SchoolRepository schoolRepository;

    @Mock
    private BookCopyRepository bookCopyRepository;

    @Mock
    private OneRosterCredentialRepository credentialRepository;

    @Mock
    private OneRosterClient oneRosterClient;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OneRosterUserSyncService oneRosterUserSyncService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private EntityManager entityManager;

    @Mock
    private Query query;

    private SchoolService schoolService;

    @BeforeEach
    void setUp() throws Exception {
        schoolService = new SchoolService(
                schoolRepository,
                bookCopyRepository,
                credentialRepository,
                userRepository,
                oneRosterClient,
                oneRosterUserSyncService,
                currentUserService,
                30L
        );
        Field entityManagerField = SchoolService.class.getDeclaredField("entityManager");
        entityManagerField.setAccessible(true);
        entityManagerField.set(schoolService, entityManager);
    }

    @Test
    void givenValidSchoolWithCredentials_whenCreate_thenSaveAndReturnDto() {
        SchoolDTO input = schoolDtoWithCredentials(
                null,
                "  MySchool.be  ",
                "  AP Hogeschool  ",
                "test-client-id",
                "test-client-secret");

        when(schoolRepository.save(any(School.class))).thenAnswer(invocation -> {
            School school = invocation.getArgument(0);
            school.setId(1L);
            return school;
        });

        SchoolDTO output = schoolService.create(input);

        assertEquals(1L, output.id());
        assertEquals("myschool.be", output.domainName());
        assertEquals("AP Hogeschool", output.name());

        verify(oneRosterClient).validateCredentials(eq("test-client-id"), eq("test-client-secret"),
                eq("  MySchool.be  "));
        verify(schoolRepository).save(any(School.class));
        verify(credentialRepository).save(any(OneRosterCredential.class));
    }

    @Test
    void givenSchoolsExist_whenGetAll_thenReturnDtos() {
        School school = schoolEntity(
                1L,
                "ap.be",
                "AP");

        when(schoolRepository.findAll()).thenReturn(List.of(school));

        List<SchoolDTO> result = schoolService.getAll();

        assertEquals(1, result.size());
        assertEquals(1L, result.getFirst().id());
        assertEquals("AP", result.getFirst().name());
        assertEquals("ap.be", result.getFirst().domainName());
    }

    @Test
    void givenValidUpdate_whenUpdate_thenSaveAndReturnUpdatedDto() {
        Long schoolId = 1L;

        School existingSchool = schoolEntity(
                schoolId,
                "ap.be",
                "AP");

        SchoolDTO update = schoolDto(
                null,
                "nieuw.be",
                "AP Campus Noord");

        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(existingSchool));
        when(schoolRepository.save(any(School.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SchoolDTO result = schoolService.update(schoolId, update);

        assertEquals(1L, result.id());
        assertEquals("nieuw.be", result.domainName());
        assertEquals("AP Campus Noord", result.name());

        verify(schoolRepository).findById(schoolId);
        verify(schoolRepository).save(existingSchool);
    }

    @Test
    void givenUnknownSchoolId_whenUpdate_thenThrowException() {
        Long schoolId = 999L;

        SchoolDTO update = schoolDto(
                null,
                "ap.be",
                "AP Campus Noord");

        when(schoolRepository.findById(schoolId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> schoolService.update(schoolId, update));

        verify(schoolRepository, never()).save(any(School.class));
    }

    @Test
    void givenExistingSchoolId_whenDelete_thenMarkForDeletion() {
        Long schoolId = 1L;
        School school = schoolEntity(schoolId, "ap.be", "AP");

        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(school));

        schoolService.delete(schoolId);

        verify(schoolRepository).findById(schoolId);
        verify(schoolRepository).save(school);
    }

    @Test
    void givenUnknownSchoolId_whenDelete_thenThrowException() {
        Long schoolId = 999L;

        when(schoolRepository.findById(schoolId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> schoolService.delete(schoolId));

        verify(schoolRepository, never()).save(any(School.class));
    }

    @Test
    void givenUnknownSchoolId_whenPurge_thenThrowException() {
        Long schoolId = 999L;

        when(schoolRepository.existsById(schoolId)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> schoolService.purge(schoolId));

        verify(schoolRepository, never()).deleteById(schoolId);
    }

    @Test
    void givenExistingSchoolId_whenGetBookCountBySchool_thenReturnCount() {
        Long schoolId = 1L;

        when(schoolRepository.existsById(schoolId)).thenReturn(true);
        when(bookCopyRepository.countByCampusSchoolId(schoolId)).thenReturn(12L);

        long result = schoolService.getBookCountBySchool(schoolId);

        assertEquals(12L, result);
        verify(bookCopyRepository).countByCampusSchoolId(schoolId);
    }

    @Test
    void givenUnknownSchoolId_whenGetBookCountBySchool_thenThrowException() {
        Long schoolId = 999L;

        when(schoolRepository.existsById(schoolId)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> schoolService.getBookCountBySchool(schoolId));

        verify(bookCopyRepository, never()).countByCampusSchoolId(schoolId);
    }

    @Test
    void whenGetTotalBookCount_thenReturnCount() {
        when(bookCopyRepository.count()).thenReturn(50L);

        long result = schoolService.getTotalBookCount();

        assertEquals(50L, result);
    }

    @Test
    void givenExistingSchool_whenDisable_thenSetsStatusDisabledAndReturnsDto() {
        Long schoolId = 1L;
        School school = schoolEntity(schoolId, "ap.be", "AP");

        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(school));
        when(schoolRepository.save(any(School.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SchoolDTO result = schoolService.disable(schoolId);

        assertEquals(SchoolStatus.DISABLED, school.getStatus());
        verify(schoolRepository).save(school);
    }

    @Test
    void givenUnknownSchool_whenDisable_thenThrowsException() {
        when(schoolRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> schoolService.disable(999L));
    }

    @Test
    void givenDisabledSchool_whenEnable_thenSetsStatusActiveAndReturnsDto() {
        Long schoolId = 1L;
        School school = schoolEntity(schoolId, "ap.be", "AP");
        school.setStatus(SchoolStatus.DISABLED);

        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(school));
        when(schoolRepository.save(any(School.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SchoolDTO result = schoolService.enable(schoolId);

        assertEquals(SchoolStatus.ACTIVE, school.getStatus());
        verify(schoolRepository).save(school);
    }

    @Test
    void givenUnknownSchool_whenEnable_thenThrowsException() {
        when(schoolRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> schoolService.enable(999L));
    }

    @Test
    void givenNonExistentDomain_whenCheckDomain_thenDoesNotThrow() {
        when(schoolRepository.existsByDomainName("new.school.be")).thenReturn(false);

        schoolService.checkDomain("new.school.be");

        verify(schoolRepository).existsByDomainName("new.school.be");
    }

    @Test
    void givenExistingDomain_whenCheckDomain_thenThrowsResponseStatusException() {
        when(schoolRepository.existsByDomainName("existing.school.be")).thenReturn(true);

        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> schoolService.checkDomain("existing.school.be"));
    }

    @Test
    void givenDomainNameWithWhitespace_whenCheckDomain_thenNormalizesBeforeCheck() {
        when(schoolRepository.existsByDomainName("  new.School.Be  ".trim().toLowerCase())).thenReturn(false);

        schoolService.checkDomain("  new.School.Be  ");

        verify(schoolRepository).existsByDomainName("new.school.be");
    }

    @Test
    void givenExistingSchool_whenDelete_thenMarksForDeletionWithStatus() {
        Long schoolId = 1L;
        School school = schoolEntity(schoolId, "ap.be", "AP");

        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(school));

        schoolService.delete(schoolId);

        assertEquals(SchoolStatus.MARKED_FOR_DELETION, school.getStatus());
        assertNotNull(school.getMarkedForDeletionAt());
        verify(schoolRepository).save(school);
    }

    @Test
    void givenSchoolMarkedForDeletion_whenGetAll_thenDaysUntilDeletionIsCalculated() {
        School school = schoolEntity(1L, "ap.be", "AP");
        school.setStatus(SchoolStatus.MARKED_FOR_DELETION);
        school.setMarkedForDeletionAt(LocalDateTime.now());

        when(schoolRepository.findAll()).thenReturn(List.of(school));

        List<SchoolDTO> result = schoolService.getAll();

        assertEquals(1, result.size());
        assertEquals(SchoolStatus.MARKED_FOR_DELETION, result.getFirst().status());
        assertNotNull(result.getFirst().daysUntilDeletion());
        assertTrue(result.getFirst().daysUntilDeletion() > 0);
    }

    @Test
    void givenExistingSchoolId_whenPurge_thenExecutesDeleteQueries() {
        Long schoolId = 1L;
        when(schoolRepository.existsById(schoolId)).thenReturn(true);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.executeUpdate()).thenReturn(1);

        schoolService.purge(schoolId);

        verify(schoolRepository).existsById(schoolId);
        verify(entityManager, atLeast(20)).createNativeQuery(anyString());
        verify(schoolRepository).deleteById(schoolId);
    }

    @Test
    void givenDomainAlreadyTaken_WhenUpdate_ThenThrowsIllegalArgumentException() {
        Long schoolId = 1L;
        School existingSchool = schoolEntity(schoolId, "ap.be", "AP");

        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(existingSchool));
        when(schoolRepository.findByDomainName("other.be")).thenReturn(Optional.of(new School()));

        SchoolDTO update = schoolDto(schoolId, "other.be", "AP");

        assertThrows(IllegalArgumentException.class, () -> schoolService.update(schoolId, update));
    }

    @Test
    void givenNullCredential_whenCreate_thenThrowsException() {
        SchoolDTO dto = schoolDto(null, "new.be", "New School");

        assertThrows(IllegalArgumentException.class, () -> schoolService.create(dto));
    }

    @Test
    void givenBlankClientId_whenCreate_thenThrowsException() {
        SchoolDTO dto = schoolDtoWithCredentials(null, "new.be", "New School", "  ", "secret");

        assertThrows(IllegalArgumentException.class, () -> schoolService.create(dto));
    }

    @Test
    void givenDuplicateDomain_whenCreate_thenThrowsException() {
        when(schoolRepository.findByDomainName("existing.be")).thenReturn(Optional.of(new School()));

        SchoolDTO dto = schoolDtoWithCredentials(null, "existing.be", "Existing", "client", "secret");

        assertThrows(IllegalArgumentException.class, () -> schoolService.create(dto));
    }

    @Test
    void givenNullCampuses_whenCreate_thenThrowsException() {
        doNothing().when(oneRosterClient).validateCredentials("client", "secret", "nullcamp.be");
        when(schoolRepository.findByDomainName("nullcamp.be")).thenReturn(Optional.empty());

        SchoolDTO dto = new SchoolDTO(
                null, "nullcamp.be", "Null Camp", 15, null,
                LocalDateTime.now(), false,
                new OneRosterCredentialDTO("client", "secret"), null, null, null, null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> schoolService.create(dto));
        assertTrue(ex.getMessage().contains("Minstens één campus"));
    }

    @Test
    void givenDuplicateCampusNames_whenCreate_thenThrowsException() {
        doNothing().when(oneRosterClient).validateCredentials("client", "secret", "dup.be");
        when(schoolRepository.findByDomainName("dup.be")).thenReturn(Optional.empty());

        CampusDTO campus1 = new CampusDTO(null, "Main", null, null, "Straat 1", 1000, null, null);
        CampusDTO campus2 = new CampusDTO(null, "Main", null, null, "Straat 2", 2000, null, null);
        SchoolDTO dto = new SchoolDTO(
                null, "dup.be", "School", 15, List.of(campus1, campus2),
                LocalDateTime.now(), false,
                new OneRosterCredentialDTO("client", "secret"), null, null, null, null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> schoolService.create(dto));
        assertTrue(ex.getMessage().contains("Campusnamen moeten uniek"));
    }

    @Test
    void givenNormalizeCampusName_whenCreate_withoutCampusNameForMultiple_thenThrowsException() {
        doNothing().when(oneRosterClient).validateCredentials("client", "secret", "multi.be");
        when(schoolRepository.findByDomainName("multi.be")).thenReturn(Optional.empty());

        CampusDTO campus1 = new CampusDTO(null, "Campus A", null, null, "Straat 1", 1000, null, null);
        CampusDTO campus2 = new CampusDTO(null, null, null, null, "Straat 2", 2000, null, null);
        SchoolDTO dto = new SchoolDTO(
                null, "multi.be", "Multi", 15, List.of(campus1, campus2),
                LocalDateTime.now(), false,
                new OneRosterCredentialDTO("client", "secret"), null, null, null, null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> schoolService.create(dto));
        assertTrue(ex.getMessage().contains("Campusnaam is verplicht"));
    }

    @Test
    void givenUpdateWithCredentials_whenUpdate_thenSavesCredentialAndSyncs() {
        Long schoolId = 1L;
        School existingSchool = schoolEntity(schoolId, "ap.be", "AP");

        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(existingSchool));
        when(schoolRepository.save(any(School.class))).thenAnswer(i -> i.getArgument(0));
        when(credentialRepository.findBySchoolId(schoolId)).thenReturn(Optional.empty());

        SchoolDTO update = new SchoolDTO(
                schoolId, "ap.be", "AP", 15,
                List.of(campusDto()), LocalDateTime.now(), false,
                new OneRosterCredentialDTO("client", "secret"), null, null, null, null);

        schoolService.update(schoolId, update);

        verify(oneRosterClient).validateCredentials("client", "secret", "ap.be");
        verify(credentialRepository).save(any(OneRosterCredential.class));
        verify(oneRosterUserSyncService).syncSchoolAsync(existingSchool);
    }

    @Test
    void givenNormalizeAddress_whenCreate_withNullAddress_thenThrowsException() {
        doNothing().when(oneRosterClient).validateCredentials("client", "secret", "noaddr.be");
        when(schoolRepository.findByDomainName("noaddr.be")).thenReturn(Optional.empty());

        CampusDTO badCampus = new CampusDTO(null, null, null, null, null, null, null, null);
        SchoolDTO dto = new SchoolDTO(
                null, "noaddr.be", "No Addr", 15, List.of(badCampus),
                LocalDateTime.now(), false,
                new OneRosterCredentialDTO("client", "secret"), null, null, null, null);

        assertThrows(IllegalArgumentException.class, () -> schoolService.create(dto));
    }

    @Test
    void Given_OwnSchool_When_SetCatalogVisibilityTrue_Then_UpdatesAndSaves() {
        Long schoolId = 1L;
        School school = schoolEntity(schoolId, "ap.be", "AP");
        Authentication auth = mock(Authentication.class);

        when(currentUserService.getCurrentUserSchool(auth)).thenReturn(school);
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(school));
        when(schoolRepository.save(any(School.class))).thenAnswer(i -> i.getArgument(0));

        schoolService.setCatalogVisibility(schoolId, true, auth);

        assertTrue(school.getCatalogVisible());
        verify(schoolRepository).findById(schoolId);
        verify(schoolRepository).save(school);
    }

    @Test
    void Given_OwnSchool_When_SetCatalogVisibilityFalse_Then_UpdatesAndSaves() {
        Long schoolId = 1L;
        School school = schoolEntity(schoolId, "ap.be", "AP");
        school.setCatalogVisible(true);
        Authentication auth = mock(Authentication.class);

        when(currentUserService.getCurrentUserSchool(auth)).thenReturn(school);
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(school));
        when(schoolRepository.save(any(School.class))).thenAnswer(i -> i.getArgument(0));

        schoolService.setCatalogVisibility(schoolId, false, auth);

        assertFalse(school.getCatalogVisible());
        verify(schoolRepository).save(school);
    }

    @Test
    void Given_OtherSchool_When_SetCatalogVisibility_Then_ThrowsAccessDeniedException() {
        School userSchool = schoolEntity(1L, "user.be", "User School");
        Authentication auth = mock(Authentication.class);

        when(currentUserService.getCurrentUserSchool(auth)).thenReturn(userSchool);

        assertThrows(AccessDeniedException.class,
                () -> schoolService.setCatalogVisibility(2L, true, auth));
        verify(schoolRepository, never()).save(any(School.class));
    }

    @Test
    void Given_UnknownSchool_When_SetCatalogVisibility_Then_ThrowsIllegalArgumentException() {
        Long schoolId = 1L;
        School userSchool = schoolEntity(schoolId, "user.be", "User School");
        Authentication auth = mock(Authentication.class);

        when(currentUserService.getCurrentUserSchool(auth)).thenReturn(userSchool);
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> schoolService.setCatalogVisibility(schoolId, true, auth));
        verify(schoolRepository, never()).save(any(School.class));
    }

    @Test
    void Given_SchoolsWithMixedVisibility_When_GetAccessibleSchools_Then_ReturnsOwnAndVisible() {
        School ownSchool = schoolEntity(1L, "own.be", "Own");
        ownSchool.setCatalogVisible(false);
        School visibleSchool = schoolEntity(2L, "vis.be", "Visible");
        visibleSchool.setCatalogVisible(true);
        School hiddenSchool = schoolEntity(3L, "hid.be", "Hidden");
        hiddenSchool.setCatalogVisible(false);
        Authentication auth = mock(Authentication.class);

        when(currentUserService.getCurrentUserSchool(auth)).thenReturn(ownSchool);
        when(schoolRepository.findByStatus(SchoolStatus.ACTIVE))
                .thenReturn(List.of(ownSchool, visibleSchool, hiddenSchool));

        List<SchoolSummaryDto> result = schoolService.getAccessibleSchools(auth);

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(s -> s.id().equals(1L)));
        assertTrue(result.stream().anyMatch(s -> s.id().equals(2L)));
        assertTrue(result.stream().noneMatch(s -> s.id().equals(3L)));
    }

    @Test
    void Given_AllOtherSchoolsHidden_When_GetAccessibleSchools_Then_ReturnsOnlyOwnSchool() {
        School ownSchool = schoolEntity(1L, "own.be", "Own");
        ownSchool.setCatalogVisible(false);
        School hiddenA = schoolEntity(2L, "hidA.be", "Hidden A");
        hiddenA.setCatalogVisible(false);
        School hiddenB = schoolEntity(3L, "hidB.be", "Hidden B");
        hiddenB.setCatalogVisible(false);
        Authentication auth = mock(Authentication.class);

        when(currentUserService.getCurrentUserSchool(auth)).thenReturn(ownSchool);
        when(schoolRepository.findByStatus(SchoolStatus.ACTIVE))
                .thenReturn(List.of(ownSchool, hiddenA, hiddenB));

        List<SchoolSummaryDto> result = schoolService.getAccessibleSchools(auth);

        assertEquals(1, result.size());
        assertEquals(1L, result.getFirst().id());
    }

    private SchoolDTO schoolDto(
            Long id,
            String domainName,
            String name) {
        return new SchoolDTO(
                id,
                domainName,
                name,
                15,
                List.of(campusDto()),
                LocalDateTime.of(2026, 4, 28, 12, 0),
                false,
                null,
                null,
                null,
                null,
                null);
    }

    private SchoolDTO schoolDtoWithCredentials(
            Long id,
            String domainName,
            String name,
            String clientId,
            String clientSecret) {
        return new SchoolDTO(
                id,
                domainName,
                name,
                15,
                List.of(campusDto()),
                LocalDateTime.of(2026, 4, 28, 12, 0),
                false,
                new OneRosterCredentialDTO(clientId, clientSecret),
                null,
                null,
                null,
                null);
    }

    private edu.ap.bib_backend.dto.CampusDTO campusDto() {
        return new edu.ap.bib_backend.dto.CampusDTO(
                null,
                null,
                null,
                null,
                "Noorderplaats 2",
                2000,
                "03 123 45 67",
                "info@ap.be");
    }

    private School schoolEntity(
            Long id,
            String domainName,
            String name) {
        School school = new School();
        school.setId(id);
        school.setDomainName(domainName);
        school.setName(name);
        school.setCreatedAt(LocalDateTime.of(2026, 4, 28, 12, 0));
        return school;
    }
}
