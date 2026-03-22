package edu.ap.bib_backend.service;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.doThrow;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.server.ResponseStatusException;

import edu.ap.bib_backend.dto.BookDTO;
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
    void getAllBooks_returnMappedDTOs() {

        Book book = new Book();
        book.setId(1L);
        book.setTitle("Test Book");
        book.setAuthor("Test Author");
        book.setISBN("1672587");
        book.setPages(324);
        book.setImageUrl("https://books.google.com/books/content?id=7p9mEAAAQBAJ&printsec=frontcover&img=1&zoom=1");
        // Extra velden toevoegen aan de entiteit
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
        assertThat(res.get(0).getDescription()).isEqualTo("dede de d e d ed e d e d e de d e d");
        assertThat(res.get(0).getReleaseYear()).isEqualTo(2017);
        assertThat(res.get(0).getReviewScore()).isEqualTo(4.5);
    }

    @Test
    void getBookById_returnsMappedDTO() {
        // Arrange
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Specifiek Boek");
        book.setAuthor("Bekende Auteur");
        book.setISBN("987654321L");
        book.setPages(250);
        book.setImageUrl("https://example.com/image.jpg");
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
        assertThat(res.getReleaseYear()).isEqualTo(2021);
        assertThat(res.getReviewScore()).isEqualTo(3.8);
    }

    @Test
    void createBookWithoutISBN_savesAndReturnsDTO() {
        // Arrange
        BookDTO dto = new BookDTO();
        dto.setTitle("Nieuwe Boek");
        dto.setAuthor("Auteur Test");
        dto.setPages(300);
        dto.setImageUrl("https://example.com/image.jpg");
        dto.setDescription("Beschrijving hier");
        dto.setReleaseYear(2024);

        // Mock the repository save method
        Book savedBook = new Book();
        savedBook.setId(1L); // Simulate DB-generated ID
        savedBook.setTitle(dto.getTitle());
        savedBook.setAuthor(dto.getAuthor());
        savedBook.setPages(dto.getPages());
        savedBook.setImageUrl(dto.getImageUrl());
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
        assertThat(result.getDescription()).isEqualTo("Beschrijving hier");
        assertThat(result.getReleaseYear()).isEqualTo(2024);
        assertThat(result.getImageUrl()).isEqualTo("https://example.com/image.jpg");
        assertThat(result.getReviewScore()).isEqualTo(0.0);
    }

    // ==================== getNewestBooks Tests ====================

    @Test
    void givenBooksExist_whenGetNewestBooks_thenReturnNewestBooksOrderedByAddedDate() {
        // Arrange
        int limit = 3;

        Book newestBook = new Book();
        newestBook.setId(3L);
        newestBook.setTitle("Newest Book");
        newestBook.setAuthor("Author C");
        newestBook.setISBN("333");
        newestBook.setPages(200);
        newestBook.setReleaseYear(2024);

        Book middleBook = new Book();
        middleBook.setId(2L);
        middleBook.setTitle("Middle Book");
        middleBook.setAuthor("Author B");
        middleBook.setISBN("222");
        middleBook.setPages(250);
        middleBook.setReleaseYear(2023);

        Book oldestBook = new Book();
        oldestBook.setId(1L);
        oldestBook.setTitle("Oldest Book");
        oldestBook.setAuthor("Author A");
        oldestBook.setISBN("111");
        oldestBook.setPages(300);
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
    void givenDefaultLimit_whenGetNewestBooks_thenReturnThreeBooks() {
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
    void givenBooksExist_whenGetMostReadBooks_thenReturnMostReadBooks() {
        // Arrange
        int limit = 3;

        Book mostReadBook = new Book();
        mostReadBook.setId(1L);
        mostReadBook.setTitle("Most Read Book");
        mostReadBook.setAuthor("Popular Author");
        mostReadBook.setISBN("111");
        mostReadBook.setPages(400);
        mostReadBook.setReleaseYear(2020);

        Book secondBook = new Book();
        secondBook.setId(2L);
        secondBook.setTitle("Second Most Read");
        secondBook.setAuthor("Author B");
        secondBook.setISBN("222");
        secondBook.setPages(300);
        secondBook.setReleaseYear(2021);

        Book thirdBook = new Book();
        thirdBook.setId(3L);
        thirdBook.setTitle("Third Most Read");
        thirdBook.setAuthor("Author C");
        thirdBook.setISBN("333");
        thirdBook.setPages(250);
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
    void givenNoBooksExist_whenGetMostReadBooks_thenThrowException() {
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
    void givenCustomLimit_whenGetMostReadBooks_thenReturnRequestedNumberOfBooks() {
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
    void givenRelatedBooksExist_whenGetRelatedBooksCarousel_thenReturnMappedBooks() {
        Book source = new Book();
        source.setId(1L);

        Book related = new Book();
        related.setId(2L);
        related.setTitle("Related");
        related.setAuthor("Author");

        when(br.findById(1L)).thenReturn(Optional.of(source));
        when(br.findRelatedBooksWithScore(1L, PageRequest.of(0, 10))).thenReturn((List<Object[]>) List.of(new Object[] { related, 2L }));

        List<BookDTO> result = bs.getRelatedBooksCarousel(1L, 10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(2L);
        assertThat(result.get(0).getTitle()).isEqualTo("Related");
    }
}
