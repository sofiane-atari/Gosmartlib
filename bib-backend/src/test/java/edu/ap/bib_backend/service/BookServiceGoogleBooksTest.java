package edu.ap.bib_backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import edu.ap.bib_backend.dto.BookCandidateDTO;
import edu.ap.bib_backend.jpa.repository.BookCopyRepository;
import edu.ap.bib_backend.jpa.repository.BookLoanRepository;
import edu.ap.bib_backend.jpa.repository.BookRepository;
import edu.ap.bib_backend.jpa.repository.GenreRepository;
import edu.ap.bib_backend.jpa.repository.ReviewRepository;

@ExtendWith(MockitoExtension.class)
class BookServiceGoogleBooksTest {

    @Mock
    private BookRepository br;

    @Mock
    private BookCopyRepository bcr;

    @Mock
    private GenreRepository gr;

    @Mock
    private RestTemplate restTemplate;

    private BookService bs;
    
    private ReviewRepository reviewRepository;

    private BookLoanRepository bookLoanRepository;

    @BeforeEach
    void setUp() {
        bs = new BookService(br, bcr, gr, reviewRepository, bookLoanRepository, restTemplate, "fake-api-key");
    }

    @Test
    void searchBookCandidates_whenMatchingBookExists_returnsCandidate() {
        Map<String, Object> volumeInfo = Map.of(
                "title", "Harry Potter and the Philosopher's Stone",
                "authors", List.of("J.K. Rowling"),
                "publishedDate", "1997-06-26",
                "industryIdentifiers", List.of(
                        Map.of("type", "ISBN_13", "identifier", "9780747532699")),
                "imageLinks", Map.of("thumbnail", "https://example.com/hp.jpg"));

        Map<String, Object> item = Map.of("volumeInfo", volumeInfo);
        Map<String, Object> response = Map.of("items", List.of(item));

        when(restTemplate.getForObject(contains("googleapis.com/books/v1/volumes"), eq(Map.class)))
                .thenReturn(response);

        List<BookCandidateDTO> result = bs.searchBookCandidates(
                "Harry Potter and the Philosopher's Stone",
                "J.K. Rowling",
                1997);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Harry Potter and the Philosopher's Stone");
        assertThat(result.get(0).getAuthor()).isEqualTo("J.K. Rowling");
        assertThat(result.get(0).getReleaseYear()).isEqualTo(1997);
        assertThat(result.get(0).getIsbn13()).isEqualTo("9780747532699");
        assertThat(result.get(0).getImageUrl()).isEqualTo("https://example.com/hp.jpg");
    }

    @Test
    void searchBookCandidates_whenVolumeInfoMissing_skipsItem() {
        Map<String, Object> item = Map.of("id", "abc");
        Map<String, Object> response = Map.of("items", List.of(item));

        when(restTemplate.getForObject(contains("googleapis.com/books/v1/volumes"), eq(Map.class)))
                .thenReturn(response);

        assertThatThrownBy(() -> bs.searchBookCandidates("Test", "Auteur", 2020))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode().value()).isEqualTo(404);
                    assertThat(rse.getReason()).isEqualTo("Geen kandidaten gevonden voor title, author en releaseYear");
                });
    }

    @Test
    void searchBookCandidates_whenTitleDoesNotMatch_filtersOutItem() {
        Map<String, Object> volumeInfo = Map.of(
                "title", "Completely Different Title",
                "authors", List.of("J.K. Rowling"),
                "publishedDate", "1997",
                "industryIdentifiers", List.of(
                        Map.of("type", "ISBN_13", "identifier", "9780747532699")));

        Map<String, Object> item = Map.of("volumeInfo", volumeInfo);
        Map<String, Object> response = Map.of("items", List.of(item));

        when(restTemplate.getForObject(contains("googleapis.com/books/v1/volumes"), eq(Map.class)))
                .thenReturn(response);

        assertThatThrownBy(() -> bs.searchBookCandidates("Harry Potter", "J.K. Rowling", 1997))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode().value()).isEqualTo(404);
                });
    }

    @Test
    void searchBookCandidates_whenAuthorDoesNotMatch_filtersOutItem() {
        Map<String, Object> volumeInfo = Map.of(
                "title", "Harry Potter",
                "authors", List.of("Andere Auteur"),
                "publishedDate", "1997",
                "industryIdentifiers", List.of(
                        Map.of("type", "ISBN_13", "identifier", "9780747532699")));

        Map<String, Object> item = Map.of("volumeInfo", volumeInfo);
        Map<String, Object> response = Map.of("items", List.of(item));

        when(restTemplate.getForObject(contains("googleapis.com/books/v1/volumes"), eq(Map.class)))
                .thenReturn(response);

        assertThatThrownBy(() -> bs.searchBookCandidates("Harry Potter", "J.K. Rowling", 1997))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void searchBookCandidates_whenReleaseYearDoesNotMatch_filtersOutItem() {
        Map<String, Object> volumeInfo = Map.of(
                "title", "Harry Potter",
                "authors", List.of("J.K. Rowling"),
                "publishedDate", "2001",
                "industryIdentifiers", List.of(
                        Map.of("type", "ISBN_13", "identifier", "9780747532699")));

        Map<String, Object> item = Map.of("volumeInfo", volumeInfo);
        Map<String, Object> response = Map.of("items", List.of(item));

        when(restTemplate.getForObject(contains("googleapis.com/books/v1/volumes"), eq(Map.class)))
                .thenReturn(response);

        assertThatThrownBy(() -> bs.searchBookCandidates("Harry Potter", "J.K. Rowling", 1997))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void searchBookCandidates_whenReleaseYearIsNull_doesNotFilterOnYear() {
        Map<String, Object> volumeInfo = Map.of(
                "title", "Harry Potter",
                "authors", List.of("J.K. Rowling"),
                "publishedDate", "2001",
                "industryIdentifiers", List.of(
                        Map.of("type", "ISBN_13", "identifier", "9780747532699")));

        Map<String, Object> item = Map.of("volumeInfo", volumeInfo);
        Map<String, Object> response = Map.of("items", List.of(item));

        when(restTemplate.getForObject(contains("googleapis.com/books/v1/volumes"), eq(Map.class)))
                .thenReturn(response);

        List<BookCandidateDTO> result = bs.searchBookCandidates("Harry Potter", "J.K. Rowling", null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getReleaseYear()).isEqualTo(2001);
    }

    @Test
    void searchBookCandidates_whenNoIsbn13_skipsItem() {
        Map<String, Object> volumeInfo = Map.of(
                "title", "Harry Potter",
                "authors", List.of("J.K. Rowling"),
                "publishedDate", "1997",
                "industryIdentifiers", List.of(
                        Map.of("type", "ISBN_10", "identifier", "0747532699")));

        Map<String, Object> item = Map.of("volumeInfo", volumeInfo);
        Map<String, Object> response = Map.of("items", List.of(item));

        when(restTemplate.getForObject(contains("googleapis.com/books/v1/volumes"), eq(Map.class)))
                .thenReturn(response);

        assertThatThrownBy(() -> bs.searchBookCandidates("Harry Potter", "J.K. Rowling", 1997))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getReason()).isEqualTo("Geen kandidaten gevonden voor title, author en releaseYear");
                });
    }

    @Test
    void searchBookCandidates_whenAuthorsMissing_filtersOutItem() {
        Map<String, Object> volumeInfo = Map.of(
                "title", "Harry Potter",
                "publishedDate", "1997",
                "industryIdentifiers", List.of(
                        Map.of("type", "ISBN_13", "identifier", "9780747532699")));

        Map<String, Object> item = Map.of("volumeInfo", volumeInfo);
        Map<String, Object> response = Map.of("items", List.of(item));

        when(restTemplate.getForObject(contains("googleapis.com/books/v1/volumes"), eq(Map.class)))
                .thenReturn(response);

        assertThatThrownBy(() -> bs.searchBookCandidates("Harry Potter", "J.K. Rowling", 1997))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void searchBookCandidates_whenGoogleReturnsNoItems_throwsNotFound() {
        Map<String, Object> response = Map.of();

        when(restTemplate.getForObject(contains("googleapis.com/books/v1/volumes"), eq(Map.class)))
                .thenReturn(response);

        assertThatThrownBy(() -> bs.searchBookCandidates("Harry Potter", "J.K. Rowling", 1997))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode().value()).isEqualTo(404);
                    assertThat(rse.getReason()).isEqualTo("Geen boeken gevonden voor title en author");
                });
    }

    @Test
    void searchBookCandidates_whenGoogleReturnsEmptyItems_throwsNotFound() {
        Map<String, Object> response = Map.of("items", List.of());

        when(restTemplate.getForObject(contains("googleapis.com/books/v1/volumes"), eq(Map.class)))
                .thenReturn(response);

        assertThatThrownBy(() -> bs.searchBookCandidates("Harry Potter", "J.K. Rowling", 1997))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode().value()).isEqualTo(404);
                    assertThat(rse.getReason()).isEqualTo("Geen boeken gevonden voor title en author");
                });
    }
}