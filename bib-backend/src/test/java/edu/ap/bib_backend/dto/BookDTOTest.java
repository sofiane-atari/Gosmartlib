package edu.ap.bib_backend.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

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
                4.5);

        assertEquals(1L, book.getId());
        assertEquals("9781234567890", book.getISBN()); // let op: getter blijft getISBN()
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

        BookDTO book = new BookDTO(
                1L,
                "9781234567890",
                "Test Boek",
                "Auteur X",
                300,
                "https://example.com/image.jpg",
                "Beschrijving",
                2022,
                4.5);

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
