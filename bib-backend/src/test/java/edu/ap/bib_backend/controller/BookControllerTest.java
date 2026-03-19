package edu.ap.bib_backend.controller;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.ap.bib_backend.TestSecurityConfig;
import edu.ap.bib_backend.dto.BookDTO;
import edu.ap.bib_backend.dto.BookPreviewDTO;
import edu.ap.bib_backend.exception.GlobalExceptionHandler;
import edu.ap.bib_backend.jpa.entity.Availability;
import edu.ap.bib_backend.service.BookService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@WebMvcTest(BookController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
public class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookService bs;

    @Test
    void getAllBooks_returnJSONList() throws Exception {
        BookDTO book = new BookDTO();
        book.setId(1L);
        book.setTitle("Test Book");
        book.setAuthor("Test Author");
        book.setISBN("1672587");
        book.setPages(324);
        book.setImageUrl("https://books.google.com/books/content?id=7p9mEAAAQBAJ&printsec=frontcover&img=1&zoom=1");
        book.setDescription("dede de d e d ed e d e d e de d e d");
        book.setReleaseYear(2017);

        Mockito.when(bs.getAllBooks()).thenReturn(List.of(book));

        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Test Book"))
                .andExpect(jsonPath("$[0].author").value("Test Author"))
                .andExpect(jsonPath("$[0].isbn").value(1672587))
                .andExpect(jsonPath("$[0].pages").value(324))
                .andExpect(jsonPath("$[0].imageUrl").value(
                        "https://books.google.com/books/content?id=7p9mEAAAQBAJ&printsec=frontcover&img=1&zoom=1"))
                .andExpect(jsonPath("$[0].availability").value("AVAILABLE"))
                .andExpect(jsonPath("$[0].copies").value(3))
                .andExpect(jsonPath("$[0].description").value("dede de d e d ed e d e d e de d e d"))
                .andExpect(jsonPath("$[0].releaseYear").value(2017));
    }

    @Test
    void getBookById_returnsJSONBook() throws Exception {

        BookDTO book = new BookDTO();
        book.setId(1L);
        book.setTitle("Specifiek Boek");
        book.setAuthor("Bekende Auteur");
        book.setISBN("987654321");
        book.setPages(250);
        book.setImageUrl("https://example.com/image.jpg");
        book.setReleaseYear(2021);

        Mockito.when(bs.getBookById(1L)).thenReturn(book);

        mockMvc.perform(get("/api/books/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Specifiek Boek"))
                .andExpect(jsonPath("$.author").value("Bekende Auteur"))
                .andExpect(jsonPath("$.isbn").value(987654321))
                .andExpect(jsonPath("$.pages").value(250))
                .andExpect(jsonPath("$.imageUrl").value("https://example.com/image.jpg"))
                .andExpect(jsonPath("$.availability").value("AVAILABLE"))
                .andExpect(jsonPath("$.copies").value(5))
                .andExpect(jsonPath("$.releaseYear").value(2021));
    }

    @Test
    void addBookWithoutISBN_returnsSavedBook() throws Exception {
        // Arrange
        BookDTO requestDTO = new BookDTO();
        requestDTO.setTitle("Nieuwe Boek");
        requestDTO.setAuthor("Auteur Test");
        requestDTO.setPages(300);
        requestDTO.setImageUrl("https://example.com/image.jpg");
        requestDTO.setDescription("Beschrijving hier");
        requestDTO.setReleaseYear(2024);

        BookDTO savedDTO = new BookDTO();
        savedDTO.setId(1L);
        savedDTO.setTitle("Nieuwe Boek");
        savedDTO.setAuthor("Auteur Test");
        savedDTO.setPages(300);
        savedDTO.setImageUrl("https://example.com/image.jpg");
        savedDTO.setDescription("Beschrijving hier");
        savedDTO.setReleaseYear(2024);

        Mockito.when(bs.addBook(Mockito.any(BookDTO.class))).thenReturn(savedDTO);

        // Act & Assert
        mockMvc.perform(post("/api/books")
                .contentType("application/json")
                .content(new ObjectMapper().writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Nieuwe Boek"))
                .andExpect(jsonPath("$.author").value("Auteur Test"))
                .andExpect(jsonPath("$.pages").value(300))
                .andExpect(jsonPath("$.availability").value("AVAILABLE"))
                .andExpect(jsonPath("$.copies").value(2))
                .andExpect(jsonPath("$.description").value("Beschrijving hier"))
                .andExpect(jsonPath("$.releaseYear").value(2024))
                .andExpect(jsonPath("$.imageUrl").value("https://example.com/image.jpg"));
    }

    // ==================== getMostReadBooks Tests ====================

    @Test
    void givenDefaultLimit_whenGetMostReadBooks_thenReturnThreeBooks() throws Exception {
        // Arrange
        BookDTO book1 = new BookDTO();
        book1.setId(1L);
        book1.setTitle("Most Read Book 1");
        book1.setAuthor("Author A");

        BookDTO book2 = new BookDTO();
        book2.setId(2L);
        book2.setTitle("Most Read Book 2");
        book2.setAuthor("Author B");

        BookDTO book3 = new BookDTO();
        book3.setId(3L);
        book3.setTitle("Most Read Book 3");
        book3.setAuthor("Author C");

        Mockito.when(bs.getMostReadBooks(3)).thenReturn(List.of(book1, book2, book3));

        // Act & Assert
        mockMvc.perform(get("/api/books/most-read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Most Read Book 1"))
                .andExpect(jsonPath("$[1].title").value("Most Read Book 2"))
                .andExpect(jsonPath("$[2].title").value("Most Read Book 3"));
    }

    @Test
    void givenCustomLimit_whenGetMostReadBooks_thenReturnRequestedNumberOfBooks() throws Exception {
        // Arrange
        BookDTO book1 = new BookDTO();
        book1.setId(1L);
        book1.setTitle("Popular Book");

        Mockito.when(bs.getMostReadBooks(5)).thenReturn(List.of(book1));

        // Act & Assert
        mockMvc.perform(get("/api/books/most-read")
                .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Popular Book"))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").hasJsonPath());
    }

    // ==================== getNewestBooks Tests ====================

    @Test
    void givenDefaultLimit_whenGetNewestBooks_thenReturnThreeBooks() throws Exception {
        // Arrange
        BookDTO book1 = new BookDTO();
        book1.setId(1L);
        book1.setTitle("Newest Book 1");
        book1.setAuthor("Author A");
        book1.setReleaseYear(2024);

        BookDTO book2 = new BookDTO();
        book2.setId(2L);
        book2.setTitle("Newest Book 2");
        book2.setAuthor("Author B");
        book2.setReleaseYear(2023);

        BookDTO book3 = new BookDTO();
        book3.setId(3L);
        book3.setTitle("Newest Book 3");
        book3.setAuthor("Author C");
        book3.setReleaseYear(2022);

        Mockito.when(bs.getNewestBooks(3)).thenReturn(List.of(book1, book2, book3));

        // Act & Assert
        mockMvc.perform(get("/api/books/newest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Newest Book 1"))
                .andExpect(jsonPath("$[1].title").value("Newest Book 2"))
                .andExpect(jsonPath("$[2].title").value("Newest Book 3"));
    }

    @Test
    void givenCustomLimit_whenGetNewestBooks_thenReturnRequestedNumberOfBooks() throws Exception {
        // Arrange
        BookDTO book1 = new BookDTO();
        book1.setId(1L);
        book1.setTitle("Recently Added Book");

        Mockito.when(bs.getNewestBooks(10)).thenReturn(List.of(book1));

        // Act & Assert
        mockMvc.perform(get("/api/books/newest")
                .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Recently Added Book"))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").hasJsonPath());
    }

    @Test
    void getBookByISBN_returnsJSONBook() throws Exception {
        BookDTO book = new BookDTO();
        book.setId(1L);
        book.setTitle("ISBN Boek");
        book.setAuthor("Auteur X");
        book.setISBN("9781234567890");
        book.setPages(280);
        book.setImageUrl("https://example.com/book.jpg");
        book.setDescription("Beschrijving van het boek");
        book.setReleaseYear(2022);

        Mockito.when(bs.fetchBookByISBN("9781234567890")).thenReturn(book);

        mockMvc.perform(get("/api/books/isbn/9781234567890"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("ISBN Boek"))
                .andExpect(jsonPath("$.author").value("Auteur X"))
                .andExpect(jsonPath("$.isbn").value("9781234567890"))
                .andExpect(jsonPath("$.pages").value(280))
                .andExpect(jsonPath("$.imageUrl").value("https://example.com/book.jpg"))
                .andExpect(jsonPath("$.description").value("Beschrijving van het boek"))
                .andExpect(jsonPath("$.releaseYear").value(2022));

        Mockito.verify(bs).fetchBookByISBN("9781234567890");
    }

    @Test
    void getYearRange_returnsMinAndMaxYear() throws Exception {
        Mockito.when(bs.getMinReleaseYear()).thenReturn(1950);
        Mockito.when(bs.getMaxReleaseYear()).thenReturn(2024);

        mockMvc.perform(get("/api/books/years/range"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.min").value(1950))
                .andExpect(jsonPath("$.max").value(2024));

        Mockito.verify(bs).getMinReleaseYear();
        Mockito.verify(bs).getMaxReleaseYear();
    }

    @Test
    void getMaxPages_returnsMaxPages() throws Exception {
        Mockito.when(bs.getMaxPages()).thenReturn(1200);

        mockMvc.perform(get("/api/books/pages/max"))
                .andExpect(status().isOk())
                .andExpect(content().string("1200"));

        Mockito.verify(bs).getMaxPages();
    }
}
