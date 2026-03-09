package edu.ap.bib_backend.service;

import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;

import edu.ap.bib_backend.dto.BookDTO;
import edu.ap.bib_backend.jpa.entity.Availability;
import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.repository.BookRepository;
import edu.ap.bib_backend.service.BookService;

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
    void getBookById_returnsMappedDTO() {
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
    void createBookWithoutISBN_savesAndReturnsDTO() {
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
}
