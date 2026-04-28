package edu.ap.bib_backend.service.book;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.ArgumentMatchers.anyLong;  // removed unused import
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

import edu.ap.bib_backend.dto.BookDTO;
import edu.ap.bib_backend.dto.GenreDTO;
import edu.ap.bib_backend.dto.ReadingLevelDTO;
import edu.ap.bib_backend.exception.ResourceNotFoundException;
import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.entity.BookCopy;
import edu.ap.bib_backend.jpa.entity.Genre;
import edu.ap.bib_backend.jpa.entity.ReadingLevel;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.repository.BookCopyRepository;
import edu.ap.bib_backend.jpa.repository.BookLoanRepository;
import edu.ap.bib_backend.jpa.repository.BookRepository;
import edu.ap.bib_backend.jpa.repository.GenreRepository;
import edu.ap.bib_backend.jpa.repository.ReadingLevelRepository;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import edu.ap.bib_backend.jpa.repository.review.ReviewRepository;
import edu.ap.bib_backend.mapper.BookCandidateMapper;
import edu.ap.bib_backend.mapper.BookMapper;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BookServiceTest {

        @Mock
        private BookRepository br;

        @Mock
        private BookCopyRepository bcr;

        @Mock
        private GenreRepository gr;

        @Mock
        private ReadingLevelRepository rlr;

        @Mock
        private ReviewRepository reviewRepository;

        @Mock
        private BookLoanRepository bookLoanRepository;

        @Mock
        private BookMapper bookMapper1;

        @Mock
        private BookCandidateMapper bookCandidateMapper;

        @Mock
        private OpenLibraryClient openLibraryClient;

        @Mock
        private UserRepository userRepository;

        private BookService bs;
        private UsernamePasswordAuthenticationToken authentication;

        @BeforeEach
        void setUp() {
                bs = new BookService(
                                br,
                                bcr,
                                gr,
                                reviewRepository,
                                bookLoanRepository,
                                rlr,
                                bookMapper1,
                                bookCandidateMapper,
                                openLibraryClient,
                                userRepository);

                // create a default authentication and user with school for tests that need it
                authentication = new UsernamePasswordAuthenticationToken("test-user", null);
                User user = new User();
                user.setId("test-user");
                School school = new School();
                school.setDomainName("school1");
                user.setSchool(school);
                // make this lenient to avoid UnnecessaryStubbingException in tests that don't use the userRepository stub
                org.mockito.Mockito.lenient().when(userRepository.findById("test-user")).thenReturn(Optional.of(user));
        }

        @Test
        @DisplayName("Given books exist in DB, When getAllBooks is called, Then return mapped DTOs")
        void givenBooksExist_whenGetAllBooks_thenReturnMappedDTOs() {
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

                BookDTO dto = new BookDTO();
                dto.setId(1L);
                dto.setIsbn("9781234567890");
                dto.setTitle("Test Boek");
                dto.setAuthor("Auteur X");

                given(br.findAll()).willReturn(List.of(book));
                given(bookMapper1.toDTOList(List.of(book))).willReturn(List.of(dto));

                List<BookDTO> result = bs.getAllBooks();

                assertThat(result).hasSize(1);
                assertThat(result.get(0).getId()).isEqualTo(1L);
                assertThat(result.get(0).getIsbn()).isEqualTo("9781234567890");
                assertThat(result.get(0).getTitle()).isEqualTo("Test Boek");
                assertThat(result.get(0).getAuthor()).isEqualTo("Auteur X");
        }

        @Test
        @DisplayName("Given valid book without ISBN, When addBook is called, Then save Book and BookCopy")
        void givenValidBookWithoutISBN_whenAddBook_thenSaveBookAndBookCopy() {
                BookDTO dto = new BookDTO();
                dto.setTitle("Nieuwe Boek");
                dto.setAuthor("Auteur Test");
                dto.setPages(300);
                dto.setImageUrl("https://example.com/image.jpg");
                dto.setDescription("Beschrijving hier");
                dto.setReleaseYear(2024);
                dto.setGenres(List.of());

                Book mappedBook = new Book();
                mappedBook.setTitle("Nieuwe Boek");
                mappedBook.setAuthor("Auteur Test");

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

                BookDTO savedDto = new BookDTO();
                savedDto.setId(1L);
                savedDto.setTitle("Nieuwe Boek");
                savedDto.setAuthor("Auteur Test");

                given(bookMapper1.toEntity(dto)).willReturn(mappedBook);
                given(br.save(any(Book.class))).willReturn(savedBook);
                given(bcr.save(any(BookCopy.class))).willAnswer(invocation -> invocation.getArgument(0));
                given(bookMapper1.toDTO(savedBook)).willReturn(savedDto);

                BookDTO result = bs.addBook(authentication, dto);

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

                BookDTO existingDto = new BookDTO();
                existingDto.setId(5L);
                existingDto.setIsbn("9781234567890");
                existingDto.setTitle("Bestaand Boek");

                given(br.findByIsbn("9781234567890")).willReturn(existingBook);
                given(bcr.save(any(BookCopy.class))).willAnswer(invocation -> invocation.getArgument(0));
                given(bookMapper1.toDTO(existingBook)).willReturn(existingDto);

                BookDTO result = bs.addBook(authentication, dto);

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
                Genre genre = createGenre("Roman", "#8B5CF6", "Literaire fictie");

                BookDTO dto = new BookDTO();
                dto.setTitle("Boek");
                dto.setAuthor("Auteur");
                dto.setPages(123);
                dto.setGenres(List.of(
                                new GenreDTO(null,"Roman", "#8B5CF6", "Literaire fictie", null, null)));

                Book mappedBook = new Book();
                mappedBook.setTitle("Boek");
                mappedBook.setAuthor("Auteur");

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

                BookDTO savedDto = new BookDTO();
                savedDto.setId(7L);
                savedDto.setTitle("Boek");

                given(bookMapper1.toEntity(dto)).willReturn(mappedBook);
                given(gr.findByName("Roman")).willReturn(Optional.of(genre));
                given(br.save(any(Book.class))).willReturn(savedBook);
                given(bcr.save(any(BookCopy.class))).willAnswer(invocation -> invocation.getArgument(0));
                given(bookMapper1.toDTO(savedBook)).willReturn(savedDto);

                bs.addBook(authentication, dto);

                ArgumentCaptor<Book> captor = ArgumentCaptor.forClass(Book.class);
                then(br).should().save(captor.capture());

                Book mappedAndSavedBook = captor.getValue();
                assertThat(mappedAndSavedBook.getGenres()).hasSize(1);
                assertThat(mappedAndSavedBook.getGenres())
                                .extracting(Genre::getName)
                                .containsExactly("Roman");
        }

        @Test
        @DisplayName("Given unknown genre, When addBook is called, Then throw Exception")
        void givenUnknownGenre_whenAddBook_thenThrowException() {
                BookDTO dto = new BookDTO();
                dto.setTitle("Boek");
                dto.setAuthor("Auteur");
                dto.setGenres(List.of(
                                new GenreDTO(null, "Onbekend", "#000000", "Bestaat niet", null, null)));

                Book mappedBook = new Book();
                given(bookMapper1.toEntity(dto)).willReturn(mappedBook);
                given(gr.findByName("Onbekend")).willReturn(Optional.empty());

                assertThatThrownBy(() -> bs.addBook(authentication, dto))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessage("Genre bestaat niet: Onbekend");

                then(br).should(never()).save(any(Book.class));
                then(bcr).should(never()).save(any(BookCopy.class));
        }

        @Test
        @DisplayName("Given unknown reading level id, when addBook is called, then throw exception")
        void givenUnknownReadingLevelId_whenAddBook_thenThrowException() {
                BookDTO dto = new BookDTO();
                dto.setTitle("Boek");
                dto.setAuthor("Auteur");
                dto.setGenres(List.of());
                dto.setReadingLevel(new ReadingLevelDTO(99L, "Z", "onbekend"));

                Book mappedBook = new Book();
                given(bookMapper1.toEntity(dto)).willReturn(mappedBook);
                given(rlr.findById(99L)).willReturn(Optional.empty());

                assertThatThrownBy(() -> bs.addBook(authentication, dto))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessage("Reading level bestaat niet: 99");

                then(br).should(never()).save(any(Book.class));
                then(bcr).should(never()).save(any(BookCopy.class));
        }

        @Test
        @DisplayName("Given valid reading level id, when addBook is called, then set reading level")
        void givenValidReadingLevelId_whenAddBook_thenSetReadingLevel() {
                BookDTO dto = new BookDTO();
                dto.setTitle("Boek");
                dto.setAuthor("Auteur");
                dto.setGenres(List.of());
                dto.setReadingLevel(new ReadingLevelDTO(2L, "B", "beschrijving"));

                ReadingLevel readingLevel = new ReadingLevel();
                readingLevel.setId(2L);
                readingLevel.setNiveauName("B");
                readingLevel.setDescription("beschrijving");

                Book mappedBook = new Book();
                Book savedBook = createBook(
                                10L,
                                null,
                                "Boek",
                                "Auteur",
                                123,
                                null,
                                null,
                                2024,
                                0.0,
                                Set.of());
                savedBook.setReadingLevel(readingLevel);

                BookDTO savedDto = new BookDTO();
                savedDto.setId(10L);
                savedDto.setReadingLevel(new ReadingLevelDTO(2L, "B", "beschrijving"));

                given(bookMapper1.toEntity(dto)).willReturn(mappedBook);
                given(rlr.findById(2L)).willReturn(Optional.of(readingLevel));
                given(br.save(any(Book.class))).willReturn(savedBook);
                given(bcr.save(any(BookCopy.class))).willAnswer(invocation -> invocation.getArgument(0));
                given(bookMapper1.toDTO(savedBook)).willReturn(savedDto);

                BookDTO result = bs.addBook(authentication, dto);

                assertThat(result.getReadingLevel()).isNotNull();
                assertThat(result.getReadingLevel().getId()).isEqualTo(2L);
        }

        @Test
        @DisplayName("Given existing book ID, When getBookById is called, Then return DTO")
        void givenExistingBookId_whenGetBookById_thenReturnDTO() {
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

                BookDTO dto = new BookDTO();
                dto.setId(3L);
                dto.setTitle("Boek 3");
                dto.setAuthor("Auteur 3");

                given(br.findById(3L)).willReturn(Optional.of(book));
                given(bookMapper1.toDTO(book)).willReturn(dto);

                BookDTO result = bs.getBookById(3L);

                assertThat(result.getId()).isEqualTo(3L);
                assertThat(result.getTitle()).isEqualTo("Boek 3");
                assertThat(result.getAuthor()).isEqualTo("Auteur 3");
        }

        @Test
        @DisplayName("Given non-existing book ID, When getBookById is called, Then throw Exception")
        void givenNonExistingBookId_whenGetBookById_thenThrowException() {
                given(br.findById(99L)).willReturn(Optional.empty());

                assertThatThrownBy(() -> bs.getBookById(99L))
                                .isInstanceOf(ResourceNotFoundException.class)
                                .hasMessageContaining("Boek niet gevonden met id: 99");
        }

        @Test
        @DisplayName("Given books exist, When getMaxPages is called, Then return repository value")
        void givenBooksExist_whenGetMaxPages_thenReturnRepositoryValue() {
                given(br.findMaxPages()).willReturn(900);

                Integer result = bs.getMaxPages();

                assertThat(result).isEqualTo(900);
        }

        @Test
        @DisplayName("Given no books exist, When getMaxPages is called, Then return default 1500")
        void givenNoBooksExist_whenGetMaxPages_thenReturnDefault1500() {
                given(br.findMaxPages()).willReturn(null);

                Integer result = bs.getMaxPages();

                assertThat(result).isEqualTo(1500);
        }

        @Test
        @DisplayName("Given books exist, When getMinPages is called, Then return repository value")
        void givenBooksExist_whenGetMinPages_thenReturnRepositoryValue() {
                given(br.findMinPages()).willReturn(50);

                Integer result = bs.getMinPages();

                assertThat(result).isEqualTo(50);
        }

        @Test
        @DisplayName("Given no books exist, When getMinPages is called, Then return default 0")
        void givenNoBooksExist_whenGetMinPages_thenReturnDefault0() {
                given(br.findMinPages()).willReturn(null);

                Integer result = bs.getMinPages();

                assertThat(result).isEqualTo(0);
        }

        @Test
        @DisplayName("Given most read books exist, When getMostReadBooks is called, Then return mapped DTOs")
        void givenMostReadBooksExist_whenGetMostReadBooks_thenReturnMappedDTOs() {
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

                BookDTO dto = new BookDTO();
                dto.setTitle("Populair Boek");

                given(br.findMostReadBook(PageRequest.of(0, 5))).willReturn(List.of(book));
                given(bookMapper1.toDTOList(List.of(book))).willReturn(List.of(dto));

                List<BookDTO> result = bs.getMostReadBooks(5);

                assertThat(result).hasSize(1);
                assertThat(result.get(0).getTitle()).isEqualTo("Populair Boek");
        }

        @Test
        @DisplayName("Given no most read books, When getMostReadBooks is called, Then throw NotFound Exception")
        void givenNoMostReadBooks_whenGetMostReadBooks_thenThrowNotFound() {
                given(br.findMostReadBook(PageRequest.of(0, 5))).willReturn(List.of());

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

                BookDTO dto = new BookDTO();
                dto.setTitle("Nieuw Boek");

                given(br.findNewestBooks(PageRequest.of(0, 3))).willReturn(List.of(book));
                given(bookMapper1.toDTOList(List.of(book))).willReturn(List.of(dto));

                List<BookDTO> result = bs.getNewestBooks(3);

                assertThat(result).hasSize(1);
                assertThat(result.get(0).getTitle()).isEqualTo("Nieuw Boek");
                then(br).should().findNewestBooks(PageRequest.of(0, 3));
        }

        @Test
        @DisplayName("Given related books exist, When getRelatedBooksCarousel is called, Then return mapped books")
        void givenRelatedBooksExist_whenGetRelatedBooksCarousel_thenReturnMappedBooks() {
                Book source = new Book();
                source.setId(1L);

                Book related = new Book();
                related.setId(2L);
                related.setTitle("Related");
                related.setAuthor("Author");
                related.setGenres(Set.of());

                BookDTO relatedDto = new BookDTO();
                relatedDto.setId(2L);
                relatedDto.setTitle("Related");

                List<Object[]> relatedBooks = new ArrayList<>();
                relatedBooks.add(new Object[] { related, 2L });

                given(br.findById(1L)).willReturn(Optional.of(source));
                given(br.findRelatedBooksWithScore(1L, PageRequest.of(0, 10))).willReturn(relatedBooks);
                given(bookMapper1.toDTO(related)).willReturn(relatedDto);

                List<BookDTO> result = bs.getRelatedBooksCarousel(1L, 10);

                assertThat(result).hasSize(1);
                assertThat(result.get(0).getId()).isEqualTo(2L);
                assertThat(result.get(0).getTitle()).isEqualTo("Related");
        }

        @Test
        @DisplayName("Given valid data, When updateBook is called, Then return updated DTO")
        void givenValidData_whenUpdateBook_thenReturnUpdatedDTO() {
                Long bookId = 1L;
                Book existingBook = createBook(bookId, "9780000000000", "Oud", "Oud", 100, null, null, 2000, 0.0,
                                new HashSet<>());

                BookDTO updateDto = new BookDTO();
                updateDto.setIsbn("978-1234567890");
                updateDto.setTitle("Nieuwe Titel");
                updateDto.setAuthor("Nieuwe Auteur");
                updateDto.setPages(250);
                updateDto.setGenres(new ArrayList<>());

                Book savedBook = createBook(bookId, "9781234567890", "Nieuwe Titel", "Nieuwe Auteur", 250, null, null,
                                2000, 0.0, new HashSet<>());

                BookDTO savedDto = new BookDTO();
                savedDto.setId(bookId);
                savedDto.setTitle("Nieuwe Titel");
                savedDto.setIsbn("9781234567890");

                given(br.findById(bookId)).willReturn(Optional.of(existingBook));
                given(br.findByIsbn("9781234567890")).willReturn(null);
                given(br.save(any(Book.class))).willReturn(savedBook);
                given(bookMapper1.toDTO(savedBook)).willReturn(savedDto);

                BookDTO result = bs.updateBook(bookId, updateDto);

                assertThat(result).isNotNull();
                assertThat(result.getId()).isEqualTo(bookId);
                assertThat(result.getTitle()).isEqualTo("Nieuwe Titel");
                assertThat(result.getIsbn()).isEqualTo("9781234567890");

                then(br).should().findById(bookId);
                then(br).should().findByIsbn("9781234567890");
                then(br).should().save(any(Book.class));
        }

        @Test
        @DisplayName("Given same ISBN as before, When updateBook is called, Then update successfully")
        void givenSameIsbnAsBefore_whenUpdateBook_thenUpdateSuccessfully() {
                Long bookId = 2L;
                String isbn = "9781111111111";

                Book existingBook = createBook(bookId, isbn, "Titel", "Auteur", 100, null, null, 2000, 0.0,
                                new HashSet<>());

                BookDTO updateDto = new BookDTO();
                updateDto.setIsbn(isbn);
                updateDto.setTitle("Geüpdatete Titel");
                updateDto.setGenres(new ArrayList<>());

                BookDTO savedDto = new BookDTO();
                savedDto.setTitle("Geüpdatete Titel");

                given(br.findById(bookId)).willReturn(Optional.of(existingBook));
                given(br.findByIsbn(isbn)).willReturn(existingBook);
                given(br.save(any(Book.class))).willAnswer(invocation -> invocation.getArgument(0));
                given(bookMapper1.toDTO(any(Book.class))).willReturn(savedDto);

                BookDTO result = bs.updateBook(bookId, updateDto);

                assertThat(result.getTitle()).isEqualTo("Geüpdatete Titel");
                then(br).should().save(any(Book.class));
        }

        @Test
        @DisplayName("Given ISBN belonging to another book, When updateBook is called, Then throw BadRequest Exception")
        void givenIsbnBelongingToAnotherBook_whenUpdateBook_thenThrowBadRequestException() {
                Long currentBookId = 3L;
                Long otherBookId = 4L;
                String duplicateIsbn = "9789999999999";

                Book currentBook = createBook(currentBookId, "9783333333333", "Titel", "Auteur", 100, null, null, 2000,
                                0.0, new HashSet<>());

                Book otherBookWithSameIsbn = createBook(otherBookId, duplicateIsbn, "Ander Boek", "Ander", 200, null,
                                null, 2010, 0.0, new HashSet<>());

                BookDTO updateDto = new BookDTO();
                updateDto.setIsbn(duplicateIsbn);

                given(br.findById(currentBookId)).willReturn(Optional.of(currentBook));
                given(br.findByIsbn(duplicateIsbn)).willReturn(otherBookWithSameIsbn);

                assertThatThrownBy(() -> bs.updateBook(currentBookId, updateDto))
                                .isInstanceOf(ResponseStatusException.class)
                                .satisfies(ex -> {
                                        ResponseStatusException rse = (ResponseStatusException) ex;
                                        assertThat(rse.getStatusCode().value()).isEqualTo(400);
                                        assertThat(rse.getReason()).contains("Er bestaat al een ander boek met ISBN");
                                });

                then(br).should(never()).save(any(Book.class));
        }

        @Test
        @DisplayName("Given non-existing book ID, When updateBook is called, Then throw Exception")
        void givenNonExistingBookId_whenUpdateBook_thenThrowException() {
                Long invalidId = 99L;
                BookDTO updateDto = new BookDTO();
                updateDto.setTitle("Ghost Book");

                given(br.findById(invalidId)).willReturn(Optional.empty());

                assertThatThrownBy(() -> bs.updateBook(invalidId, updateDto))
                                .isInstanceOf(ResourceNotFoundException.class)
                                .hasMessageContaining("Boek niet gevonden met id: 99");

                then(br).should(never()).findByIsbn(anyString());
                then(br).should(never()).save(any());
        }

        @Test
        @DisplayName("Given existing book ID, When addCopyToBook is called, Then save available copy")
        void givenExistingBookId_whenAddCopyToBook_thenSaveAvailableCopy() {
                Book book = createBook(11L, "9781234567890", "Titel", "Auteur", 123, null, null, 2020, 0.0, Set.of());
                BookDTO dto = new BookDTO();
                dto.setId(11L);

                given(br.findById(11L)).willReturn(Optional.of(book));
                given(bcr.save(any(BookCopy.class))).willAnswer(invocation -> invocation.getArgument(0));
                given(bookMapper1.toDTO(book)).willReturn(dto);

                BookDTO result = bs.addCopyToBook(authentication, 11L);

                assertThat(result.getId()).isEqualTo(11L);
                then(bcr).should().save(any(BookCopy.class));
        }

        @Test
        @DisplayName("Given missing ISBN, When getBookByISBN is called, Then throw bad request")
        void givenMissingIsbn_whenGetBookByISBN_thenThrowBadRequest() {
                assertThatThrownBy(() -> bs.getBookByISBN("   "))
                                .isInstanceOf(ResponseStatusException.class)
                                .satisfies(ex -> {
                                        ResponseStatusException rse = (ResponseStatusException) ex;
                                        assertThat(rse.getStatusCode().value()).isEqualTo(400);
                                        assertThat(rse.getReason()).isEqualTo("Ongeldig ISBN");
                                });
        }

        @Test
        @DisplayName("Given ISBN exists in DB, When getBookByISBN is called, Then return DB book")
        void givenExistingIsbnInDb_whenGetBookByISBN_thenReturnDbBook() {
                Book book = createBook(15L, "9781234567890", "DB Boek", "Auteur", 200, null, null, 2020, 0.0, Set.of());
                BookDTO dto = new BookDTO();
                dto.setId(15L);
                dto.setIsbn("9781234567890");
                dto.setTitle("DB Boek");

                given(br.findByIsbn("9781234567890")).willReturn(book);
                given(bookMapper1.toDTO(book)).willReturn(dto);

                BookDTO result = bs.getBookByISBN("978-1234567890");

                assertThat(result.getId()).isEqualTo(15L);
                assertThat(result.getTitle()).isEqualTo("DB Boek");
                then(openLibraryClient).should(never()).fetchBookByISBN(anyString());
        }

        @Test
        @DisplayName("Given ISBN does not exist in DB, When getBookByISBN is called, Then fetch from OpenLibrary")
        void givenMissingIsbnInDb_whenGetBookByISBN_thenFetchFromOpenLibrary() {
                BookDTO dto = new BookDTO();
                dto.setIsbn("9781234567890");
                dto.setTitle("OpenLibrary Boek");

                given(br.findByIsbn("9781234567890")).willReturn(null);
                given(openLibraryClient.fetchBookByISBN("9781234567890")).willReturn(dto);

                BookDTO result = bs.getBookByISBN("978-1234567890");

                assertThat(result.getTitle()).isEqualTo("OpenLibrary Boek");
                then(openLibraryClient).should().fetchBookByISBN("9781234567890");
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

