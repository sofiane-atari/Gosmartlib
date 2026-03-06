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
        long ISBN = 1672587;
        book.setId(1L);
        book.setTitle("Test Book");
        book.setAuthor("Test Author");
        book.setISBN(ISBN);
        book.setPages(324);
        book.setImage_url("https://books.google.com/books/content?id=7p9mEAAAQBAJ&printsec=frontcover&img=1&zoom=1");
        // Extra velden toevoegen aan de entiteit
        book.setAvailability(Availability.AVAILABLE);
        book.setCopies(3);
        book.setDescription("dede de d e d ed e d e d e de d e d");
        book.setRelease_year(2017);

        when(br.findAll()).thenReturn(List.of(book));

        List<BookDTO> res = bs.getAllBooks();

        assertThat(res).hasSize(1);
        assertThat(res.get(0).getTitle()).isEqualTo("Test Book");
        assertThat(res.get(0).getAuthor()).isEqualTo("Test Author");
        assertThat(res.get(0).getISBN()).isEqualTo(1672587);
        assertThat(res.get(0).getPages()).isEqualTo(324);
        assertThat(res.get(0).getImage_url())
                .isEqualTo("https://books.google.com/books/content?id=7p9mEAAAQBAJ&printsec=frontcover&img=1&zoom=1");

        // Assertions voor de nieuwe velden
        assertThat(res.get(0).getAvailability()).isEqualTo(Availability.AVAILABLE);
        assertThat(res.get(0).getCopies()).isEqualTo(3);
        assertThat(res.get(0).getDescription()).isEqualTo("dede de d e d ed e d e d e de d e d");
        assertThat(res.get(0).getRelease_year()).isEqualTo(2017);
    }

    @Test
    void getBookById_returnsMappedDTO() {
        // Arrange
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Specifiek Boek");
        book.setAuthor("Bekende Auteur");
        book.setISBN(987654321L);
        book.setPages(250);
        book.setImage_url("https://example.com/image.jpg");
        book.setAvailability(Availability.AVAILABLE);
        book.setCopies(5);
        book.setDescription("Beschrijving van het specifieke boek.");
        book.setRelease_year(2021);

        // Mock de repository: als er gezocht wordt op ID 1, geef het boek terug
        when(br.findById(1L)).thenReturn(Optional.of(book));

        // Act
        BookDTO res = bs.getBookById(1L);

        // Assert
        assertThat(res).isNotNull();
        assertThat(res.getId()).isEqualTo(1L);
        assertThat(res.getTitle()).isEqualTo("Specifiek Boek");
        assertThat(res.getAvailability()).isEqualTo(Availability.AVAILABLE);
        assertThat(res.getRelease_year()).isEqualTo(2021);
    }

    @Test
    void createBookWithoutISBN_savesAndReturnsDTO() {
        // Arrange
        BookDTO dto = new BookDTO();
        dto.setTitle("Nieuwe Boek");
        dto.setAuthor("Auteur Test");
        dto.setPages(300);
        dto.setImage_url("https://example.com/image.jpg");
        dto.setAvailability(Availability.AVAILABLE);
        dto.setCopies(2);
        dto.setDescription("Beschrijving hier");
        dto.setRelease_year(2024);

        // Mock the repository save method
        Book savedBook = new Book();
        savedBook.setId(1L); // Simulate DB-generated ID
        savedBook.setTitle(dto.getTitle());
        savedBook.setAuthor(dto.getAuthor());
        savedBook.setPages(dto.getPages());
        savedBook.setImage_url(dto.getImage_url());
        savedBook.setAvailability(dto.getAvailability());
        savedBook.setCopies(dto.getCopies());
        savedBook.setDescription(dto.getDescription());
        savedBook.setRelease_year(dto.getRelease_year());

        Mockito.when(br.save(Mockito.any(Book.class))).thenReturn(savedBook);

        // Act
        BookDTO result = bs.createBookWithoutISBN(dto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Nieuwe Boek");
        assertThat(result.getAuthor()).isEqualTo("Auteur Test");
        assertThat(result.getPages()).isEqualTo(300);
        assertThat(result.getAvailability()).isEqualTo(Availability.AVAILABLE);
        assertThat(result.getCopies()).isEqualTo(2);
        assertThat(result.getDescription()).isEqualTo("Beschrijving hier");
        assertThat(result.getRelease_year()).isEqualTo(2024);
    }
}
