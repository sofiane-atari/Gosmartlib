package edu.ap.bib_backend.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class BookDTOTest {
    @Test
    void constructor_ShouldMapFieldsCorrectly() {
        BookDTO book = new BookDTO(
                1L,
                "9781234567890",
                "Test Boek",
                "Auteur X",
                300,
                "https://example.com/image.jpg",
                "Beschrijving",
                2022,
                4.5,
                LocalDateTime.now(),
                List.of(
                        new GenreDTO("Roman", "#8B5CF6", "Literaire fictie", null, null),
                        new GenreDTO("Thriller", "#EF4444", "Spannend verhaal", null, null)));

        assertEquals(1L, book.getId());
        assertEquals("9781234567890", book.getIsbn()); // let op: getter blijft getISBN()
        assertEquals("Test Boek", book.getTitle());
        assertEquals("Auteur X", book.getAuthor());
        assertEquals(300, book.getPages());
        assertEquals("https://example.com/image.jpg", book.getImageUrl());
        assertEquals("Beschrijving", book.getDescription());
        assertEquals(2022, book.getReleaseYear());
        assertEquals(4.5, book.getReviewScore());
    }

    @Test
    void bookDTO_ShouldSerializeCorrectlyToJson() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        BookDTO book = new BookDTO(
                1L,
                "9781234567890",
                "Test Boek",
                "Auteur X",
                300,
                "https://example.com/image.jpg",
                "Beschrijving",
                2022,
                4.5,
                LocalDateTime.now(),
                List.of(
                        new GenreDTO("Roman", "#8B5CF6", "Literaire fictie", null, null),
                        new GenreDTO("Thriller", "#EF4444", "Spannend verhaal", null, null)));

        String json = mapper.writeValueAsString(book);

        assertTrue(json.contains("\"isbn\":\"9781234567890\""));
        assertTrue(json.contains("\"title\":\"Test Boek\""));
        assertTrue(json.contains("\"author\":\"Auteur X\""));
        assertTrue(json.contains("\"pages\":300"));
        assertTrue(json.contains("\"imageUrl\":\"https://example.com/image.jpg\""));
        assertTrue(json.contains("\"description\":\"Beschrijving\""));
        assertTrue(json.contains("\"releaseYear\":2022"));
        assertTrue(json.contains("\"reviewScore\":4.5"));
    }
}
