package edu.ap.bib_backend.dto;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import edu.ap.bib_backend.jpa.enums.MailType;

class MailSubscriptionDTOTest {

    @Test
    void GivenAllFields_WhenBuildDto_ThenFieldsAreMapped() {
        LocalDateTime now = LocalDateTime.of(2026, 5, 15, 10, 30);
        MailSubscriptionDTO dto = MailSubscriptionDTO.builder()
                .id(1L)
                .bookId(42L)
                .bookTitle("Test Book")
                .mailType(MailType.AVAILABLE_BOOK)
                .createdAt(now)
                .build();

        assertEquals(1L, dto.id());
        assertEquals(42L, dto.bookId());
        assertEquals("Test Book", dto.bookTitle());
        assertEquals(MailType.AVAILABLE_BOOK, dto.mailType());
        assertEquals(now, dto.createdAt());
    }

    @Test
    void GivenRecord_WhenSerializedToJson_ThenProducesCorrectJson() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        MailSubscriptionDTO dto = MailSubscriptionDTO.builder()
                .id(1L)
                .bookId(42L)
                .bookTitle("Test Book")
                .mailType(MailType.AVAILABLE_BOOK)
                .build();

        String json = mapper.writeValueAsString(dto);

        assertTrue(json.contains("\"id\":1"));
        assertTrue(json.contains("\"bookId\":42"));
        assertTrue(json.contains("\"bookTitle\":\"Test Book\""));
        assertTrue(json.contains("\"mailType\":\"AVAILABLE_BOOK\""));
    }
}
