package edu.ap.bib_backend.controller;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.ap.bib_backend.TestSecurityConfig;
import edu.ap.bib_backend.dto.BookCandidateDTO;
import edu.ap.bib_backend.dto.BookDTO;
import edu.ap.bib_backend.jpa.enums.BookAvailabilityFilter;
import edu.ap.bib_backend.jpa.enums.BookSortOption;
import edu.ap.bib_backend.service.AdminOverrideService;
import edu.ap.bib_backend.service.JwtService;
import edu.ap.bib_backend.service.TokenStorageService;
import edu.ap.bib_backend.service.book.BookSearchService;
import edu.ap.bib_backend.service.book.BookService;
import edu.ap.bib_backend.service.book.GoogleBooksClient;
import edu.ap.bib_backend.service.book.OpenLibraryClient;
import edu.ap.bib_backend.service.lessontips.LessonTipService;
import edu.ap.bib_backend.service.smartschool.SmartschoolOAuth2UserService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;

@WebMvcTest(BookController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
public class BookControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private BookService bs;

        @MockitoBean
        private BookSearchService bookSearchService;

        @MockitoBean
        private JwtService jwtService;

        @MockitoBean
        private TokenStorageService tokenStorageService;

        @MockitoBean
        private SmartschoolOAuth2UserService smartschoolOAuth2UserService;

        @MockitoBean
        private AdminOverrideService adminOverrideService;

        @MockitoBean
        private LessonTipService lessonTipService;

        @MockitoBean
        private GoogleBooksClient gbc;

        @MockitoBean
        private OpenLibraryClient olc;

        @Mock
        private Authentication authentication;

        @Test
        void givenFilters_whenGetFilteredBooks_thenReturnsPagedResult() throws Exception {
                // Arrange
                BookDTO book = new BookDTO();
                book.setId(1L);
                book.setTitle("Harry Potter");
                book.setAuthor("J.K. Rowling");
                book.setPages(350);

                Page<BookDTO> page = new PageImpl<>(List.of(book));

                when(bookSearchService.getFilteredBooks(
                                "Harry",
                                BookAvailabilityFilter.ALL,
                                100,
                                500,
                                null,
                                null,
                                0,
                                10,
                                BookSortOption.AZ, null)).thenReturn(page);

                // Act
                mockMvc.perform(get("/books/catalog")
                                .param("searchTerm", "Harry")
                                .param("availability", "ALL")
                                .param("minPages", "100")
                                .param("maxPages", "500")
                                .param("sortBy", "AZ")
                                .param("page", "0")
                                .param("size", "10"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data[0].title").value("Harry Potter"))
                                .andExpect(jsonPath("$.data[0].author").value("J.K. Rowling"))
                                .andExpect(jsonPath("$.data[0].pages").value(350))
                                .andExpect(jsonPath("$.meta").exists());

                // Assert
                verify(bookSearchService).getFilteredBooks(
                                "Harry",
                                BookAvailabilityFilter.ALL,
                                100,
                                500,
                                null,
                                null,
                                0,
                                10,
                                BookSortOption.AZ, null);
        }

        @Test
        void givenExistingBookId_whenDeleteBook_thenReturn204() throws Exception {
                // Arrange
                Long bookId = 1L;
                Mockito.doNothing().when(bs).deleteBook(Mockito.eq(bookId), Mockito.nullable(Authentication.class));

                // Act
                var result = mockMvc.perform(delete("/books/{id}", bookId));

                // Assert
                result.andExpect(status().isNoContent());

                Mockito.verify(bs, Mockito.times(1)).deleteBook(Mockito.eq(bookId), Mockito.nullable(Authentication.class));
        }

        @Test
        void givenNonExistingBookId_whenDeleteBook_thenReturn404() throws Exception {
                // Arrange
                Long bookId = 99L;
                Mockito.doThrow(new edu.ap.bib_backend.exception.ResourceNotFoundException("id",
                                "Boek niet gevonden met id: 99"))
                                .when(bs).deleteBook(Mockito.eq(bookId), Mockito.nullable(Authentication.class));

                // Act
                var result = mockMvc.perform(delete("/books/{id}", bookId));

                // Assert
                result.andExpect(status().isNotFound());
                Mockito.verify(bs, Mockito.times(1)).deleteBook(Mockito.eq(bookId), Mockito.nullable(Authentication.class));
        }

        @Test
        void givenBookWithBorrowedCopies_whenDeleteBook_thenReturn400() throws Exception {
                // Arrange
                Long bookId = 2L;
                Mockito.doThrow(new IllegalStateException(
                                "Dit boek kan niet verwijderd worden omdat minstens één exemplaar momenteel is uitgeleend."))
                                .when(bs).deleteBook(Mockito.eq(bookId), Mockito.nullable(Authentication.class));

                // Act
                var result = mockMvc.perform(delete("/books/{id}", bookId));

                // Assert
                result.andExpect(status().isBadRequest())
                                .andExpect(r -> org.junit.jupiter.api.Assertions.assertInstanceOf(
                                                IllegalStateException.class, r.getResolvedException()))
                                .andExpect(r -> org.junit.jupiter.api.Assertions.assertEquals(
                                                "Dit boek kan niet verwijderd worden omdat minstens één exemplaar momenteel is uitgeleend.",
                                                r.getResolvedException() != null ? r.getResolvedException().getMessage() : null));

                // Controleer of de delete methode daadwerkelijk is aangeroepen
                Mockito.verify(bs, Mockito.times(1)).deleteBook(Mockito.eq(bookId), Mockito.nullable(Authentication.class));
        }

        @Test
        void givenNoFilters_whenGetFilteredBooks_thenReturnsPagedResultWithDefaultParams() throws Exception {
            // Arrange
            Page<BookDTO> page = new PageImpl<>(List.of());

            Mockito.when(bookSearchService.getFilteredBooks(
                    Mockito.isNull(),
                    Mockito.isNull(),
                    Mockito.isNull(),
                    Mockito.isNull(),
                    Mockito.isNull(),
                    Mockito.isNull(),
                    Mockito.eq(0),
                    Mockito.eq(10),
                    Mockito.isNull(),
                    Mockito.nullable(Authentication.class)))
                    .thenReturn(page);

            // Act
            var result = mockMvc.perform(get("/books/catalog"));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.meta").exists());

            Mockito.verify(bookSearchService).getFilteredBooks(
                    Mockito.isNull(),
                    Mockito.isNull(),
                    Mockito.isNull(),
                    Mockito.isNull(),
                    Mockito.isNull(),
                    Mockito.isNull(),
                    Mockito.eq(0),
                    Mockito.eq(10),
                    Mockito.isNull(),
                    Mockito.nullable(Authentication.class));
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

            Mockito.when(bs.getRelatedBooksCarousel(
                    Mockito.eq(1L),
                    Mockito.eq(5),
                    Mockito.nullable(Authentication.class)))
                    .thenReturn(List.of(related1, related2));

            // Act
            var result = mockMvc.perform(get("/books/1/related-carousel")
                    .param("limit", "5"));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].title").value("Related Book 1"))
                    .andExpect(jsonPath("$[1].title").value("Related Book 2"));

            Mockito.verify(bs).getRelatedBooksCarousel(
                    Mockito.eq(1L),
                    Mockito.eq(5),
                    Mockito.nullable(Authentication.class));
        }

        @Test
        void givenBookId_whenGetRelatedBooksCarousel_thenReturnsListWithDefaultLimit() throws Exception {
            // Arrange
            BookDTO related = new BookDTO();
            related.setId(2L);
            related.setTitle("Default Related Book");

            Mockito.when(bs.getRelatedBooksCarousel(
                    Mockito.eq(1L),
                    Mockito.eq(10),
                    Mockito.nullable(Authentication.class)))
                    .thenReturn(List.of(related));

            // Act
            var result = mockMvc.perform(get("/books/1/related-carousel"));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].title").value("Default Related Book"));

            Mockito.verify(bs).getRelatedBooksCarousel(
                    Mockito.eq(1L),
                    Mockito.eq(10),
                    Mockito.nullable(Authentication.class));
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
                BookCandidateDTO candidate = new BookCandidateDTO();
                candidate.setTitle("Candidate Book");
                candidate.setAuthor("Candidate Author");
                candidate.setReleaseYear(2020);

                Mockito.when(gbc.searchBookCandidates("Dune", "Frank Herbert", 1965))
                                .thenReturn(List.of(candidate));

                mockMvc.perform(get("/books/search-candidates")
                                .param("title", "Dune")
                                .param("author", "Frank Herbert")
                                .param("releaseYear", "1965"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].title").value("Candidate Book"))
                                .andExpect(jsonPath("$[0].author").value("Candidate Author"))
                                .andExpect(jsonPath("$[0].releaseYear").value(2020));

                Mockito.verify(gbc).searchBookCandidates("Dune", "Frank Herbert", 1965);
        }

        @Test
        void givenIsbn13_whenSelectBook_thenReturnsBookByIsbn13() throws Exception {
                BookDTO book = new BookDTO();
                book.setId(9L);
                book.setTitle("Selected Book");
                book.setAuthor("Selected Author");
                book.setIsbn("9789999999999");

                Mockito.when(bs.getBookByISBN(Mockito.eq("9789999999999"), Mockito.nullable(Authentication.class))).thenReturn(book);

                mockMvc.perform(post("/books/select")
                                .param("isbn13", "9789999999999")
                                .param("campusId", "1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(9))
                                .andExpect(jsonPath("$.title").value("Selected Book"))
                                .andExpect(jsonPath("$.author").value("Selected Author"))
                                .andExpect(jsonPath("$.isbn").value("9789999999999"));

                Mockito.verify(bs).getBookByISBN(Mockito.eq("9789999999999"), Mockito.nullable(Authentication.class));
        }

        @Test
        void givenMissingReleaseYear_whenSearchBooksInDb_thenReturnsOk() throws Exception {
                BookCandidateDTO candidate = new BookCandidateDTO();
                candidate.setTitle("Dune");
                candidate.setAuthor("Frank Herbert");
                candidate.setReleaseYear(1965);

                Mockito.when(bs.searchBooksInDb(Mockito.eq("Dune"), Mockito.eq("Frank Herbert"), Mockito.isNull(), Mockito.nullable(Authentication.class)))
                                .thenReturn(List.of(candidate));

                mockMvc.perform(get("/books/search")
                                .param("title", "Dune")
                                .param("author", "Frank Herbert"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].title").value("Dune"))
                                .andExpect(jsonPath("$[0].author").value("Frank Herbert"))
                                .andExpect(jsonPath("$[0].releaseYear").value(1965));

                Mockito.verify(bs).searchBooksInDb(Mockito.eq("Dune"), Mockito.eq("Frank Herbert"), Mockito.isNull(), Mockito.nullable(Authentication.class));
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
                book.setIsbn("1672587");
                book.setPages(324);
                book.setImageUrl(
                                "https://books.google.com/books/content?id=7p9mEAAAQBAJ&printsec=frontcover&img=1&zoom=1");
                book.setDescription("dede de d e d ed e d e d e de d e d");
                book.setReleaseYear(2017);

                Mockito.when(bs.getAllBooks(Mockito.nullable(Authentication.class))).thenReturn(List.of(book));

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
                book.setIsbn("987654321");
                book.setPages(250);
                book.setImageUrl("https://example.com/image.jpg");
                book.setReleaseYear(2021);

                Mockito.when(bs.getBookById(Mockito.eq(1L), Mockito.nullable(Authentication.class))).thenReturn(book);

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

                Mockito.when(bs.addBook(Mockito.any(BookDTO.class), Mockito.any())).thenReturn(savedDTO);

                // Act & Assert
                mockMvc.perform(post("/books")
                                .principal(new UsernamePasswordAuthenticationToken("test-user", "password"))
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

            Mockito.when(bs.getMostReadBooks(
                    Mockito.eq(3),
                    Mockito.nullable(Authentication.class)))
                    .thenReturn(List.of(book1, book2, book3));

            // Act
            var result = mockMvc.perform(get("/books/most-read"));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].title").value("Most Read Book 1"))
                    .andExpect(jsonPath("$[1].title").value("Most Read Book 2"))
                    .andExpect(jsonPath("$[2].title").value("Most Read Book 3"));

            Mockito.verify(bs).getMostReadBooks(
                    Mockito.eq(3),
                    Mockito.nullable(Authentication.class));
        }

        @Test
        void givenCustomLimit_whenGetMostReadBooks_thenReturnRequestedNumberOfBooks() throws Exception {
            // Arrange
            BookDTO book1 = new BookDTO();
            book1.setId(1L);
            book1.setTitle("Popular Book");

            Mockito.when(bs.getMostReadBooks(
                    Mockito.eq(5),
                    Mockito.nullable(Authentication.class)))
                    .thenReturn(List.of(book1));

            // Act
            var result = mockMvc.perform(get("/books/most-read")
                    .param("limit", "5"));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].title").value("Popular Book"))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").hasJsonPath());

            Mockito.verify(bs).getMostReadBooks(
                    Mockito.eq(5),
                    Mockito.nullable(Authentication.class));
        }

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

            Mockito.when(bs.getNewestBooks(
                    Mockito.eq(3),
                    Mockito.nullable(Authentication.class)))
                    .thenReturn(List.of(book1, book2, book3));

            // Act
            var result = mockMvc.perform(get("/books/newest"));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].title").value("Newest Book 1"))
                    .andExpect(jsonPath("$[1].title").value("Newest Book 2"))
                    .andExpect(jsonPath("$[2].title").value("Newest Book 3"));

            Mockito.verify(bs).getNewestBooks(
                    Mockito.eq(3),
                    Mockito.nullable(Authentication.class));
        }

        @Test
        void givenCustomLimit_whenGetNewestBooks_thenReturnRequestedNumberOfBooks() throws Exception {
            // Arrange
            BookDTO book1 = new BookDTO();
            book1.setId(1L);
            book1.setTitle("Recently Added Book");

            Mockito.when(bs.getNewestBooks(
                    Mockito.eq(10),
                    Mockito.nullable(Authentication.class)))
                    .thenReturn(List.of(book1));

            // Act
            var result = mockMvc.perform(get("/books/newest")
                    .param("limit", "10"));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].title").value("Recently Added Book"))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").hasJsonPath());

            Mockito.verify(bs).getNewestBooks(
                    Mockito.eq(10),
                    Mockito.nullable(Authentication.class));
        }

        @Test
        void getBookByISBN_returnsJSONBook() throws Exception {
                BookDTO book = new BookDTO();
                book.setId(1L);
                book.setTitle("ISBN Boek");
                book.setAuthor("Auteur X");
                book.setIsbn("9781234567890");
                book.setPages(280);
                book.setImageUrl("https://example.com/book.jpg");
                book.setDescription("Beschrijving van het boek");
                book.setReleaseYear(2022);

                Mockito.when(bs.getBookByISBN(Mockito.eq("9781234567890"), Mockito.nullable(Authentication.class))).thenReturn(book);

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

                Mockito.verify(bs).getBookByISBN(Mockito.eq("9781234567890"), Mockito.nullable(Authentication.class));
        }

        @Test
        void getMaxPages_returnsMaxPages() throws Exception {
                Mockito.when(bs.getMaxPages()).thenReturn(1200);

                mockMvc.perform(get("/books/pages/max"))
                                .andExpect(status().isOk())
                                .andExpect(content().string("1200"));

                Mockito.verify(bs).getMaxPages();
        }

        @Test
        void givenValidRequest_whenUpdateBook_thenReturnUpdatedBook() throws Exception {
                // Arrange
                Long bookId = 1L;
                BookDTO requestDto = new BookDTO();
                requestDto.setTitle("Geüpdatet Boek");
                requestDto.setAuthor("Nieuwe Auteur");
                requestDto.setPages(400);

                BookDTO updatedDto = new BookDTO();
                updatedDto.setId(bookId);
                updatedDto.setTitle("Geüpdatet Boek");
                updatedDto.setAuthor("Nieuwe Auteur");
                updatedDto.setPages(400);

                Mockito.when(bs.updateBook(Mockito.eq(bookId), Mockito.any(BookDTO.class), Mockito.nullable(Authentication.class)))
                                .thenReturn(updatedDto);

                // Act
                var result = mockMvc.perform(put("/books/{id}", bookId)
                                .contentType("application/json")
                                .content(new ObjectMapper().writeValueAsString(requestDto)));

                // Assert
                result.andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(1))
                                .andExpect(jsonPath("$.title").value("Geüpdatet Boek"))
                                .andExpect(jsonPath("$.author").value("Nieuwe Auteur"))
                                .andExpect(jsonPath("$.pages").value(400));

                Mockito.verify(bs, Mockito.times(1)).updateBook(Mockito.eq(bookId), Mockito.any(BookDTO.class), Mockito.nullable(Authentication.class));
        }

        @Test
        void givenNonExistingBook_whenUpdateBook_thenReturn500() throws Exception {
                // Arrange
                Long bookId = 99L;
                BookDTO requestDto = new BookDTO();
                requestDto.setTitle("Spook Boek");

                Mockito.when(bs.updateBook(Mockito.eq(bookId), Mockito.any(BookDTO.class), Mockito.nullable(Authentication.class)))
                                .thenThrow(new RuntimeException("Boek niet gevonden met id: 99"));

                // Act
                var result = mockMvc.perform(put("/books/{id}", bookId)
                                .contentType("application/json")
                                .content(new ObjectMapper().writeValueAsString(requestDto)));

                // Assert
                result.andExpect(status().isInternalServerError())
                                .andExpect(r -> org.junit.jupiter.api.Assertions
                                                .assertInstanceOf(RuntimeException.class, r.getResolvedException()))
                                .andExpect(r -> org.junit.jupiter.api.Assertions.assertEquals(
                                                "Boek niet gevonden met id: 99",
                                                r.getResolvedException() != null ? r.getResolvedException().getMessage() : null));

                Mockito.verify(bs, Mockito.times(1)).updateBook(Mockito.eq(bookId), Mockito.any(BookDTO.class), Mockito.nullable(Authentication.class));
        }
}

