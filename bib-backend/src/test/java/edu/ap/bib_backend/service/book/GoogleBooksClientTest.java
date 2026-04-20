package edu.ap.bib_backend.service.book;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

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

        @Mock
        private RestTemplate restTemplate;

        private GoogleBooksClient client;

        @BeforeEach
        void setUp() {
                client = new GoogleBooksClient(restTemplate, "fake-api-key", new BookCandidateMapper());
        }

        @Test
        @DisplayName("Given matching book in Google API, When searchBookCandidates is called, Then return candidate")
        void givenMatchingBookInGoogleApi_whenSearchBookCandidates_thenReturnCandidate() {
                Map<String, Object> volumeInfo = Map.of(
                                "title", "Harry Potter and the Philosopher's Stone",
                                "authors", List.of("J.K. Rowling"),
                                "publishedDate", "1997-06-26",
                                "industryIdentifiers", List.of(
                                                Map.of("type", "ISBN_13", "identifier", "9780747532699")),
                                "imageLinks", Map.of("thumbnail", "https://example.com/hp.jpg"));

                Map<String, Object> item = Map.of("volumeInfo", volumeInfo);
                Map<String, Object> response = Map.of("items", List.of(item));

                given(restTemplate.getForObject(contains("googleapis.com/books/v1/volumes"), eq(Map.class)))
                                .willReturn(response);

                List<BookCandidateDTO> result = client.searchBookCandidates(
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
        @DisplayName("Given missing volume info in Google API, When searchBookCandidates is called, Then return empty list")
        void givenMissingVolumeInfo_whenSearchBookCandidates_thenReturnEmptyList() {
                Map<String, Object> item = Map.of("id", "abc");
                Map<String, Object> response = Map.of("items", List.of(item));

                given(restTemplate.getForObject(contains("googleapis.com/books/v1/volumes"), eq(Map.class)))
                                .willReturn(response);

                List<BookCandidateDTO> result = client.searchBookCandidates("Test", "Auteur", 2020);

                assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Given non-matching title in Google API, When searchBookCandidates is called, Then filter out item")
        void givenNonMatchingTitle_whenSearchBookCandidates_thenFilterOutItem() {
                Map<String, Object> volumeInfo = Map.of(
                                "title", "Completely Different Title",
                                "authors", List.of("J.K. Rowling"),
                                "publishedDate", "1997",
                                "industryIdentifiers", List.of(
                                                Map.of("type", "ISBN_13", "identifier", "9780747532699")));

                Map<String, Object> item = Map.of("volumeInfo", volumeInfo);
                Map<String, Object> response = Map.of("items", List.of(item));

                given(restTemplate.getForObject(contains("googleapis.com/books/v1/volumes"), eq(Map.class)))
                                .willReturn(response);

                List<BookCandidateDTO> result = client.searchBookCandidates("Harry Potter", "J.K. Rowling", 1997);

                assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Given non-matching author in Google API, When searchBookCandidates is called, Then filter out item")
        void givenNonMatchingAuthor_whenSearchBookCandidates_thenFilterOutItem() {
                Map<String, Object> volumeInfo = Map.of(
                                "title", "Harry Potter",
                                "authors", List.of("Andere Auteur"),
                                "publishedDate", "1997",
                                "industryIdentifiers", List.of(
                                                Map.of("type", "ISBN_13", "identifier", "9780747532699")));

                Map<String, Object> item = Map.of("volumeInfo", volumeInfo);
                Map<String, Object> response = Map.of("items", List.of(item));

                given(restTemplate.getForObject(contains("googleapis.com/books/v1/volumes"), eq(Map.class)))
                                .willReturn(response);

                List<BookCandidateDTO> result = client.searchBookCandidates("Harry Potter", "J.K. Rowling", 1997);

                assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Given multiple matching books with different release years, When searchBookCandidates is called, Then sort by closest release year")
        void givenMultipleMatchingBooks_whenSearchBookCandidates_thenSortByClosestReleaseYear() {
                Map<String, Object> volumeInfo1 = Map.of(
                                "title", "Harry Potter",
                                "authors", List.of("J.K. Rowling"),
                                "publishedDate", "2001",
                                "industryIdentifiers", List.of(
                                                Map.of("type", "ISBN_13", "identifier", "9780747532699")));

                Map<String, Object> volumeInfo2 = Map.of(
                                "title", "Harry Potter",
                                "authors", List.of("J.K. Rowling"),
                                "publishedDate", "1998",
                                "industryIdentifiers", List.of(
                                                Map.of("type", "ISBN_13", "identifier", "9780747538493")));

                Map<String, Object> volumeInfo3 = Map.of(
                                "title", "Harry Potter",
                                "authors", List.of("J.K. Rowling"),
                                "publishedDate", "1997",
                                "industryIdentifiers", List.of(
                                                Map.of("type", "ISBN_13", "identifier", "9780747532743")));

                Map<String, Object> item1 = Map.of("volumeInfo", volumeInfo1);
                Map<String, Object> item2 = Map.of("volumeInfo", volumeInfo2);
                Map<String, Object> item3 = Map.of("volumeInfo", volumeInfo3);
                Map<String, Object> response = Map.of("items", List.of(item1, item2, item3));

                given(restTemplate.getForObject(contains("googleapis.com/books/v1/volumes"), eq(Map.class)))
                                .willReturn(response);

                List<BookCandidateDTO> result = client.searchBookCandidates("Harry Potter", "J.K. Rowling", 1997);

                assertThat(result).hasSize(3);
                assertThat(result)
                                .extracting(BookCandidateDTO::getReleaseYear)
                                .containsExactly(1997, 1998, 2001);
        }

        @Test
        @DisplayName("Given release year within tolerance, When searchBookCandidates is called, Then return candidate")
        void givenReleaseYearWithinTolerance_whenSearchBookCandidates_thenReturnCandidate() {
                Map<String, Object> volumeInfo = Map.of(
                                "title", "Harry Potter",
                                "authors", List.of("J.K. Rowling"),
                                "publishedDate", "1998",
                                "industryIdentifiers", List.of(
                                                Map.of("type", "ISBN_13", "identifier", "9780747532699")));

                Map<String, Object> item = Map.of("volumeInfo", volumeInfo);
                Map<String, Object> response = Map.of("items", List.of(item));

                given(restTemplate.getForObject(contains("googleapis.com/books/v1/volumes"), eq(Map.class)))
                                .willReturn(response);

                List<BookCandidateDTO> result = client.searchBookCandidates("Harry Potter", "J.K. Rowling", 1997);

                assertThat(result).hasSize(1);
                assertThat(result.get(0).getReleaseYear()).isEqualTo(1998);
        }

        @Test
        @DisplayName("Given null release year, When searchBookCandidates is called, Then do not filter on year")
        void givenNullReleaseYear_whenSearchBookCandidates_thenDoNotFilterOnYear() {
                Map<String, Object> volumeInfo = Map.of(
                                "title", "Harry Potter",
                                "authors", List.of("J.K. Rowling"),
                                "publishedDate", "2001",
                                "industryIdentifiers", List.of(
                                                Map.of("type", "ISBN_13", "identifier", "9780747532699")));

                Map<String, Object> item = Map.of("volumeInfo", volumeInfo);
                Map<String, Object> response = Map.of("items", List.of(item));

                given(restTemplate.getForObject(contains("googleapis.com/books/v1/volumes"), eq(Map.class)))
                                .willReturn(response);

                List<BookCandidateDTO> result = client.searchBookCandidates("Harry Potter", "J.K. Rowling", null);

                assertThat(result).hasSize(1);
                assertThat(result.get(0).getReleaseYear()).isEqualTo(2001);
        }

        @Test
        @DisplayName("Given missing ISBN-13 in Google API, When searchBookCandidates is called, Then skip item")
        void givenMissingIsbn13_whenSearchBookCandidates_thenSkipItem() {
                Map<String, Object> volumeInfo = Map.of(
                                "title", "Harry Potter",
                                "authors", List.of("J.K. Rowling"),
                                "publishedDate", "1997",
                                "industryIdentifiers", List.of(
                                                Map.of("type", "ISBN_10", "identifier", "0747532699")));

                Map<String, Object> item = Map.of("volumeInfo", volumeInfo);
                Map<String, Object> response = Map.of("items", List.of(item));

                given(restTemplate.getForObject(contains("googleapis.com/books/v1/volumes"), eq(Map.class)))
                                .willReturn(response);

                List<BookCandidateDTO> result = client.searchBookCandidates("Harry Potter", "J.K. Rowling", 1997);

                assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Given missing authors in Google API, When searchBookCandidates is called, Then filter out item")
        void givenMissingAuthors_whenSearchBookCandidates_thenFilterOutItem() {
                Map<String, Object> volumeInfo = Map.of(
                                "title", "Harry Potter",
                                "publishedDate", "1997",
                                "industryIdentifiers", List.of(
                                                Map.of("type", "ISBN_13", "identifier", "9780747532699")));

                Map<String, Object> item = Map.of("volumeInfo", volumeInfo);
                Map<String, Object> response = Map.of("items", List.of(item));

                given(restTemplate.getForObject(contains("googleapis.com/books/v1/volumes"), eq(Map.class)))
                                .willReturn(response);

                List<BookCandidateDTO> result = client.searchBookCandidates("Harry Potter", "J.K. Rowling", 1997);

                assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Given null response from Google API, When searchBookCandidates is called, Then throw NotFound Exception")
        void givenNullResponseFromGoogle_whenSearchBookCandidates_thenThrowNotFound() {
                Map<String, Object> response = Map.of();

                given(restTemplate.getForObject(contains("googleapis.com/books/v1/volumes"), eq(Map.class)))
                                .willReturn(response);

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
        @DisplayName("Given empty items list from Google API, When searchBookCandidates is called, Then throw NotFound Exception")
        void givenEmptyItemsFromGoogle_whenSearchBookCandidates_thenThrowNotFound() {
                Map<String, Object> response = Map.of("items", List.of());

                given(restTemplate.getForObject(contains("googleapis.com/books/v1/volumes"), eq(Map.class)))
                                .willReturn(response);

                assertThatThrownBy(() -> client.searchBookCandidates("Harry Potter", "J.K. Rowling", 1997))
                                .isInstanceOf(ResponseStatusException.class)
                                .satisfies(ex -> {
                                        ResponseStatusException rse = (ResponseStatusException) ex;
                                        assertThat(rse.getStatusCode().value()).isEqualTo(404);
                                        assertThat(rse.getReason())
                                                        .isEqualTo("Geen boeken gevonden voor title en author");
                                });
        }
}