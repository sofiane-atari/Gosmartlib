package edu.ap.bib_backend.service.book;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import edu.ap.bib_backend.dto.BookCandidateDTO;
import edu.ap.bib_backend.mapper.BookCandidateMapper;

@ExtendWith(MockitoExtension.class)
class GoogleBooksClientTest {

        private static final String GOOGLE_BOOKS_URL = "googleapis.com/books/v1/volumes";

        @Mock
        private RestTemplate restTemplate;

        private GoogleBooksClient client;

        @BeforeEach
        void setUp() {
                client = new GoogleBooksClient(restTemplate, "fake-api-key", new BookCandidateMapper());
        }

        @Test
        @DisplayName("Given matching book in Google API, when searchBookCandidates is called, then return candidate")
        void givenMatchingBookInGoogleApi_whenSearchBookCandidates_thenReturnCandidate() {
                Map<String, Object> response = responseOf(
                                bookItem(
                                                "Harry Potter and the Philosopher's Stone",
                                                List.of("J.K. Rowling"),
                                                "1997-06-26",
                                                "9780747532699",
                                                "https://example.com/hp.jpg"));

                mockGoogleResponse(response);

                List<BookCandidateDTO> result = client.searchBookCandidates(
                                "Harry Potter and the Philosopher's Stone",
                                "J.K. Rowling",
                                1997);

                assertThat(result).hasSize(1);

                BookCandidateDTO candidate = result.get(0);
                assertThat(candidate.getTitle()).isEqualTo("Harry Potter and the Philosopher's Stone");
                assertThat(candidate.getAuthor()).isEqualTo("J.K. Rowling");
                assertThat(candidate.getReleaseYear()).isEqualTo(1997);
                assertThat(candidate.getIsbn13()).isEqualTo("9780747532699");
                assertThat(candidate.getImageUrl()).isEqualTo("https://example.com/hp.jpg");
        }

        @Test
        @DisplayName("Given missing volume info, when searchBookCandidates is called, then throw not found")
        void givenMissingVolumeInfo_whenSearchBookCandidates_thenThrowNotFound() {
                Map<String, Object> response = Map.of("items", List.of(Map.of("id", "abc")));

                mockGoogleResponse(response);

                assertThatThrownBy(() -> client.searchBookCandidates("Test", "Auteur", 2020))
                                .isInstanceOf(ResponseStatusException.class)
                                .satisfies(ex -> {
                                        ResponseStatusException rse = (ResponseStatusException) ex;
                                        assertThat(rse.getStatusCode().value()).isEqualTo(404);
                                        assertThat(rse.getReason()).isEqualTo(
                                                        "Geen kandidaten gevonden voor title, author en releaseYear");
                                });
        }

        @Test
        @DisplayName("Given non-matching title, when searchBookCandidates is called, then throw not found")
        void givenNonMatchingTitle_whenSearchBookCandidates_thenThrowNotFound() {
                Map<String, Object> response = responseOf(
                                bookItem(
                                                "Completely Different Title",
                                                List.of("J.K. Rowling"),
                                                "1997",
                                                "9780747532699",
                                                null));

                mockGoogleResponse(response);

                assertThatThrownBy(() -> client.searchBookCandidates("Harry Potter", "J.K. Rowling", 1997))
                                .isInstanceOf(ResponseStatusException.class)
                                .satisfies(ex -> {
                                        ResponseStatusException rse = (ResponseStatusException) ex;
                                        assertThat(rse.getStatusCode().value()).isEqualTo(404);
                                        assertThat(rse.getReason()).isEqualTo(
                                                        "Geen kandidaten gevonden voor title, author en releaseYear");
                                });
        }

        @Test
        @DisplayName("Given non-matching author, when searchBookCandidates is called, then throw not found")
        void givenNonMatchingAuthor_whenSearchBookCandidates_thenThrowNotFound() {
                Map<String, Object> response = responseOf(
                                bookItem(
                                                "Harry Potter",
                                                List.of("Andere Auteur"),
                                                "1997",
                                                "9780747532699",
                                                null));

                mockGoogleResponse(response);

                assertThatThrownBy(() -> client.searchBookCandidates("Harry Potter", "J.K. Rowling", 1997))
                                .isInstanceOf(ResponseStatusException.class)
                                .satisfies(ex -> {
                                        ResponseStatusException rse = (ResponseStatusException) ex;
                                        assertThat(rse.getStatusCode().value()).isEqualTo(404);
                                        assertThat(rse.getReason()).isEqualTo(
                                                        "Geen kandidaten gevonden voor title, author en releaseYear");
                                });
        }

        @Test
        @DisplayName("Given multiple matching books, when searchBookCandidates is called, then sort by closest release year")
        void givenMultipleMatchingBooks_whenSearchBookCandidates_thenSortByClosestReleaseYear() {
                Map<String, Object> response = responseOf(
                                bookItem("Harry Potter", List.of("J.K. Rowling"), "2001", "9780747532699", null),
                                bookItem("Harry Potter", List.of("J.K. Rowling"), "1998", "9780747538493", null),
                                bookItem("Harry Potter", List.of("J.K. Rowling"), "1997", "9780747532743", null));

                mockGoogleResponse(response);

                List<BookCandidateDTO> result = client.searchBookCandidates("Harry Potter", "J.K. Rowling", 1997);

                assertThat(result).hasSize(3);
                assertThat(result)
                                .extracting(BookCandidateDTO::getReleaseYear)
                                .containsExactly(1997, 1998, 2001);
        }

        @Test
        @DisplayName("Given release year far away, when searchBookCandidates is called, then candidate is still returned and sorted")
        void givenReleaseYearFarAway_whenSearchBookCandidates_thenCandidateStillReturned() {
                Map<String, Object> response = responseOf(
                                bookItem("Harry Potter", List.of("J.K. Rowling"), "2005", "9780747532699", null));

                mockGoogleResponse(response);

                List<BookCandidateDTO> result = client.searchBookCandidates("Harry Potter", "J.K. Rowling", 1997);

                assertThat(result).hasSize(1);
                assertThat(result.get(0).getReleaseYear()).isEqualTo(2005);
        }

        @Test
        @DisplayName("Given null release year, when searchBookCandidates is called, then do not sort by year")
        void givenNullReleaseYear_whenSearchBookCandidates_thenDoNotSortByYear() {
                Map<String, Object> response = responseOf(
                                bookItem("Harry Potter", List.of("J.K. Rowling"), "2001", "9780747532699", null));

                mockGoogleResponse(response);

                List<BookCandidateDTO> result = client.searchBookCandidates("Harry Potter", "J.K. Rowling", null);

                assertThat(result).hasSize(1);
                assertThat(result.get(0).getReleaseYear()).isEqualTo(2001);
        }

        @Test
        @DisplayName("Given missing ISBN-13, when searchBookCandidates is called, then throw not found")
        void givenMissingIsbn13_whenSearchBookCandidates_thenThrowNotFound() {
                Map<String, Object> volumeInfo = Map.of(
                                "title", "Harry Potter",
                                "authors", List.of("J.K. Rowling"),
                                "publishedDate", "1997",
                                "industryIdentifiers", List.of(
                                                Map.of("type", "ISBN_10", "identifier", "0747532699")));

                Map<String, Object> response = Map.of("items", List.of(Map.of("volumeInfo", volumeInfo)));

                mockGoogleResponse(response);

                assertThatThrownBy(() -> client.searchBookCandidates("Harry Potter", "J.K. Rowling", 1997))
                                .isInstanceOf(ResponseStatusException.class)
                                .satisfies(ex -> {
                                        ResponseStatusException rse = (ResponseStatusException) ex;
                                        assertThat(rse.getStatusCode().value()).isEqualTo(404);
                                        assertThat(rse.getReason()).isEqualTo(
                                                        "Geen kandidaten gevonden voor title, author en releaseYear");
                                });
        }

        @Test
        @DisplayName("Given missing authors, when searchBookCandidates is called, then throw not found")
        void givenMissingAuthors_whenSearchBookCandidates_thenThrowNotFound() {
                Map<String, Object> volumeInfo = Map.of(
                                "title", "Harry Potter",
                                "publishedDate", "1997",
                                "industryIdentifiers", List.of(
                                                Map.of("type", "ISBN_13", "identifier", "9780747532699")));

                Map<String, Object> response = Map.of("items", List.of(Map.of("volumeInfo", volumeInfo)));

                mockGoogleResponse(response);

                assertThatThrownBy(() -> client.searchBookCandidates("Harry Potter", "J.K. Rowling", 1997))
                                .isInstanceOf(ResponseStatusException.class)
                                .satisfies(ex -> {
                                        ResponseStatusException rse = (ResponseStatusException) ex;
                                        assertThat(rse.getStatusCode().value()).isEqualTo(404);
                                        assertThat(rse.getReason()).isEqualTo(
                                                        "Geen kandidaten gevonden voor title, author en releaseYear");
                                });
        }

        @Test
        @DisplayName("Given response without items, when searchBookCandidates is called, then throw not found")
        void givenResponseWithoutItems_whenSearchBookCandidates_thenThrowNotFound() {
                Map<String, Object> response = Map.of();

                mockGoogleResponse(response);

                assertThatThrownBy(() -> client.searchBookCandidates("Harry Potter", "J.K. Rowling", 1997))
                                .isInstanceOf(ResponseStatusException.class)
                                .satisfies(ex -> {
                                        ResponseStatusException rse = (ResponseStatusException) ex;
                                        assertThat(rse.getStatusCode().value()).isEqualTo(404);
                                        assertThat(rse.getReason())
                                                        .isEqualTo("Geen boeken gevonden voor title en author");
                                });
        }

        @Test
        @DisplayName("Given empty items list, when searchBookCandidates is called, then throw not found")
        void givenEmptyItemsList_whenSearchBookCandidates_thenThrowNotFound() {
                Map<String, Object> response = Map.of("items", List.of());

                mockGoogleResponse(response);

                assertThatThrownBy(() -> client.searchBookCandidates("Harry Potter", "J.K. Rowling", 1997))
                                .isInstanceOf(ResponseStatusException.class)
                                .satisfies(ex -> {
                                        ResponseStatusException rse = (ResponseStatusException) ex;
                                        assertThat(rse.getStatusCode().value()).isEqualTo(404);
                                        assertThat(rse.getReason())
                                                        .isEqualTo("Geen boeken gevonden voor title en author");
                                });
        }

        @Test
        @DisplayName("Given more than 4 matching candidates, when searchBookCandidates is called, then only return 4")
        void givenMoreThanFourMatchingCandidates_whenSearchBookCandidates_thenReturnOnlyFour() {
                Map<String, Object> response = responseOf(
                                bookItem("Harry Potter", List.of("J.K. Rowling"), "1997", "9780747532700", null),
                                bookItem("Harry Potter", List.of("J.K. Rowling"), "1998", "9780747532701", null),
                                bookItem("Harry Potter", List.of("J.K. Rowling"), "1999", "9780747532702", null),
                                bookItem("Harry Potter", List.of("J.K. Rowling"), "2000", "9780747532703", null),
                                bookItem("Harry Potter", List.of("J.K. Rowling"), "2001", "9780747532704", null));

                mockGoogleResponse(response);

                List<BookCandidateDTO> result = client.searchBookCandidates("Harry Potter", "J.K. Rowling", 1997);

                assertThat(result).hasSize(4);
        }

        private void mockGoogleResponse(Map<String, Object> response) {
                given(restTemplate.getForObject(contains(GOOGLE_BOOKS_URL), eq(Map.class)))
                                .willReturn(response);
        }

        @SafeVarargs
        private final Map<String, Object> responseOf(Map<String, Object>... items) {
                return Map.of("items", List.of(items));
        }

        private Map<String, Object> bookItem(
                        String title,
                        List<String> authors,
                        String publishedDate,
                        String isbn13,
                        String imageUrl) {

                Map<String, Object> volumeInfo = new HashMap<>();
                volumeInfo.put("title", title);
                volumeInfo.put("authors", authors);
                volumeInfo.put("publishedDate", publishedDate);
                volumeInfo.put("industryIdentifiers", List.of(
                                Map.of("type", "ISBN_13", "identifier", isbn13)));

                if (imageUrl != null) {
                        volumeInfo.put("imageLinks", Map.of("thumbnail", imageUrl));
                }

                return Map.of("volumeInfo", volumeInfo);
        }
}