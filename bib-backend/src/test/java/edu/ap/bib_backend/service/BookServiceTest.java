package edu.ap.bib_backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import edu.ap.bib_backend.dto.BookCandidateDTO;
import edu.ap.bib_backend.dto.BookDTO;
import edu.ap.bib_backend.dto.BookPreviewDTO;
import edu.ap.bib_backend.dto.GenreDTO;
import edu.ap.bib_backend.exception.ResourceNotFoundException;
import edu.ap.bib_backend.dto.ReadingLevelDTO;
import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.entity.BookCopy;
import edu.ap.bib_backend.jpa.entity.Genre;
import edu.ap.bib_backend.jpa.enums.BookAvailabilityFilter;
import edu.ap.bib_backend.jpa.enums.BookSortOption;
import edu.ap.bib_backend.jpa.repository.BookCopyRepository;
import edu.ap.bib_backend.jpa.repository.BookLoanRepository;
import edu.ap.bib_backend.jpa.repository.BookRepository;
import edu.ap.bib_backend.jpa.repository.GenreRepository;
import edu.ap.bib_backend.jpa.repository.ReadingListBookRepository;
import edu.ap.bib_backend.jpa.repository.ReviewRepository;
import edu.ap.bib_backend.jpa.repository.ReadingLevelRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import java.util.Collections;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

        @Mock
        private BookRepository br;

        @Mock
        private BookCopyRepository bcr;

        @Mock
        private GenreRepository gr;

        @Mock
        private RestTemplate restTemplate;

        @Mock
        private ReadingLevelRepository rlr;

        @Mock
        private ReviewRepository reviewRepository;

        @Mock
        private BookLoanRepository bookLoanRepository;

        @Mock
        private ReadingListBookRepository readingListBookRepository;

        private BookService bs;

        @BeforeEach
        void setUp() {
                bs = new BookService(br, bcr, gr, rlr, reviewRepository, bookLoanRepository,  restTemplate, "fake-api-key");
        }

        @Test
        @DisplayName("Given books exist in DB, When getAllBooks is called, Then return mapped DTOs")
        void givenBooksExist_whenGetAllBooks_thenReturnMappedDTOs() {
                // Given
                Book book = createBook(
                                1L,
                                "9781234567890",
                                "Test Boek",
                                "Auteur X",
                                300,
                                "https://example.com/image.jpg",
                                "Beschrijving",
                                2022,
                                4.5,
                                Set.of(createGenre("Roman", "#8B5CF6", "Literaire fictie")));

                given(br.findAll()).willReturn(List.of(book));

                // When
                List<BookDTO> result = bs.getAllBooks();

                // Then
                assertThat(result).hasSize(1);
                assertThat(result.get(0).getId()).isEqualTo(1L);
                assertThat(result.get(0).getIsbn()).isEqualTo("9781234567890");
                assertThat(result.get(0).getTitle()).isEqualTo("Test Boek");
                assertThat(result.get(0).getAuthor()).isEqualTo("Auteur X");
                assertThat(result.get(0).getPages()).isEqualTo(300);
                assertThat(result.get(0).getImageUrl()).isEqualTo("https://example.com/image.jpg");
                assertThat(result.get(0).getDescription()).isEqualTo("Beschrijving");
                assertThat(result.get(0).getReleaseYear()).isEqualTo(2022);
                assertThat(result.get(0).getReviewScore()).isEqualTo(4.5);
                assertThat(result.get(0).getGenres()).hasSize(1);
                assertThat(result.get(0).getGenres().get(0).name()).isEqualTo("Roman");
        }

        @Test
        @DisplayName("Given valid book without ISBN, When addBook is called, Then save Book and BookCopy")
        void givenValidBookWithoutISBN_whenAddBook_thenSaveBookAndBookCopy() {
                // Given
                BookDTO dto = new BookDTO();
                dto.setTitle("Nieuwe Boek");
                dto.setAuthor("Auteur Test");
                dto.setPages(300);
                dto.setImageUrl("https://example.com/image.jpg");
                dto.setDescription("Beschrijving hier");
                dto.setReleaseYear(2024);
                dto.setGenres(List.of());

                Book savedBook = createBook(
                                1L,
                                null,
                                "Nieuwe Boek",
                                "Auteur Test",
                                300,
                                "https://example.com/image.jpg",
                                "Beschrijving hier",
                                2024,
                                0.0,
                                Set.of());

                given(br.save(any(Book.class))).willReturn(savedBook);
                given(bcr.save(any(BookCopy.class))).willAnswer(invocation -> invocation.getArgument(0));

                // When
                BookDTO result = bs.addBook(dto);

                // Then
                assertThat(result).isNotNull();
                assertThat(result.getId()).isEqualTo(1L);
                assertThat(result.getTitle()).isEqualTo("Nieuwe Boek");
                assertThat(result.getAuthor()).isEqualTo("Auteur Test");

                then(br).should().save(any(Book.class));
                then(bcr).should().save(any(BookCopy.class));
        }

        @Test
        @DisplayName("Given existing ISBN, When addBook is called, Then normalize ISBN and reuse Book")
        void givenExistingISBN_whenAddBook_thenNormalizeAndReuseExistingBook() {
                // Given
                BookDTO dto = new BookDTO();
                dto.setIsbn("978-1 234567890");
                dto.setTitle("Bestaand Boek");
                dto.setAuthor("Auteur");
                dto.setGenres(List.of());

                Book existingBook = createBook(
                                5L,
                                "9781234567890",
                                "Bestaand Boek",
                                "Auteur",
                                250,
                                null,
                                null,
                                2020,
                                3.5,
                                Set.of());

                given(br.findByIsbn("9781234567890")).willReturn(existingBook);
                given(bcr.save(any(BookCopy.class))).willAnswer(invocation -> invocation.getArgument(0));

                // When
                BookDTO result = bs.addBook(dto);

                // Then
                assertThat(dto.getIsbn()).isEqualTo("9781234567890");
                assertThat(result.getId()).isEqualTo(5L);
                assertThat(result.getIsbn()).isEqualTo("9781234567890");

                then(br).should().findByIsbn("9781234567890");
                then(br).should(never()).save(any(Book.class));
                then(bcr).should().save(any(BookCopy.class));
        }

        @Test
        @DisplayName("Given valid genres, When addBook is called, Then map existing genres correctly")
        void givenValidGenres_whenAddBook_thenMapExistingGenres() {
                // Given
                Genre genre = createGenre("Roman", "#8B5CF6", "Literaire fictie");

                BookDTO dto = new BookDTO();
                dto.setTitle("Boek");
                dto.setAuthor("Auteur");
                dto.setPages(123);
                dto.setGenres(List.of(
                                new GenreDTO("Roman", "#8B5CF6", "Literaire fictie", null)));

                Book savedBook = createBook(
                                7L,
                                null,
                                "Boek",
                                "Auteur",
                                123,
                                null,
                                null,
                                2024,
                                0.0,
                                Set.of(genre));

                given(gr.findByName("Roman")).willReturn(Optional.of(genre));
                given(br.save(any(Book.class))).willReturn(savedBook);
                given(bcr.save(any(BookCopy.class))).willAnswer(invocation -> invocation.getArgument(0));

                // When
                bs.addBook(dto);

                // Then
                ArgumentCaptor<Book> captor = ArgumentCaptor.forClass(Book.class);
                then(br).should().save(captor.capture());

                Book mappedBook = captor.getValue();
                assertThat(mappedBook.getGenres()).hasSize(1);
                assertThat(mappedBook.getGenres())
                                .extracting(Genre::getName)
                                .containsExactly("Roman");
        }

        @Test
        @DisplayName("Given unknown genre, When addBook is called, Then throw Exception")
        void givenUnknownGenre_whenAddBook_thenThrowException() {
                // Given
                BookDTO dto = new BookDTO();
                dto.setTitle("Boek");
                dto.setAuthor("Auteur");
                dto.setGenres(List.of(
                                new GenreDTO("Onbekend", "#000000", "Bestaat niet", null)));

                given(gr.findByName("Onbekend")).willReturn(Optional.empty());

                // When & Then
                assertThatThrownBy(() -> bs.addBook(dto))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessage("Genre bestaat niet: Onbekend");

                then(br).should(never()).save(any(Book.class));
                then(bcr).should(never()).save(any(BookCopy.class));
        }

        @Test
        @DisplayName("Given existing book ID, When getBookById is called, Then return DTO")
        void givenExistingBookId_whenGetBookById_thenReturnDTO() {
                // Given
                Book book = createBook(
                                3L,
                                "9781234567890",
                                "Boek 3",
                                "Auteur 3",
                                111,
                                null,
                                "Beschrijving",
                                2021,
                                4.2,
                                Set.of());

                given(br.findById(3L)).willReturn(Optional.of(book));

                // When
                BookDTO result = bs.getBookById(3L);

                // Then
                assertThat(result.getId()).isEqualTo(3L);
                assertThat(result.getTitle()).isEqualTo("Boek 3");
                assertThat(result.getAuthor()).isEqualTo("Auteur 3");
        }

        @Test
        @DisplayName("Given non-existing book ID, When getBookById is called, Then throw Exception")
        void givenNonExistingBookId_whenGetBookById_thenThrowException() {
                // Given
                given(br.findById(99L)).willReturn(Optional.empty());

                // When & Then
                assertThatThrownBy(() -> bs.getBookById(99L))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessage("Boek niet gevonden met id: 99");
        }

        @Test
        @DisplayName("Given books exist, When getMaxPages is called, Then return repository value")
        void givenBooksExist_whenGetMaxPages_thenReturnRepositoryValue() {
                // Given
                given(br.findMaxPages()).willReturn(900);

                // When
                Integer result = bs.getMaxPages();

                // Then
                assertThat(result).isEqualTo(900);
        }

        @Test
        @DisplayName("Given no books exist, When getMaxPages is called, Then return default 1500")
        void givenNoBooksExist_whenGetMaxPages_thenReturnDefault1500() {
                // Given
                given(br.findMaxPages()).willReturn(null);

                // When
                Integer result = bs.getMaxPages();

                // Then
                assertThat(result).isEqualTo(1500);
        }

        @Test
        @DisplayName("Given most read books exist, When getMostReadBooks is called, Then return mapped DTOs")
        void givenMostReadBooksExist_whenGetMostReadBooks_thenReturnMappedDTOs() {
                // Given
                Book book = createBook(
                                1L,
                                "9781234567890",
                                "Populair Boek",
                                "Auteur",
                                200,
                                null,
                                null,
                                2022,
                                4.0,
                                Set.of());

                given(br.findMostReadBook(PageRequest.of(0, 5))).willReturn(List.of(book));

                // When
                List<BookDTO> result = bs.getMostReadBooks(5);

                // Then
                assertThat(result).hasSize(1);
                assertThat(result.get(0).getTitle()).isEqualTo("Populair Boek");
        }

        @Test
        @DisplayName("Given no most read books, When getMostReadBooks is called, Then throw NotFound Exception")
        void givenNoMostReadBooks_whenGetMostReadBooks_thenThrowNotFound() {
                // Given
                given(br.findMostReadBook(PageRequest.of(0, 5))).willReturn(List.of());

                // When & Then
                assertThatThrownBy(() -> bs.getMostReadBooks(5))
                                .isInstanceOf(ResponseStatusException.class)
                                .satisfies(ex -> {
                                        ResponseStatusException rse = (ResponseStatusException) ex;
                                        assertThat(rse.getStatusCode().value()).isEqualTo(404);
                                        assertThat(rse.getReason()).isEqualTo("Geen uitgeleende boeken gevonden");
                                });
        }

        @Test
        @DisplayName("Given newest books exist, When getNewestBooks is called, Then return mapped DTOs")
        void givenNewestBooksExist_whenGetNewestBooks_thenReturnMappedDTOs() {
                // Given
                Book book = createBook(
                                2L,
                                "9781234567890",
                                "Nieuw Boek",
                                "Auteur",
                                180,
                                null,
                                null,
                                2025,
                                0.0,
                                Set.of());

                given(br.findNewestBooks(PageRequest.of(0, 3))).willReturn(List.of(book));

                // When
                List<BookDTO> result = bs.getNewestBooks(3);

                // Then
                assertThat(result).hasSize(1);
                assertThat(result.get(0).getTitle()).isEqualTo("Nieuw Boek");
                then(br).should().findNewestBooks(PageRequest.of(0, 3));
        }

        @Test
        @DisplayName("Given matching book in Google API, When searchBookCandidates is called, Then return candidate")
        void givenMatchingBookInGoogleApi_whenSearchBookCandidates_thenReturnCandidate() {
                // Given
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

                // When
                List<BookCandidateDTO> result = bs.searchBookCandidates(
                                "Harry Potter and the Philosopher's Stone",
                                "J.K. Rowling",
                                1997);

                // Then
                assertThat(result).hasSize(1);
                assertThat(result.get(0).getTitle()).isEqualTo("Harry Potter and the Philosopher's Stone");
                assertThat(result.get(0).getAuthor()).isEqualTo("J.K. Rowling");
                assertThat(result.get(0).getReleaseYear()).isEqualTo(1997);
                assertThat(result.get(0).getIsbn13()).isEqualTo("9780747532699");
                assertThat(result.get(0).getImageUrl()).isEqualTo("https://example.com/hp.jpg");
        }

        @Test
        @DisplayName("Given missing volume info in Google API, When searchBookCandidates is called, Then skip item")
        void givenMissingVolumeInfo_whenSearchBookCandidates_thenSkipItem() {
                // Given
                Map<String, Object> item = Map.of("id", "abc");
                Map<String, Object> response = Map.of("items", List.of(item));

                given(restTemplate.getForObject(contains("googleapis.com/books/v1/volumes"), eq(Map.class)))
                                .willReturn(response);

                // When & Then
                assertThatThrownBy(() -> bs.searchBookCandidates("Test", "Auteur", 2020))
                                .isInstanceOf(ResponseStatusException.class)
                                .satisfies(ex -> {
                                        ResponseStatusException rse = (ResponseStatusException) ex;
                                        assertThat(rse.getStatusCode().value()).isEqualTo(404);
                                        assertThat(rse.getReason()).isEqualTo(
                                                        "Geen kandidaten gevonden voor title, author en releaseYear");
                                });
        }

        @Test
        @DisplayName("Given non-matching title in Google API, When searchBookCandidates is called, Then filter out item")
        void givenNonMatchingTitle_whenSearchBookCandidates_thenFilterOutItem() {
                // Given
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

                // When & Then
                assertThatThrownBy(() -> bs.searchBookCandidates("Harry Potter", "J.K. Rowling", 1997))
                                .isInstanceOf(ResponseStatusException.class)
                                .satisfies(ex -> {
                                        ResponseStatusException rse = (ResponseStatusException) ex;
                                        assertThat(rse.getStatusCode().value()).isEqualTo(404);
                                });
        }

        @Test
        @DisplayName("Given non-matching author in Google API, When searchBookCandidates is called, Then filter out item")
        void givenNonMatchingAuthor_whenSearchBookCandidates_thenFilterOutItem() {
                // Given
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

                // When & Then
                assertThatThrownBy(() -> bs.searchBookCandidates("Harry Potter", "J.K. Rowling", 1997))
                                .isInstanceOf(ResponseStatusException.class);
        }

        @Test
        @DisplayName("Given non-matching release year in Google API, When searchBookCandidates is called, Then filter out item")
        void givenNonMatchingReleaseYear_whenSearchBookCandidates_thenFilterOutItem() {
                // Given
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

                // When & Then
                assertThatThrownBy(() -> bs.searchBookCandidates("Harry Potter", "J.K. Rowling", 1997))
                                .isInstanceOf(ResponseStatusException.class);
        }

        @Test
        @DisplayName("Given null release year, When searchBookCandidates is called, Then do not filter on year")
        void givenNullReleaseYear_whenSearchBookCandidates_thenDoNotFilterOnYear() {
                // Given
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

                // When
                List<BookCandidateDTO> result = bs.searchBookCandidates("Harry Potter", "J.K. Rowling", null);

                // Then
                assertThat(result).hasSize(1);
                assertThat(result.get(0).getReleaseYear()).isEqualTo(2001);
        }

        @Test
        @DisplayName("Given missing ISBN-13 in Google API, When searchBookCandidates is called, Then skip item")
        void givenMissingIsbn13_whenSearchBookCandidates_thenSkipItem() {
                // Given
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

                // When & Then
                assertThatThrownBy(() -> bs.searchBookCandidates("Harry Potter", "J.K. Rowling", 1997))
                                .isInstanceOf(ResponseStatusException.class)
                                .satisfies(ex -> {
                                        ResponseStatusException rse = (ResponseStatusException) ex;
                                        assertThat(rse.getReason()).isEqualTo(
                                                        "Geen kandidaten gevonden voor title, author en releaseYear");
                                });
        }

        @Test
        @DisplayName("Given missing authors in Google API, When searchBookCandidates is called, Then filter out item")
        void givenMissingAuthors_whenSearchBookCandidates_thenFilterOutItem() {
                // Given
                Map<String, Object> volumeInfo = Map.of(
                                "title", "Harry Potter",
                                "publishedDate", "1997",
                                "industryIdentifiers", List.of(
                                                Map.of("type", "ISBN_13", "identifier", "9780747532699")));

                Map<String, Object> item = Map.of("volumeInfo", volumeInfo);
                Map<String, Object> response = Map.of("items", List.of(item));

                given(restTemplate.getForObject(contains("googleapis.com/books/v1/volumes"), eq(Map.class)))
                                .willReturn(response);

                // When & Then
                assertThatThrownBy(() -> bs.searchBookCandidates("Harry Potter", "J.K. Rowling", 1997))
                                .isInstanceOf(ResponseStatusException.class);
        }

        @Test
        @DisplayName("Given null response from Google API, When searchBookCandidates is called, Then throw NotFound Exception")
        void givenNullResponseFromGoogle_whenSearchBookCandidates_thenThrowNotFound() {
                // Given
                Map<String, Object> response = Map.of();

                given(restTemplate.getForObject(contains("googleapis.com/books/v1/volumes"), eq(Map.class)))
                                .willReturn(response);

                // When & Then
                assertThatThrownBy(() -> bs.searchBookCandidates("Harry Potter", "J.K. Rowling", 1997))
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
                // Given
                Map<String, Object> response = Map.of("items", List.of());

                given(restTemplate.getForObject(contains("googleapis.com/books/v1/volumes"), eq(Map.class)))
                                .willReturn(response);

                // When & Then
                assertThatThrownBy(() -> bs.searchBookCandidates("Harry Potter", "J.K. Rowling", 1997))
                                .isInstanceOf(ResponseStatusException.class)
                                .satisfies(ex -> {
                                        ResponseStatusException rse = (ResponseStatusException) ex;
                                        assertThat(rse.getStatusCode().value()).isEqualTo(404);
                                        assertThat(rse.getReason())
                                                        .isEqualTo("Geen boeken gevonden voor title en author");
                                });
        }

        @Test
        @DisplayName("Given publish date contains year, When fetchBookByISBN is called, Then extract year correctly")
        void givenPublishDateContainsYear_whenFetchBookByISBN_thenExtractYear() {
                // Given
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

                // When
                BookDTO result = bs.fetchBookByISBN("9781234567890");

                // Then
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
                // Given
                given(restTemplate.getForObject(
                                "https://openlibrary.org/api/books?bibkeys=ISBN:9781234567890&format=json&jscmd=data",
                                Map.class))
                                .willReturn(Map.of());

                // When & Then
                assertThatThrownBy(() -> bs.fetchBookByISBN("9781234567890"))
                                .isInstanceOf(ResponseStatusException.class)
                                .satisfies(ex -> {
                                        ResponseStatusException rse = (ResponseStatusException) ex;
                                        assertThat(rse.getStatusCode().value()).isEqualTo(404);
                                        assertThat(rse.getReason())
                                                        .isEqualTo("Geen boek gevonden voor ISBN: 9781234567890");
                                });
        }

        @Test
        @DisplayName("Given related books exist, When getRelatedBooksCarousel is called, Then return mapped books")
        void givenRelatedBooksExist_whenGetRelatedBooksCarousel_thenReturnMappedBooks() {
                // Given
                Book source = new Book();
                source.setId(1L);

                Book related = new Book();
                related.setId(2L);
                related.setTitle("Related");
                related.setAuthor("Author");

                List<Object[]> relatedBooks = new ArrayList<>();
                relatedBooks.add(new Object[] { related, 2L });

                given(br.findById(1L)).willReturn(Optional.of(source));
                given(br.findRelatedBooksWithScore(1L, PageRequest.of(0, 10))).willReturn(relatedBooks);

                // When
                List<BookDTO> result = bs.getRelatedBooksCarousel(1L, 10);

                // Then
                assertThat(result).hasSize(1);
                assertThat(result.get(0).getId()).isEqualTo(2L);
                assertThat(result.get(0).getTitle()).isEqualTo("Related");
        }

        @Test
        @DisplayName("Given valid data, When updateBook is called, Then return updated DTO")
        void givenValidData_whenUpdateBook_thenReturnUpdatedDTO() {
                // Arrange
                Long bookId = 1L;
                Book existingBook = createBook(bookId, "9780000000000", "Oud", "Oud", 100, null, null, 2000, 0.0,
                                new HashSet<>());
                existingBook.setBookCopies(new ArrayList<>()); // Voorkom nullpointer in mapToDTO

                BookDTO updateDto = new BookDTO();
                updateDto.setIsbn("978-1234567890"); // Met streepje om normalizeIsbn te testen
                updateDto.setTitle("Nieuwe Titel");
                updateDto.setAuthor("Nieuwe Auteur");
                updateDto.setPages(250);
                updateDto.setGenres(new ArrayList<>());

                Book savedBook = createBook(bookId, "9781234567890", "Nieuwe Titel", "Nieuwe Auteur", 250, null, null,
                                2000, 0.0, new HashSet<>());
                savedBook.setBookCopies(new ArrayList<>());

                given(br.findById(bookId)).willReturn(Optional.of(existingBook));
                given(br.findByIsbn("9781234567890")).willReturn(null); // ISBN is nog niet in gebruik
                given(br.save(any(Book.class))).willReturn(savedBook);

                // Act
                BookDTO result = bs.updateBook(bookId, updateDto);

                // Assert
                assertThat(result).isNotNull();
                assertThat(result.getId()).isEqualTo(bookId);
                assertThat(result.getTitle()).isEqualTo("Nieuwe Titel");
                assertThat(result.getIsbn()).isEqualTo("9781234567890"); // Gecontroleerd op normalisatie

                then(br).should().findById(bookId);
                then(br).should().findByIsbn("9781234567890");
                then(br).should().save(any(Book.class));
        }

        @Test
        @DisplayName("Given same ISBN as before, When updateBook is called, Then update successfully")
        void givenSameIsbnAsBefore_whenUpdateBook_thenUpdateSuccessfully() {
                // Arrange
                Long bookId = 2L;
                String isbn = "9781111111111";

                Book existingBook = createBook(bookId, isbn, "Titel", "Auteur", 100, null, null, 2000, 0.0,
                                new HashSet<>());
                existingBook.setBookCopies(new ArrayList<>());

                BookDTO updateDto = new BookDTO();
                updateDto.setIsbn(isbn); // Hetzelfde ISBN
                updateDto.setTitle("Geüpdatete Titel");
                updateDto.setGenres(new ArrayList<>());

                given(br.findById(bookId)).willReturn(Optional.of(existingBook));
                // findByIsbn geeft hetzelfde boek terug (zelfde ID), dus dit mag!
                given(br.findByIsbn(isbn)).willReturn(existingBook);
                given(br.save(any(Book.class))).willAnswer(invocation -> invocation.getArgument(0));

                // Act
                BookDTO result = bs.updateBook(bookId, updateDto);

                // Assert
                assertThat(result.getTitle()).isEqualTo("Geüpdatete Titel");
                then(br).should().save(any(Book.class));
        }

        @Test
        @DisplayName("Given ISBN belonging to another book, When updateBook is called, Then throw BadRequest Exception")
        void givenIsbnBelongingToAnotherBook_whenUpdateBook_thenThrowBadRequestException() {
                // Arrange
                Long currentBookId = 3L;
                Long otherBookId = 4L;
                String duplicateIsbn = "9789999999999";

                Book currentBook = createBook(currentBookId, "9783333333333", "Titel", "Auteur", 100, null, null, 2000,
                                0.0, new HashSet<>());

                // Een ander boek heeft dit ISBN al in de database staan
                Book otherBookWithSameIsbn = createBook(otherBookId, duplicateIsbn, "Ander Boek", "Ander", 200, null,
                                null, 2010, 0.0, new HashSet<>());

                BookDTO updateDto = new BookDTO();
                updateDto.setIsbn(duplicateIsbn);

                given(br.findById(currentBookId)).willReturn(Optional.of(currentBook));
                given(br.findByIsbn(duplicateIsbn)).willReturn(otherBookWithSameIsbn);

                // Act & Assert
                assertThatThrownBy(() -> bs.updateBook(currentBookId, updateDto))
                                .isInstanceOf(ResponseStatusException.class)
                                .satisfies(ex -> {
                                        ResponseStatusException rse = (ResponseStatusException) ex;
                                        assertThat(rse.getStatusCode().value()).isEqualTo(400); // BAD_REQUEST
                                        assertThat(rse.getReason()).contains("Er bestaat al een ander boek met ISBN");
                                });

                then(br).should(never()).save(any(Book.class));
        }

        @Test
        @DisplayName("Given non-existing book ID, When updateBook is called, Then throw RuntimeException")
        void givenNonExistingBookId_whenUpdateBook_thenThrowException() {
                // Arrange
                Long invalidId = 99L;
                BookDTO updateDto = new BookDTO();
                updateDto.setTitle("Ghost Book");

                given(br.findById(invalidId)).willReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> bs.updateBook(invalidId, updateDto))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessage("Boek niet gevonden met id: 99");

                then(br).should(never()).findByIsbn(any());
                then(br).should(never()).save(any());
        }

        @Test
        @DisplayName("Given unknown reading level id, when addBook is called, then throw exception")
        void givenUnknownReadingLevelId_whenAddBook_thenThrowException() {
                // Given
                BookDTO dto = new BookDTO();
                dto.setTitle("Boek");
                dto.setAuthor("Auteur");
                dto.setGenres(List.of());
                dto.setReadingLevel(new ReadingLevelDTO(99L, "Z", "onbekend"));

                given(rlr.findById(99L)).willReturn(Optional.empty());

                // When / Then
                assertThatThrownBy(() -> bs.addBook(dto))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessage("Leesniveau niet gevonden");

                then(br).should(never()).save(any(Book.class));
                then(bcr).should(never()).save(any(BookCopy.class));
        }

        @Test
        @DisplayName("Given valid ISBN and preview url from OpenLibrary, when fetchPreviewByISBN is called, then return available preview")
        void givenValidIsbnAndPreviewUrl_whenFetchPreviewByISBN_thenReturnAvailablePreview() {
                // Given
                RestTemplate restTemplate = mock(RestTemplate.class);
                BookService spyService = spy(bs);

                doReturn(restTemplate).when(spyService).createRestTemplate();

                Map<String, Object> bookData = new HashMap<>();
                bookData.put("url", "/books/OL1M/Test");
                bookData.put("preview_url", "https://archive.org/details/testbook");

                Map<String, Object> response = new HashMap<>();
                response.put("ISBN:9789027439642", bookData);

                given(restTemplate.getForObject(anyString(), eq(Map.class))).willReturn(response);

                // When
                BookPreviewDTO result = spyService.fetchPreviewByISBN("978-90-274-3964-2");

                // Then
                assertThat(result.isPreviewAvailable()).isTrue();
                assertThat(result.getPreviewUrl()).isEqualTo("https://archive.org/details/testbook");
                assertThat(result.getDetailsUrl()).isEqualTo("https://openlibrary.org/books/OL1M/Test");
                assertThat(result.getMessage()).isEqualTo("Preview beschikbaar");
                assertThat(result.getEmbedUrl()).isEqualTo("https://archive.org/embed/testbook");
        }

        @Test
        @DisplayName("Given valid ISBN and ocaid, when fetchPreviewByISBN is called, then build embed url")
        void givenValidIsbnAndOcaid_whenFetchPreviewByISBN_thenBuildEmbedUrl() {
                // Given
                RestTemplate restTemplate = mock(RestTemplate.class);
                BookService spyService = spy(bs);

                doReturn(restTemplate).when(spyService).createRestTemplate();

                Map<String, Object> bookData = new HashMap<>();
                bookData.put("url", "/books/OL1M/Test");
                bookData.put("ocaid", "mybook123");

                Map<String, Object> response = new HashMap<>();
                response.put("ISBN:9789027439642", bookData);

                given(restTemplate.getForObject(anyString(), eq(Map.class))).willReturn(response);

                // When
                BookPreviewDTO result = spyService.fetchPreviewByISBN("9789027439642");

                // Then
                assertThat(result.isPreviewAvailable()).isTrue();
                assertThat(result.getPreviewUrl()).isNull();
                assertThat(result.getEmbedUrl()).isEqualTo("https://archive.org/embed/mybook123");
                assertThat(result.getMessage()).isEqualTo("Preview beschikbaar");
        }

        @Test
        @DisplayName("Given ISBN not found, when fetchPreviewByISBN is called, then throw not found")
        void givenIsbnNotFound_whenFetchPreviewByISBN_thenThrowNotFound() {
                // Given
                RestTemplate restTemplate = mock(RestTemplate.class);
                BookService spyService = spy(bs);

                doReturn(restTemplate).when(spyService).createRestTemplate();
                given(restTemplate.getForObject(anyString(), eq(Map.class))).willReturn(Collections.emptyMap());

                // When / Then
                assertThatThrownBy(() -> spyService.fetchPreviewByISBN("9789027439642"))
                                .isInstanceOf(ResponseStatusException.class)
                                .hasMessageContaining("404 NOT_FOUND")
                                .hasMessageContaining("Geen boek gevonden voor dit ISBN");
        }

        @Test
        @DisplayName("Given invalid ISBN format, when fetchPreviewByISBN is called, then throw bad request")
        void givenInvalidIsbnFormat_whenFetchPreviewByISBN_thenThrowBadRequest() {
                assertThatThrownBy(() -> bs.fetchPreviewByISBN("abc"))
                                .isInstanceOf(ResponseStatusException.class)
                                .hasMessageContaining("400 BAD_REQUEST")
                                .hasMessageContaining("Ongeldig ISBN-formaat");
        }

        @Test
        @DisplayName("Given null ISBN, when fetchPreviewByISBN is called, then throw bad request")
        void givenNullIsbn_whenFetchPreviewByISBN_thenThrowBadRequest() {
                assertThatThrownBy(() -> bs.fetchPreviewByISBN(null))
                                .isInstanceOf(ResponseStatusException.class)
                                .hasMessageContaining("400 BAD_REQUEST")
                                .hasMessageContaining("ISBN is verplicht");
        }

        @Test
        @DisplayName("Given OpenLibrary unavailable, when fetchPreviewByISBN is called, then throw service unavailable")
        void givenOpenLibraryUnavailable_whenFetchPreviewByISBN_thenThrowServiceUnavailable() {
                // Given
                RestTemplate restTemplate = mock(RestTemplate.class);
                BookService spyService = spy(bs);

                doReturn(restTemplate).when(spyService).createRestTemplate();
                given(restTemplate.getForObject(anyString(), eq(Map.class))).willThrow(new RuntimeException("boom"));

                // When / Then
                assertThatThrownBy(() -> spyService.fetchPreviewByISBN("9789027439642"))
                                .isInstanceOf(ResponseStatusException.class)
                                .hasMessageContaining("503 SERVICE_UNAVAILABLE")
                                .hasMessageContaining("Preview tijdelijk niet beschikbaar");
        }

        @Test
        @DisplayName("Given unsafe preview url, when fetchPreviewByISBN is called, then preview url is ignored")
        void givenUnsafePreviewUrl_whenFetchPreviewByISBN_thenPreviewUrlIsIgnored() {
                // Given
                RestTemplate restTemplate = mock(RestTemplate.class);
                BookService spyService = spy(bs);

                doReturn(restTemplate).when(spyService).createRestTemplate();

                Map<String, Object> bookData = new HashMap<>();
                bookData.put("url", "/books/OL1M/Test");
                bookData.put("preview_url", "http://evil.com/preview");

                Map<String, Object> response = new HashMap<>();
                response.put("ISBN:9789027439642", bookData);

                given(restTemplate.getForObject(anyString(), eq(Map.class))).willReturn(response);

                // When
                BookPreviewDTO result = spyService.fetchPreviewByISBN("9789027439642");

                // Then
                assertThat(result.isPreviewAvailable()).isFalse();
                assertThat(result.getPreviewUrl()).isNull();
                assertThat(result.getEmbedUrl()).isNull();
                assertThat(result.getMessage()).isEqualTo("Geen preview beschikbaar");
        }

        @Test
        @DisplayName("Given no filters, when getFilteredBooks is called, then use default paging and title ascending sort")
        void givenNoFilters_whenGetFilteredBooks_thenUseDefaultPagingAndSort() {
                // Given
                Page<Book> pageResult = new PageImpl<>(List.of());
                given(br.findAll(any(Specification.class), any(Pageable.class))).willReturn(pageResult);

                // When
                bs.getFilteredBooks(null, null, null, null, 0, 10, null);

                // Then
                ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
                then(br).should().findAll(any(Specification.class), pageableCaptor.capture());

                Pageable pageable = pageableCaptor.getValue();
                assertThat(pageable.getPageNumber()).isEqualTo(0);
                assertThat(pageable.getPageSize()).isEqualTo(10);
                assertThat(pageable.getSort().getOrderFor("title")).isNotNull();
                assertThat(pageable.getSort().getOrderFor("title").isAscending()).isTrue();
        }

        @Test
        @DisplayName("Given negative page, when getFilteredBooks is called, then normalize page to zero")
        void givenNegativePage_whenGetFilteredBooks_thenNormalizePageToZero() {
                given(br.findAll(any(Specification.class), any(Pageable.class)))
                                .willReturn(new PageImpl<>(List.of()));

                bs.getFilteredBooks(null, null, null, null, -5, 10, null);

                ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
                then(br).should().findAll(any(Specification.class), pageableCaptor.capture());

                assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(0);
        }

        @Test
        @DisplayName("Given size below one, when getFilteredBooks is called, then normalize size to one")
        void givenSizeBelowOne_whenGetFilteredBooks_thenNormalizeSizeToOne() {
                given(br.findAll(any(Specification.class), any(Pageable.class)))
                                .willReturn(new PageImpl<>(List.of()));

                bs.getFilteredBooks(null, null, null, null, 0, 0, null);

                ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
                then(br).should().findAll(any(Specification.class), pageableCaptor.capture());

                assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(1);
        }

        @Test
        @DisplayName("Given size above max, when getFilteredBooks is called, then cap size at fifty")
        void givenSizeAboveMax_whenGetFilteredBooks_thenCapSizeAtFifty() {
                given(br.findAll(any(Specification.class), any(Pageable.class)))
                                .willReturn(new PageImpl<>(List.of()));

                bs.getFilteredBooks(null, null, null, null, 0, 999, null);

                ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
                then(br).should().findAll(any(Specification.class), pageableCaptor.capture());

                assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(50);
        }

        @Test
        @DisplayName("Given minPages greater than maxPages, when getFilteredBooks is called, then throw exception")
        void givenMinPagesGreaterThanMaxPages_whenGetFilteredBooks_thenThrowException() {
                assertThatThrownBy(() -> bs.getFilteredBooks(null, null, 300, 100, 0, 10, null))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessage("minPages mag niet groter zijn dan maxPages.");
        }

        @Test
        @DisplayName("Given available filter, when getFilteredBooks is called, then query repository")
        void givenAvailableFilter_whenGetFilteredBooks_thenQueryRepository() {
                given(br.findAll(any(Specification.class), any(Pageable.class)))
                                .willReturn(new PageImpl<>(List.of()));

                bs.getFilteredBooks(null, BookAvailabilityFilter.AVAILABLE, null, null, 0, 10, null);

                then(br).should().findAll(any(Specification.class), any(Pageable.class));
        }

        @Test
        @DisplayName("Given AUTHOR_ZA sort, when getFilteredBooks is called, then use author descending")
        void givenAuthorZaSort_whenGetFilteredBooks_thenUseAuthorDescending() {
                given(br.findAll(any(Specification.class), any(Pageable.class)))
                                .willReturn(new PageImpl<>(List.of()));

                bs.getFilteredBooks(null, null, null, null, 0, 10, BookSortOption.AUTHOR_ZA);

                ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
                then(br).should().findAll(any(Specification.class), pageableCaptor.capture());

                Sort.Order order = pageableCaptor.getValue().getSort().getOrderFor("author");
                assertThat(order).isNotNull();
                assertThat(order.isDescending()).isTrue();
        }

        @Test
        @DisplayName("Given repository returns books, when getFilteredBooks is called, then map page content to dto")
        void givenRepositoryReturnsBooks_whenGetFilteredBooks_thenMapPageContentToDto() {
                // Given
                Book book = new Book();
                book.setId(5L);
                book.setTitle("Titel");
                book.setAuthor("Auteur");
                book.setPages(200);
                book.setGenres(Set.of());
                book.setBookCopies(List.of());
                book.setReviewScore(null);

                given(br.findAll(any(Specification.class), any(Pageable.class)))
                                .willReturn(new PageImpl<>(List.of(book)));

                // When
                Page<BookDTO> result = bs.getFilteredBooks(null, null, null, null, 0, 10, BookSortOption.AZ);

                // Then
                assertThat(result.getTotalElements()).isEqualTo(1);
                assertThat(result.getContent()).hasSize(1);
                assertThat(result.getContent().get(0).getId()).isEqualTo(5L);
                assertThat(result.getContent().get(0).getTitle()).isEqualTo("Titel");
                assertThat(result.getContent().get(0).getReviewScore()).isEqualTo(0.0);
        }

        private Book createBook(
                        Long id,
                        String isbn,
                        String title,
                        String author,
                        Integer pages,
                        String imageUrl,
                        String description,
                        Integer releaseYear,
                        Double reviewScore,
                        Set<Genre> genres) {

                Book book = new Book();
                book.setId(id);
                book.setIsbn(isbn);
                book.setTitle(title);
                book.setAuthor(author);
                book.setPages(pages);
                book.setImageUrl(imageUrl);
                book.setDescription(description);
                book.setReleaseYear(releaseYear);
                book.setReviewScore(reviewScore);
                book.setAddedDate(LocalDateTime.now());
                book.setGenres(genres);
                return book;
        }

        private Genre createGenre(String name, String color, String description) {
                Genre genre = new Genre();
                genre.setName(name);
                genre.setColor(color);
                genre.setDescription(description);
                return genre;
        }
}