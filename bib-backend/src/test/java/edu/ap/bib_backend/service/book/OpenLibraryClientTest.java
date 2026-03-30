package edu.ap.bib_backend.service.book;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import edu.ap.bib_backend.dto.BookDTO;
import edu.ap.bib_backend.dto.BookPreviewDTO;

@ExtendWith(MockitoExtension.class)
class OpenLibraryClientTest {

    @Mock
    private RestTemplate restTemplate;

    private OpenLibraryClient client;

    @BeforeEach
    void setUp() {
        client = new OpenLibraryClient(restTemplate);
    }

    @Test
    @DisplayName("Given publish date contains year, When fetchBookByISBN is called, Then extract year correctly")
    void givenPublishDateContainsYear_whenFetchBookByISBN_thenExtractYear() {
        Map<String, Object> bookData = Map.of(
                "title", "Test Boek",
                "publish_date", "May 12, 2001",
                "authors", List.of(Map.of("name", "Auteur X")),
                "number_of_pages", 321,
                "cover", Map.of("medium", "https://example.com/image.jpg"));

        Map<String, Object> response = Map.of("ISBN:9781234567890", bookData);

        given(restTemplate.getForObject(
                "https://openlibrary.org/api/books?bibkeys=ISBN:9781234567890&format=json&jscmd=data",
                Map.class))
                .willReturn(response);

        BookDTO result = client.fetchBookByISBN("9781234567890");

        assertThat(result.getIsbn()).isEqualTo("9781234567890");
        assertThat(result.getTitle()).isEqualTo("Test Boek");
        assertThat(result.getAuthor()).isEqualTo("Auteur X");
        assertThat(result.getPages()).isEqualTo(321);
        assertThat(result.getImageUrl()).isEqualTo("https://example.com/image.jpg");
        assertThat(result.getReleaseYear()).isEqualTo(2001);
    }

    @Test
    @DisplayName("Given empty response from OpenLibrary, When fetchBookByISBN is called, Then throw NotFound Exception")
    void givenEmptyResponseFromOpenLibrary_whenFetchBookByISBN_thenThrowNotFound() {
        given(restTemplate.getForObject(
                "https://openlibrary.org/api/books?bibkeys=ISBN:9781234567890&format=json&jscmd=data",
                Map.class))
                .willReturn(Map.of());

        assertThatThrownBy(() -> client.fetchBookByISBN("9781234567890"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode().value()).isEqualTo(404);
                    assertThat(rse.getReason()).isEqualTo("Geen boek gevonden voor ISBN: 9781234567890");
                });
    }

    @Test
    @DisplayName("Given valid ISBN and preview url from OpenLibrary, when fetchPreviewByISBN is called, then return available preview")
    void givenValidIsbnAndPreviewUrl_whenFetchPreviewByISBN_thenReturnAvailablePreview() {
        Map<String, Object> bookData = new HashMap<>();
        bookData.put("url", "/books/OL1M/Test");
        bookData.put("preview_url", "https://archive.org/details/testbook");

        Map<String, Object> response = new HashMap<>();
        response.put("ISBN:9789027439642", bookData);

        given(restTemplate.getForObject(anyString(), eq(Map.class))).willReturn(response);

        BookPreviewDTO result = client.fetchPreviewByISBN("978-90-274-3964-2");

        assertThat(result.isPreviewAvailable()).isTrue();
        assertThat(result.getPreviewUrl()).isEqualTo("https://archive.org/details/testbook");
        assertThat(result.getDetailsUrl()).isEqualTo("https://openlibrary.org/books/OL1M/Test");
        assertThat(result.getMessage()).isEqualTo("Preview beschikbaar");
        assertThat(result.getEmbedUrl()).isEqualTo("https://archive.org/embed/testbook");
    }

    @Test
    @DisplayName("Given valid ISBN and ocaid, when fetchPreviewByISBN is called, then build embed url")
    void givenValidIsbnAndOcaid_whenFetchPreviewByISBN_thenBuildEmbedUrl() {
        Map<String, Object> bookData = new HashMap<>();
        bookData.put("url", "/books/OL1M/Test");
        bookData.put("ocaid", "mybook123");

        Map<String, Object> response = new HashMap<>();
        response.put("ISBN:9789027439642", bookData);

        given(restTemplate.getForObject(anyString(), eq(Map.class))).willReturn(response);

        BookPreviewDTO result = client.fetchPreviewByISBN("9789027439642");

        assertThat(result.isPreviewAvailable()).isTrue();
        assertThat(result.getPreviewUrl()).isNull();
        assertThat(result.getEmbedUrl()).isEqualTo("https://archive.org/embed/mybook123");
        assertThat(result.getMessage()).isEqualTo("Preview beschikbaar");
    }

    @Test
    @DisplayName("Given ISBN not found, when fetchPreviewByISBN is called, then throw not found")
    void givenIsbnNotFound_whenFetchPreviewByISBN_thenThrowNotFound() {
        given(restTemplate.getForObject(anyString(), eq(Map.class))).willReturn(Collections.emptyMap());

        assertThatThrownBy(() -> client.fetchPreviewByISBN("9789027439642"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND")
                .hasMessageContaining("Geen boek gevonden voor dit ISBN");
    }

    @Test
    @DisplayName("Given invalid ISBN format, when fetchPreviewByISBN is called, then throw illegal argument")
    void givenInvalidIsbnFormat_whenFetchPreviewByISBN_thenThrowIllegalArgument() {
        assertThatThrownBy(() -> client.fetchPreviewByISBN("abc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ongeldig ISBN-formaat");
    }

    @Test
    @DisplayName("Given null ISBN, when fetchPreviewByISBN is called, then throw illegal argument")
    void givenNullIsbn_whenFetchPreviewByISBN_thenThrowIllegalArgument() {
        assertThatThrownBy(() -> client.fetchPreviewByISBN(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ISBN is verplicht");
    }

    @Test
    @DisplayName("Given OpenLibrary unavailable, when fetchPreviewByISBN is called, then throw service unavailable")
    void givenOpenLibraryUnavailable_whenFetchPreviewByISBN_thenThrowServiceUnavailable() {
        given(restTemplate.getForObject(anyString(), eq(Map.class))).willThrow(new RuntimeException("boom"));

        assertThatThrownBy(() -> client.fetchPreviewByISBN("9789027439642"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("503 SERVICE_UNAVAILABLE")
                .hasMessageContaining("Preview tijdelijk niet beschikbaar");
    }

    @Test
    @DisplayName("Given unsafe preview url, when fetchPreviewByISBN is called, then preview url is ignored")
    void givenUnsafePreviewUrl_whenFetchPreviewByISBN_thenPreviewUrlIsIgnored() {
        Map<String, Object> bookData = new HashMap<>();
        bookData.put("url", "/books/OL1M/Test");
        bookData.put("preview_url", "http://evil.com/preview");

        Map<String, Object> response = new HashMap<>();
        response.put("ISBN:9789027439642", bookData);

        given(restTemplate.getForObject(anyString(), eq(Map.class))).willReturn(response);

        BookPreviewDTO result = client.fetchPreviewByISBN("9789027439642");

        assertThat(result.isPreviewAvailable()).isFalse();
        assertThat(result.getPreviewUrl()).isNull();
        assertThat(result.getEmbedUrl()).isNull();
        assertThat(result.getMessage()).isEqualTo("Geen preview beschikbaar");
    }
}