package edu.ap.bib_backend.dto;

import org.junit.jupiter.api.Test;

import edu.ap.bib_backend.dto.loan.LoanBookDTO;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class DtoTest {

    @Test
    void Given_LoanBookDTO_When_Instantiated_Then_ValuesAreSet() {
        LoanBookDTO dto = LoanBookDTO.builder().id(1L).title("Title").author("Author").isbn("12345").imageUrl(null).copiesCount(2).build();
        assertEquals(1L, dto.id());
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
        SmartschoolProfileDto dto = new SmartschoolProfileDto("id123", "John", "Doe", "John Doe", "john@test.be", "platform");
        assertEquals("John", dto.firstName());
        assertEquals("Doe", dto.lastName());
        assertEquals("john@test.be", dto.email());
    }
}
