package edu.ap.bib_backend;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import edu.ap.bib_backend.controller.BookController;
import edu.ap.bib_backend.dto.BookDTO;
import edu.ap.bib_backend.jpa.entity.Availability;
import edu.ap.bib_backend.service.BookService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookController.class)
public class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookService bs;

    @Test
    void getAllBooks_returnJSONList() throws Exception {
        BookDTO book = new BookDTO();
        long ISBN = 1672587;
        book.setId(1L);
        book.setTitle("Test Book");
        book.setAuthor("Test Author");
        book.setISBN(ISBN);
        book.setPages(324);
        book.setImage_url("https://books.google.com/books/content?id=7p9mEAAAQBAJ&printsec=frontcover&img=1&zoom=1");
        book.setAvailability(Availability.AVAILABLE);
        book.setCopies(3);
        book.setDescription("dede de d e d ed e d e d e de d e d");
        book.setRelease_year(2017);

        Mockito.when(bs.getAllBooks()).thenReturn(List.of(book));

        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Test Book"))
                .andExpect(jsonPath("$[0].author").value("Test Author"))
                .andExpect(jsonPath("$[0].isbn").value(1672587))
                .andExpect(jsonPath("$[0].pages").value(324))
                .andExpect(jsonPath("$[0].image_url").value(
                        "https://books.google.com/books/content?id=7p9mEAAAQBAJ&printsec=frontcover&img=1&zoom=1"))
                .andExpect(jsonPath("$[0].availability").value("AVAILABLE"))
                .andExpect(jsonPath("$[0].copies").value(3))
                .andExpect(jsonPath("$[0].description").value("dede de d e d ed e d e d e de d e d"))
                .andExpect(jsonPath("$[0].release_year").value(2017));
    }
    @Test
void getBookById_returnsJSONBook() throws Exception {
    // Arrange
    BookDTO book = new BookDTO();
    book.setId(1L);
    book.setTitle("Specifiek Boek");
    book.setAuthor("Bekende Auteur");
    book.setISBN(987654321L);
    book.setPages(250);
    book.setImage_url("https://example.com/image.jpg");
    book.setAvailability(Availability.AVAILABLE);
    book.setCopies(5);
    book.setRelease_year(2021);

    // Mock de service: als getBookById(1) wordt aangeroepen, geef de DTO terug
    Mockito.when(bs.getBookById(1L)).thenReturn(book);

    // Act & Assert
    mockMvc.perform(get("/api/books/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.title").value("Specifiek Boek"))
            .andExpect(jsonPath("$.author").value("Bekende Auteur"))
            .andExpect(jsonPath("$.availability").value("AVAILABLE"))
            .andExpect(jsonPath("$.release_year").value(2021));
}
}
