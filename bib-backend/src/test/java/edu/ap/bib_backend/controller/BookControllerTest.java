package edu.ap.bib_backend.controller;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.ap.bib_backend.TestSecurityConfig;
import edu.ap.bib_backend.dto.BookCandidateDTO;
import edu.ap.bib_backend.dto.BookDTO;
import edu.ap.bib_backend.dto.BookPreviewDTO;
import edu.ap.bib_backend.jpa.enums.BookAvailabilityFilter;
import edu.ap.bib_backend.jpa.enums.BookSortOption;
import edu.ap.bib_backend.service.BookService;
import edu.ap.bib_backend.service.JwtService;
import edu.ap.bib_backend.service.SmartschoolOAuth2UserService;
import edu.ap.bib_backend.service.TokenStorageService;

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

    @MockBean
    private JwtService jwtService;

    @MockBean
    private TokenStorageService tokenStorageService;

    @MockBean
    private SmartschoolOAuth2UserService smartschoolOAuth2UserService;
    //



    @Test
    void givenFilters_whenGetFilteredBooks_thenReturnsPagedResult() throws Exception {
        // Arrange
        BookDTO book = new BookDTO();
        book.setId(1L);
        book.setTitle("Harry Potter");
        book.setAuthor("J.K. Rowling");
        book.setPages(350);

        Page<BookDTO> page = new org.springframework.data.domain.PageImpl<>(List.of(book));

        Mockito.when(bs.getFilteredBooks(
                "Harry",
                BookAvailabilityFilter.ALL,
                100,
                500,
                0,
                10,
                BookSortOption.AZ
        )).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/books/catalog")
                .param("searchTerm", "Harry")
                .param("availability", "ALL")
                .param("minPages", "100")
                .param("maxPages", "500")
                .param("sortBy", "AZ")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                // FIXED: Changed $.content to $.data
                .andExpect(jsonPath("$.data[0].title").value("Harry Potter"))
                .andExpect(jsonPath("$.data[0].author").value("J.K. Rowling"))
                .andExpect(jsonPath("$.data[0].pages").value(350))
                .andExpect(jsonPath("$.meta").exists());

        Mockito.verify(bs).getFilteredBooks(
                "Harry",
                BookAvailabilityFilter.ALL,
                100,
                500,
                0,
                10,
                BookSortOption.AZ
        );
    }

    @Test
    void givenNoFilters_whenGetFilteredBooks_thenReturnsPagedResultWithDefaultParams() throws Exception {
        // Arrange
        Page<BookDTO> page = new org.springframework.data.domain.PageImpl<>(List.of());

        Mockito.when(bs.getFilteredBooks(
                null,
                null,
                null,
                null,
                0,
                10,
                null
        )).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/books/catalog"))
                .andExpect(status().isOk())
                // FIXED: Changed $.content to $.data
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.meta").exists());

        Mockito.verify(bs).getFilteredBooks(
                null,
                null,
                null,
                null,
                0,
                10,
                null
        );
    }


    @Test
void givenBookIdAndLimit_whenGetRelatedBooksCarousel_thenReturnsListOfRelatedBooks() throws Exception {
    // Arrange
    BookDTO related1 = new BookDTO();
    related1.setId(2L);
    related1.setTitle("Related Book 1");

    BookDTO related2 = new BookDTO();
    related2.setId(3L);
    related2.setTitle("Related Book 2");

    Mockito.when(bs.getRelatedBooksCarousel(1L, 5))
            .thenReturn(List.of(related1, related2));

    // Act & Assert
    mockMvc.perform(get("/books/1/related-carousel")
            .param("limit", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].title").value("Related Book 1"))
            .andExpect(jsonPath("$[1].title").value("Related Book 2"));

    Mockito.verify(bs).getRelatedBooksCarousel(1L, 5);
}

@Test
void givenBookId_whenGetRelatedBooksCarousel_thenReturnsListWithDefaultLimit() throws Exception {
    // Arrange
    BookDTO related = new BookDTO();
    related.setId(2L);
    related.setTitle("Default Related Book");

    Mockito.when(bs.getRelatedBooksCarousel(1L, 10))
            .thenReturn(List.of(related));

    // Act & Assert
    mockMvc.perform(get("/books/1/related-carousel"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].title").value("Default Related Book"));

    Mockito.verify(bs).getRelatedBooksCarousel(1L, 10);
}


@Test
void givenMinPagesExists_whenGetMinPages_thenReturnsMinPages() throws Exception {
    // Arrange
    Mockito.when(bs.getMinPages()).thenReturn(42);

    // Act & Assert
    mockMvc.perform(get("/books/pages/min"))
            .andExpect(status().isOk())
            .andExpect(content().string("42"));

    Mockito.verify(bs).getMinPages();
}

@Test
void givenSearchParameters_whenSearchBookCandidates_thenReturnsCandidateList() throws Exception {
    // Arrange
    BookCandidateDTO candidate = new BookCandidateDTO();
    candidate.setTitle("Candidate Book");
    candidate.setAuthor("Candidate Author");
    candidate.setReleaseYear(2020);

    Mockito.when(bs.searchBookCandidates("Dune", "Frank Herbert", 1965))
            .thenReturn(List.of(candidate));

    // Act & Assert
    mockMvc.perform(get("/books/search")
            .param("title", "Dune")
            .param("author", "Frank Herbert")
            .param("releaseYear", "1965"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].title").value("Candidate Book"))
            .andExpect(jsonPath("$[0].author").value("Candidate Author"))
            .andExpect(jsonPath("$[0].releaseYear").value(2020));

    Mockito.verify(bs).searchBookCandidates("Dune", "Frank Herbert", 1965);
}

@Test
void givenIsbn13_whenSelectBook_thenReturnsBookByIsbn13() throws Exception {
    // Arrange
    BookDTO book = new BookDTO();
    book.setId(9L);
    book.setTitle("Selected Book");
    book.setAuthor("Selected Author");
    book.setISBN("9789999999999");

    Mockito.when(bs.fetchBookByISBN("9789999999999")).thenReturn(book);

    // Act & Assert
    mockMvc.perform(post("/books/select")
            .param("isbn13", "9789999999999"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(9))
            .andExpect(jsonPath("$.title").value("Selected Book"))
            .andExpect(jsonPath("$.author").value("Selected Author"))
            .andExpect(jsonPath("$.isbn").value("9789999999999"));

    Mockito.verify(bs).fetchBookByISBN("9789999999999");
}

@Test
void givenMissingReleaseYear_whenSearchBookCandidates_thenReturnsBadRequest() throws Exception {
    // Arrange

    // Act & Assert
    mockMvc.perform(get("/books/search")
            .param("title", "Dune")
            .param("author", "Frank Herbert"))
            .andExpect(status().isBadRequest());
}

@Test
void givenMissingIsbn13_whenSelectBook_thenReturnsBadRequest() throws Exception {
    // Arrange

    // Act & Assert
    mockMvc.perform(post("/books/select"))
            .andExpect(status().isBadRequest());
}


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

        mockMvc.perform(get("/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Test Book"))
                .andExpect(jsonPath("$[0].author").value("Test Author"))
                .andExpect(jsonPath("$[0].isbn").value(1672587))
                .andExpect(jsonPath("$[0].pages").value(324))
                .andExpect(jsonPath("$[0].imageUrl").value(
                        "https://books.google.com/books/content?id=7p9mEAAAQBAJ&printsec=frontcover&img=1&zoom=1"))
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

        mockMvc.perform(get("/books/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Specifiek Boek"))
                .andExpect(jsonPath("$.author").value("Bekende Auteur"))
                .andExpect(jsonPath("$.isbn").value(987654321))
                .andExpect(jsonPath("$.pages").value(250))
                .andExpect(jsonPath("$.imageUrl").value("https://example.com/image.jpg"))
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
        mockMvc.perform(post("/books")
                .contentType("application/json")
                .content(new ObjectMapper().writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Nieuwe Boek"))
                .andExpect(jsonPath("$.author").value("Auteur Test"))
                .andExpect(jsonPath("$.pages").value(300))
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
        mockMvc.perform(get("/books/most-read"))
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
        mockMvc.perform(get("/books/most-read")
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
        mockMvc.perform(get("/books/newest"))
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
        mockMvc.perform(get("/books/newest")
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

        mockMvc.perform(get("/books/isbn/9781234567890"))
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
    void getMaxPages_returnsMaxPages() throws Exception {
        Mockito.when(bs.getMaxPages()).thenReturn(1200);

        mockMvc.perform(get("/books/pages/max"))
                .andExpect(status().isOk())
                .andExpect(content().string("1200"));

        Mockito.verify(bs).getMaxPages();
    }
}
