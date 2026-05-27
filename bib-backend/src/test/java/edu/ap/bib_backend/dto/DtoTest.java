package edu.ap.bib_backend.dto;

import org.junit.jupiter.api.Test;

import edu.ap.bib_backend.dto.loan.LoanBookDTO;
import edu.ap.bib_backend.dto.oneroster.OneRosterCredentialDTO;
import edu.ap.bib_backend.dto.oneroster.OneRosterCredentialValidationDTO;
import edu.ap.bib_backend.jpa.enums.SchoolStatus;
import edu.ap.bib_backend.service.api.ProviderType;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class DtoTest {

    @Test
    void Given_LoanBookDTO_When_Instantiated_Then_ValuesAreSet() {
        LoanBookDTO dto = LoanBookDTO.builder().id(1L).title("Title").author("Author").isbn("12345").imageUrl(null).bookCopyIds(List.of(10L, 11L)).copiesCount(2).build();
        assertEquals(1L, dto.id());
        assertEquals(List.of(10L, 11L), dto.bookCopyIds());
        assertEquals(2, dto.copiesCount());
    }

    @Test
    void Given_OneRosterAudienceOptionsDto_When_Instantiated_Then_ValuesAreSet() {
        OneRosterAudienceOptionsDto dto = new OneRosterAudienceOptionsDto(List.of("class-1"), List.of("year-1"), List.of("school-1"));
        assertEquals(1, dto.schools().size());
        assertEquals("school-1", dto.schools().get(0));
        assertEquals(1, dto.classes().size());
    }

    @Test
    void Given_ValidationProgress_When_Instantiated_Then_ValuesAreSet() {
        ValidationProgress progress = new ValidationProgress(2, 10, 20, null);
        assertEquals(10, progress.getTotalRows());
        assertEquals(2, progress.getProcessedRows());
        assertEquals(20, progress.getProgress());
    }

    @Test
    void Given_UserNotificationStatusDto_When_Instantiated_Then_ValuesAreSet() {
        UserNotificationStatusDto dto = new UserNotificationStatusDto(true);
        assertTrue(dto.hasValidRefreshToken());
    }

    @Test
    void Given_SmartschoolGroupDto_When_Instantiated_Then_ValuesAreSet() {
        SmartschoolGroupDto dto = new SmartschoolGroupDto("GroupA", List.of("ParentA"));
        assertEquals("GroupA", dto.primaryGroupName());
        assertEquals("ParentA", dto.parentGroupNames().get(0));
    }

    @Test
    void Given_SmartschoolProfileDto_When_Instantiated_Then_ValuesAreSet() {
        SmartschoolProfileDto dto = new SmartschoolProfileDto("id123", "John", "Doe", "John Doe", "john@test.be", "platform", "johndoe");
        assertEquals("John", dto.firstName());
        assertEquals("Doe", dto.lastName());
        assertEquals("john@test.be", dto.email());
    }

    @Test
    void Given_SyncConfigDto_When_Instantiated_Then_ValuesAreSet() {
        SyncConfigDto dto = new SyncConfigDto("03:00", "Europe/Brussels", 1, "0 0 3 * * *");
        assertEquals("03:00", dto.syncTime());
        assertEquals("Europe/Brussels", dto.syncZone());
        assertEquals(1, dto.syncIntervalDays());
        assertEquals("0 0 3 * * *", dto.syncCron());
    }

    @Test
    void Given_OneRosterCredentialDTO_When_Instantiated_Then_ValuesAreSet() {
        OneRosterCredentialDTO dto = new OneRosterCredentialDTO("client-123", "secret-456");
        assertEquals("client-123", dto.clientId());
        assertEquals("secret-456", dto.clientSecret());
    }

    @Test
    void Given_OneRosterCredentialValidationDTO_When_Instantiated_Then_ValuesAreSet() {
        OneRosterCredentialValidationDTO dto = new OneRosterCredentialValidationDTO("school.be", "client-1", "secret-1");
        assertEquals("school.be", dto.domainName());
        assertEquals("client-1", dto.clientId());
        assertEquals("secret-1", dto.clientSecret());
    }

    @Test
    void Given_SchoolStatusEnum_When_ValuesRetrieved_Then_HasExpectedConstants() {
        assertEquals(3, SchoolStatus.values().length);
        assertEquals(SchoolStatus.ACTIVE, SchoolStatus.valueOf("ACTIVE"));
        assertEquals(SchoolStatus.DISABLED, SchoolStatus.valueOf("DISABLED"));
        assertEquals(SchoolStatus.MARKED_FOR_DELETION, SchoolStatus.valueOf("MARKED_FOR_DELETION"));
    }

    @Test
    void Given_ProviderTypeEnum_When_ValuesRetrieved_Then_HasExpectedConstants() {
        assertEquals(2, ProviderType.values().length);
        assertEquals(ProviderType.MOCK, ProviderType.valueOf("MOCK"));
        assertEquals(ProviderType.REAL, ProviderType.valueOf("REAL"));
    }
}
