package edu.ap.bib_backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import edu.ap.bib_backend.dto.BookCandidateDTO;
import edu.ap.bib_backend.dto.BookDTO;
import edu.ap.bib_backend.dto.GenreDTO;
import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.entity.BookCopy;
import edu.ap.bib_backend.jpa.entity.Genre;
import edu.ap.bib_backend.jpa.repository.BookCopyRepository;
import edu.ap.bib_backend.jpa.repository.BookRepository;
import edu.ap.bib_backend.jpa.repository.GenreRepository;

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

    private BookService bs;

    @BeforeEach
    void setUp() {
        bs = new BookService(br, bcr, gr, restTemplate, "fake-api-key");
    }

    @Test
    void getAllBooks_returnsMappedDTOs() {
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
                List.of(createGenre("Roman", "#8B5CF6", "Literaire fictie")));

        when(br.findAll()).thenReturn(List.of(book));

        List<BookDTO> result = bs.getAllBooks();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getISBN()).isEqualTo("9781234567890");
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
    void addBook_withoutISBN_savesBookAndBookCopy() {
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
                List.of());

        when(br.save(any(Book.class))).thenReturn(savedBook);
        when(bcr.save(any(BookCopy.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookDTO result = bs.addBook(dto);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Nieuwe Boek");
        assertThat(result.getAuthor()).isEqualTo("Auteur Test");
        assertThat(result.getPages()).isEqualTo(300);
        assertThat(result.getDescription()).isEqualTo("Beschrijving hier");
        assertThat(result.getReleaseYear()).isEqualTo(2024);
        assertThat(result.getImageUrl()).isEqualTo("https://example.com/image.jpg");
        assertThat(result.getReviewScore()).isEqualTo(0.0);

        verify(br).save(any(Book.class));
        verify(bcr).save(any(BookCopy.class));
    }

    @Test
    void addBook_withISBN_normalizesAndUsesExistingBook() {
        BookDTO dto = new BookDTO();
        dto.setISBN("978-1 234567890");
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
                List.of());

        when(br.findByISBN("9781234567890")).thenReturn(existingBook);
        when(bcr.save(any(BookCopy.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookDTO result = bs.addBook(dto);

        assertThat(dto.getISBN()).isEqualTo("9781234567890");
        assertThat(result.getId()).isEqualTo(5L);
        assertThat(result.getISBN()).isEqualTo("9781234567890");

        verify(br).findByISBN("9781234567890");
        verify(br, never()).save(any(Book.class));
        verify(bcr).save(any(BookCopy.class));
    }

    @Test
    void addBook_withGenres_mapsExistingGenres() {
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
                List.of(genre));

        when(gr.findById("Roman")).thenReturn(Optional.of(genre));
        when(br.save(any(Book.class))).thenReturn(savedBook);
        when(bcr.save(any(BookCopy.class))).thenAnswer(invocation -> invocation.getArgument(0));

        bs.addBook(dto);

        ArgumentCaptor<Book> captor = ArgumentCaptor.forClass(Book.class);
        verify(br).save(captor.capture());

        Book mappedBook = captor.getValue();
        assertThat(mappedBook.getGenres()).hasSize(1);
        assertThat(mappedBook.getGenres().get(0).getName()).isEqualTo("Roman");
    }

    @Test
    void addBook_withUnknownGenre_throwsException() {
        BookDTO dto = new BookDTO();
        dto.setTitle("Boek");
        dto.setAuthor("Auteur");
        dto.setGenres(List.of(
                new GenreDTO("Onbekend", "#000000", "Bestaat niet", null)));

        when(gr.findById("Onbekend")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bs.addBook(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Genre bestaat niet: Onbekend");

        verify(br, never()).save(any(Book.class));
        verify(bcr, never()).save(any(BookCopy.class));
    }

    @Test
    void getBookById_whenFound_returnsDTO() {
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
                List.of());

        when(br.findById(3L)).thenReturn(Optional.of(book));

        BookDTO result = bs.getBookById(3L);

        assertThat(result.getId()).isEqualTo(3L);
        assertThat(result.getTitle()).isEqualTo("Boek 3");
        assertThat(result.getAuthor()).isEqualTo("Auteur 3");
    }

    @Test
    void getBookById_whenNotFound_throwsException() {
        when(br.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bs.getBookById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Boek niet gevonden met id: 99");
    }

    @Test
    void getMinReleaseYear_returnsRepositoryValue() {
        when(br.findMinReleaseYear()).thenReturn(1950);

        Integer result = bs.getMinReleaseYear();

        assertThat(result).isEqualTo(1950);
    }

    @Test
    void getMinReleaseYear_whenNull_returnsDefault1900() {
        when(br.findMinReleaseYear()).thenReturn(null);

        Integer result = bs.getMinReleaseYear();

        assertThat(result).isEqualTo(1900);
    }

    @Test
    void getMaxReleaseYear_returnsRepositoryValue() {
        when(br.findMaxReleaseYear()).thenReturn(2024);

        Integer result = bs.getMaxReleaseYear();

        assertThat(result).isEqualTo(2024);
    }

    @Test
    void getMaxPages_returnsRepositoryValue() {
        when(br.findMaxPages()).thenReturn(900);

        Integer result = bs.getMaxPages();

        assertThat(result).isEqualTo(900);
    }

    @Test
    void getMaxPages_whenNull_returnsDefault1500() {
        when(br.findMaxPages()).thenReturn(null);

        Integer result = bs.getMaxPages();

        assertThat(result).isEqualTo(1500);
    }

    @Test
    void getMostReadBooks_whenBooksExist_returnsMappedDTOs() {
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
                List.of());

        when(br.findMostReadBook(5)).thenReturn(List.of(book));

        List<BookDTO> result = bs.getMostReadBooks(5);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Populair Boek");
    }

    @Test
    void getMostReadBooks_whenEmpty_throwsNotFound() {
        when(br.findMostReadBook(5)).thenReturn(List.of());

        assertThatThrownBy(() -> bs.getMostReadBooks(5))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode().value()).isEqualTo(404);
                    assertThat(rse.getReason()).isEqualTo("Geen uitgeleende boeken gevonden");
                });
    }

    @Test
    void getNewestBooks_returnsMappedDTOs() {
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
                List.of());

        when(br.findNewestBooks(PageRequest.of(0, 3))).thenReturn(List.of(book));

        List<BookDTO> result = bs.getNewestBooks(3);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Nieuw Boek");
        verify(br).findNewestBooks(PageRequest.of(0, 3));
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
            List<Genre> genres) {

        Book book = new Book();
        book.setId(id);
        book.setISBN(isbn);
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

    @Test
    void fetchBookByISBN_whenPublishDateContainsYear_extractsYear() {
        Map<String, Object> bookData = Map.of(
                "title", "Test Boek",
                "publish_date", "May 12, 2001",
                "authors", List.of(Map.of("name", "Auteur X")),
                "number_of_pages", 321,
                "cover", Map.of("medium", "https://example.com/image.jpg"));

        Map<String, Object> response = Map.of("ISBN:9781234567890", bookData);

        when(restTemplate.getForObject(
                "https://openlibrary.org/api/books?bibkeys=ISBN:9781234567890&format=json&jscmd=data",
                Map.class))
                .thenReturn(response);

        BookDTO result = bs.fetchBookByISBN("9781234567890");

        assertThat(result.getISBN()).isEqualTo("9781234567890");
        assertThat(result.getTitle()).isEqualTo("Test Boek");
        assertThat(result.getAuthor()).isEqualTo("Auteur X");
        assertThat(result.getPages()).isEqualTo(321);
        assertThat(result.getImageUrl()).isEqualTo("https://example.com/image.jpg");
        assertThat(result.getReleaseYear()).isEqualTo(2001);
    }

    @Test
    void fetchBookByISBN_whenResponseEmpty_throwsNotFound() {
        when(restTemplate.getForObject(
                "https://openlibrary.org/api/books?bibkeys=ISBN:9781234567890&format=json&jscmd=data",
                Map.class))
                .thenReturn(Map.of());

        assertThatThrownBy(() -> bs.fetchBookByISBN("9781234567890"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode().value()).isEqualTo(404);
                    assertThat(rse.getReason()).isEqualTo("Geen boek gevonden voor ISBN: 9781234567890");
                });
    }
}
