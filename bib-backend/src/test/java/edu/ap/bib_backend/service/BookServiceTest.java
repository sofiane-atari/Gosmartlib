package edu.ap.bib_backend.service;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.doReturn;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import edu.ap.bib_backend.dto.BookDTO;
import edu.ap.bib_backend.dto.BookPreviewDTO;
import edu.ap.bib_backend.jpa.entity.Availability;
import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.repository.BookRepository;

@ExtendWith(MockitoExtension.class)
public class BookServiceTest {

    @Mock
    private BookRepository br;

    @InjectMocks
    private BookService bs;

    public BookServiceTest() {
    }

    @Test
    void given_books_exist_when_get_all_books_then_return_mapped_dtos() {

        Book book = new Book();
        book.setId(1L);
        book.setTitle("Test Book");
        book.setAuthor("Test Author");
        book.setISBN("1672587");
        book.setPages(324);
        book.setImageUrl("https://books.google.com/books/content?id=7p9mEAAAQBAJ&printsec=frontcover&img=1&zoom=1");
        // Extra velden toevoegen aan de entiteit
        book.setAvailability(Availability.AVAILABLE);
        book.setCopies(3);
        book.setDescription("dede de d e d ed e d e d e de d e d");
        book.setReleaseYear(2017);
        book.setReviewScore(4.5);

        when(br.findAll()).thenReturn(List.of(book));

        List<BookDTO> res = bs.getAllBooks();

        assertThat(res).hasSize(1);
        assertThat(res.get(0).getTitle()).isEqualTo("Test Book");
        assertThat(res.get(0).getAuthor()).isEqualTo("Test Author");
        assertThat(res.get(0).getISBN()).isEqualTo("1672587");
        assertThat(res.get(0).getPages()).isEqualTo(324);
        assertThat(res.get(0).getImageUrl())
                .isEqualTo("https://books.google.com/books/content?id=7p9mEAAAQBAJ&printsec=frontcover&img=1&zoom=1");

        // Assertions voor de nieuwe velden
        assertThat(res.get(0).getAvailability()).isEqualTo(Availability.AVAILABLE);
        assertThat(res.get(0).getCopies()).isEqualTo(3);
        assertThat(res.get(0).getDescription()).isEqualTo("dede de d e d ed e d e d e de d e d");
        assertThat(res.get(0).getReleaseYear()).isEqualTo(2017);
        assertThat(res.get(0).getReviewScore()).isEqualTo(4.5);
    }

    @Test
    void given_existing_book_id_when_get_book_by_id_then_return_mapped_dto() {
        // Arrange
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Specifiek Boek");
        book.setAuthor("Bekende Auteur");
        book.setISBN("987654321L");
        book.setPages(250);
        book.setImageUrl("https://example.com/image.jpg");
        book.setAvailability(Availability.AVAILABLE);
        book.setCopies(5);
        book.setDescription("Beschrijving van het specifieke boek.");
        book.setReleaseYear(2021);
        book.setReviewScore(3.8);

        // Mock de repository: als er gezocht wordt op ID 1, geef het boek terug
        when(br.findById(1L)).thenReturn(Optional.of(book));

        // Act
        BookDTO res = bs.getBookById(1L);

        // Assert
        assertThat(res).isNotNull();
        assertThat(res.getId()).isEqualTo(1L);
        assertThat(res.getTitle()).isEqualTo("Specifiek Boek");
        assertThat(res.getAvailability()).isEqualTo(Availability.AVAILABLE);
        assertThat(res.getReleaseYear()).isEqualTo(2021);
        assertThat(res.getReviewScore()).isEqualTo(3.8);
    }

    @Test
    void given_book_dto_without_isbn_when_create_book_then_save_and_return_dto() {
        // Arrange
        BookDTO dto = new BookDTO();
        dto.setTitle("Nieuwe Boek");
        dto.setAuthor("Auteur Test");
        dto.setPages(300);
        dto.setImageUrl("https://example.com/image.jpg");
        dto.setAvailability(Availability.AVAILABLE);
        dto.setCopies(2);
        dto.setDescription("Beschrijving hier");
        dto.setReleaseYear(2024);

        // Mock the repository save method
        Book savedBook = new Book();
        savedBook.setId(1L); // Simulate DB-generated ID
        savedBook.setTitle(dto.getTitle());
        savedBook.setAuthor(dto.getAuthor());
        savedBook.setPages(dto.getPages());
        savedBook.setImageUrl(dto.getImageUrl());
        savedBook.setAvailability(dto.getAvailability());
        savedBook.setCopies(dto.getCopies());
        savedBook.setDescription(dto.getDescription());
        savedBook.setReleaseYear(dto.getReleaseYear());
        savedBook.setReviewScore(0.0);

        Mockito.when(br.save(Mockito.any(Book.class))).thenReturn(savedBook);

        // Act
        BookDTO result = bs.addBook(dto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Nieuwe Boek");
        assertThat(result.getAuthor()).isEqualTo("Auteur Test");
        assertThat(result.getPages()).isEqualTo(300);
        assertThat(result.getAvailability()).isEqualTo(Availability.AVAILABLE);
        assertThat(result.getCopies()).isEqualTo(2);
        assertThat(result.getDescription()).isEqualTo("Beschrijving hier");
        assertThat(result.getReleaseYear()).isEqualTo(2024);
        assertThat(result.getImageUrl()).isEqualTo("https://example.com/image.jpg");
        assertThat(result.getReviewScore()).isEqualTo(0.0);
    }

    // ==================== getNewestBooks Tests ====================

    @Test
    void given_books_exist_when_get_newest_books_then_return_ordered_books() {
        // Arrange
        int limit = 3;

        Book newestBook = new Book();
        newestBook.setId(3L);
        newestBook.setTitle("Newest Book");
        newestBook.setAuthor("Author C");
        newestBook.setISBN("333");
        newestBook.setPages(200);
        newestBook.setAvailability(Availability.AVAILABLE);
        newestBook.setCopies(1);
        newestBook.setReleaseYear(2024);

        Book middleBook = new Book();
        middleBook.setId(2L);
        middleBook.setTitle("Middle Book");
        middleBook.setAuthor("Author B");
        middleBook.setISBN("222");
        middleBook.setPages(250);
        middleBook.setAvailability(Availability.AVAILABLE);
        middleBook.setCopies(2);
        middleBook.setReleaseYear(2023);

        Book oldestBook = new Book();
        oldestBook.setId(1L);
        oldestBook.setTitle("Oldest Book");
        oldestBook.setAuthor("Author A");
        oldestBook.setISBN("111");
        oldestBook.setPages(300);
        oldestBook.setAvailability(Availability.AVAILABLE);
        oldestBook.setCopies(3);
        oldestBook.setReleaseYear(2022);

        List<Book> books = List.of(newestBook, middleBook, oldestBook);

        when(br.findNewestBooks(PageRequest.of(0, limit))).thenReturn(books);

        // Act
        List<BookDTO> result = bs.getNewestBooks(limit);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);

        assertThat(result.get(0).getId()).isEqualTo(3L);
        assertThat(result.get(0).getTitle()).isEqualTo("Newest Book");

        assertThat(result.get(1).getId()).isEqualTo(2L);
        assertThat(result.get(1).getTitle()).isEqualTo("Middle Book");

        assertThat(result.get(2).getId()).isEqualTo(1L);
        assertThat(result.get(2).getTitle()).isEqualTo("Oldest Book");

        verify(br, times(1)).findNewestBooks(PageRequest.of(0, limit));
    }

    @Test
    void given_default_limit_when_get_newest_books_then_return_three_books() {
        // Arrange
        int defaultLimit = 3;

        Book book1 = new Book();
        book1.setId(1L);
        book1.setTitle("Book 1");

        Book book2 = new Book();
        book2.setId(2L);
        book2.setTitle("Book 2");

        Book book3 = new Book();
        book3.setId(3L);
        book3.setTitle("Book 3");

        List<Book> books = List.of(book1, book2, book3);

        when(br.findNewestBooks(PageRequest.of(0, defaultLimit))).thenReturn(books);

        // Act
        List<BookDTO> result = bs.getNewestBooks(defaultLimit);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);

        verify(br, times(1)).findNewestBooks(PageRequest.of(0, defaultLimit));
    }

    // ==================== getMostReadBooks Tests ====================

    @Test
    void given_books_exist_when_get_most_read_books_then_return_books() {
        // Arrange
        int limit = 3;

        Book mostReadBook = new Book();
        mostReadBook.setId(1L);
        mostReadBook.setTitle("Most Read Book");
        mostReadBook.setAuthor("Popular Author");
        mostReadBook.setISBN("111");
        mostReadBook.setPages(400);
        mostReadBook.setAvailability(Availability.AVAILABLE);
        mostReadBook.setCopies(5);
        mostReadBook.setReleaseYear(2020);

        Book secondBook = new Book();
        secondBook.setId(2L);
        secondBook.setTitle("Second Most Read");
        secondBook.setAuthor("Author B");
        secondBook.setISBN("222");
        secondBook.setPages(300);
        secondBook.setAvailability(Availability.AVAILABLE);
        secondBook.setCopies(3);
        secondBook.setReleaseYear(2021);

        Book thirdBook = new Book();
        thirdBook.setId(3L);
        thirdBook.setTitle("Third Most Read");
        thirdBook.setAuthor("Author C");
        thirdBook.setISBN("333");
        thirdBook.setPages(250);
        thirdBook.setAvailability(Availability.BORROWED);
        thirdBook.setCopies(2);
        thirdBook.setReleaseYear(2022);

        List<Book> books = List.of(mostReadBook, secondBook, thirdBook);

        when(br.findMostReadBook(limit)).thenReturn(books);

        // Act
        List<BookDTO> result = bs.getMostReadBooks(limit);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);

        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getTitle()).isEqualTo("Most Read Book");

        assertThat(result.get(1).getId()).isEqualTo(2L);
        assertThat(result.get(1).getTitle()).isEqualTo("Second Most Read");

        assertThat(result.get(2).getId()).isEqualTo(3L);
        assertThat(result.get(2).getTitle()).isEqualTo("Third Most Read");

        verify(br, times(1)).findMostReadBook(limit);
    }

    @Test
    void given_no_books_exist_when_get_most_read_books_then_throw_exception() {
        // Arrange
        int limit = 3;

        when(br.findMostReadBook(limit)).thenReturn(List.of());

        // Act
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> bs.getMostReadBooks(limit));

        // Assert
        assertThat(exception.getReason()).isEqualTo("Geen uitgeleende boeken gevonden");

        verify(br, times(1)).findMostReadBook(limit);
    }

    @Test
    void given_custom_limit_when_get_most_read_books_then_return_requested_number_of_books() {
        // Arrange
        int limit = 5;

        Book book1 = new Book();
        book1.setId(1L);
        book1.setTitle("Book 1");

        Book book2 = new Book();
        book2.setId(2L);
        book2.setTitle("Book 2");

        List<Book> books = List.of(book1, book2);

        when(br.findMostReadBook(limit)).thenReturn(books);

        // Act
        List<BookDTO> result = bs.getMostReadBooks(limit);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);

        verify(br, times(1)).findMostReadBook(limit);
    }

    @Test
    void given_missing_book_id_when_get_book_by_id_then_throw_runtime_exception() {
        when(br.findById(99L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> bs.getBookById(99L));

        assertThat(exception.getMessage()).isEqualTo("Boek niet gevonden met id: 99");
    }

    @Test
    void given_null_min_release_year_when_get_min_release_year_then_use_default_1900() {
        when(br.findMinReleaseYear()).thenReturn(null);

        Integer result = bs.getMinReleaseYear();

        assertThat(result).isEqualTo(1900);
    }

    @Test
    void given_null_max_release_year_when_get_max_release_year_then_use_current_year() {
        when(br.findMaxReleaseYear()).thenReturn(null);

        Integer result = bs.getMaxReleaseYear();

        assertThat(result).isEqualTo(LocalDate.now().getYear());
    }

    @Test
    void given_null_max_pages_when_get_max_pages_then_use_default_1500() {
        when(br.findMaxPages()).thenReturn(null);

        Integer result = bs.getMaxPages();

        assertThat(result).isEqualTo(1500);
    }

    @Test
    void given_invalid_isbn_when_fetch_preview_by_isbn_then_throw_bad_request() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> bs.fetchPreviewByISBN("abc"));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getReason()).isEqualTo("Ongeldig ISBN-formaat");
    }

    @Test
    void given_api_call_failure_when_fetch_preview_by_isbn_then_throw_service_unavailable() {
        BookService service = Mockito.spy(new BookService(br));
        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        doReturn(restTemplate).when(service).createRestTemplate();

        when(restTemplate.getForObject(Mockito.anyString(), Mockito.eq(Map.class)))
                .thenThrow(new RuntimeException("temporary network issue"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.fetchPreviewByISBN("9780306406157"));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(exception.getReason()).isEqualTo("Preview tijdelijk niet beschikbaar");
    }

    @Test
    void given_empty_response_when_fetch_preview_by_isbn_then_throw_not_found() {
        BookService service = Mockito.spy(new BookService(br));
        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        doReturn(restTemplate).when(service).createRestTemplate();

        when(restTemplate.getForObject(Mockito.anyString(), Mockito.eq(Map.class))).thenReturn(Map.of());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.fetchPreviewByISBN("9780306406157"));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exception.getReason()).isEqualTo("Geen boek gevonden voor dit ISBN");
    }

    @Test
    void given_response_without_matching_isbn_when_fetch_preview_by_isbn_then_throw_not_found() {
        BookService service = Mockito.spy(new BookService(br));
        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        doReturn(restTemplate).when(service).createRestTemplate();

        when(restTemplate.getForObject(Mockito.anyString(), Mockito.eq(Map.class)))
                .thenReturn(Map.of("ISBN:1111111111", Map.of("title", "other")));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.fetchPreviewByISBN("9780306406157"));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exception.getReason()).isEqualTo("Geen boek gevonden voor dit ISBN");
    }

    @Test
    void given_relative_details_and_preview_url_when_fetch_preview_by_isbn_then_return_available_preview() {
        BookService service = Mockito.spy(new BookService(br));
        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        doReturn(restTemplate).when(service).createRestTemplate();

        Map<String, Object> response = Map.of(
                "ISBN:9780306406157", Map.of(
                        "url", "/books/OL1M",
                        "preview_url", "https://archive.org/details/samplebook"));

        when(restTemplate.getForObject(Mockito.anyString(), Mockito.eq(Map.class))).thenReturn(response);

        BookPreviewDTO result = service.fetchPreviewByISBN("978-0-306-40615-7");

        assertThat(result.isPreviewAvailable()).isTrue();
        assertThat(result.getPreviewUrl()).isEqualTo("https://archive.org/details/samplebook");
        assertThat(result.getDetailsUrl()).isEqualTo("https://openlibrary.org/books/OL1M");
        assertThat(result.getEmbedUrl()).isEqualTo("https://archive.org/embed/samplebook");
        assertThat(result.getMessage()).isEqualTo("Preview beschikbaar");
    }

    @Test
    void given_only_ebook_preview_url_when_fetch_preview_by_isbn_then_use_ebook_preview() {
        BookService service = Mockito.spy(new BookService(br));
        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        doReturn(restTemplate).when(service).createRestTemplate();

        Map<String, Object> response = Map.of(
                "ISBN:123456789X", Map.of(
                        "url", "https://openlibrary.org/books/OL2M",
                        "ebooks", List.of(Map.of("preview_url", "https://archive.org/details/ebook-preview"))));

        when(restTemplate.getForObject(Mockito.anyString(), Mockito.eq(Map.class))).thenReturn(response);

        BookPreviewDTO result = service.fetchPreviewByISBN("123456789x");

        assertThat(result.isPreviewAvailable()).isTrue();
        assertThat(result.getPreviewUrl()).isEqualTo("https://archive.org/details/ebook-preview");
        assertThat(result.getEmbedUrl()).isEqualTo("https://archive.org/embed/ebook-preview");
        assertThat(result.getMessage()).isEqualTo("Preview beschikbaar");
    }

    @Test
    void given_invalid_details_host_when_fetch_preview_by_isbn_then_fall_back_to_safe_openlibrary_url() {
        BookService service = Mockito.spy(new BookService(br));
        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        doReturn(restTemplate).when(service).createRestTemplate();

        Map<String, Object> response = Map.of(
                "ISBN:9780306406157", Map.of(
                        "url", "https://evil.example.com/steal",
                        "preview_url", "http://archive.org/details/not-https"));

        when(restTemplate.getForObject(Mockito.anyString(), Mockito.eq(Map.class))).thenReturn(response);

        BookPreviewDTO result = service.fetchPreviewByISBN("9780306406157");

        assertThat(result.isPreviewAvailable()).isFalse();
        assertThat(result.getPreviewUrl()).isNull();
        assertThat(result.getEmbedUrl()).isNull();
        assertThat(result.getDetailsUrl()).isEqualTo("https://openlibrary.org/isbn/9780306406157");
        assertThat(result.getMessage()).isEqualTo("Geen preview beschikbaar");
    }

    @Test
    void given_ocaid_in_identifiers_when_fetch_preview_by_isbn_then_build_embed_url() {
        BookService service = Mockito.spy(new BookService(br));
        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        doReturn(restTemplate).when(service).createRestTemplate();

        Map<String, Object> response = Map.of(
                "ISBN:9780306406157", Map.of(
                        "url", "https://openlibrary.org/books/OL3M",
                        "identifiers", Map.of("ocaid", List.of("my_archive_id"))));

        when(restTemplate.getForObject(Mockito.anyString(), Mockito.eq(Map.class))).thenReturn(response);

        BookPreviewDTO result = service.fetchPreviewByISBN("9780306406157");

        assertThat(result.isPreviewAvailable()).isTrue();
        assertThat(result.getPreviewUrl()).isNull();
        assertThat(result.getEmbedUrl()).isEqualTo("https://archive.org/embed/my_archive_id");
        assertThat(result.getMessage()).isEqualTo("Preview beschikbaar");
    }
}
