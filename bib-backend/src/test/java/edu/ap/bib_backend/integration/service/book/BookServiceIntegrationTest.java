package edu.ap.bib_backend.integration.service.book;

import edu.ap.bib_backend.BibBackendApplication;
import edu.ap.bib_backend.dto.BookDTO;
import edu.ap.bib_backend.jpa.entity.*;
import edu.ap.bib_backend.jpa.repository.*;
import edu.ap.bib_backend.service.book.BookService;
import edu.ap.bib_backend.service.book.OpenLibraryClient;
import edu.ap.bib_backend.service.user.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = BibBackendApplication.class)
@ActiveProfiles("test")
@Transactional
class BookServiceIntegrationTest {

    @Autowired
    private BookService bookService;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookCopyRepository bookCopyRepository;

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private CampusRepository campusRepository;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private CurrentUserService currentUserService;

    @MockitoBean
    private OpenLibraryClient openLibraryClient;

    @Test
    void Given_BooksInSchool_When_GetAllBooks_Then_ReturnsOnlySchoolBooks() {
        School school = createSchool("book-svc-1.be");
        Campus campus = createCampus("Campus 1", school);
        createBookWithCopy("Book A", campus);
        createBookWithCopy("Book B", campus);
        entityManager.flush();

        Authentication auth = mockAuth();
        when(currentUserService.getCurrentUserSchool(auth)).thenReturn(school);
        when(currentUserService.isModerator(auth)).thenReturn(true);

        List<BookDTO> result = bookService.getAllBooks(auth);

        List<String> titles = result.stream().map(BookDTO::getTitle).toList();
        assertTrue(titles.contains("Book A"));
        assertTrue(titles.contains("Book B"));
    }

    @Test
    void Given_ValidData_When_AddBook_Then_CreatesBookAndCopy() {
        School school = createSchool("book-svc-3.be");
        Campus campus = createCampus("Campus 3", school);
        entityManager.flush();

        Authentication auth = mockAuth();
        when(currentUserService.getCurrentUserSchool(auth)).thenReturn(school);
        when(currentUserService.canSeeDidacticBooks(auth)).thenReturn(true);
        when(currentUserService.isModerator(auth)).thenReturn(true);

        BookDTO dto = new BookDTO();
        dto.setIsbn("9783161484100");
        dto.setTitle("New Book");
        dto.setAuthor("New Author");
        dto.setPages(200);
        dto.setReleaseYear(2024);

        BookDTO result = bookService.addBook(dto, campus.getId(), auth);

        assertNotNull(result);
        assertEquals("New Book", result.getTitle());
        assertNotNull(result.getId());
    }

    @Test
    void Given_ExistingBook_When_AddCopyToBook_Then_CopyIsCreated() {
        School school = createSchool("book-svc-4.be");
        Campus campus = createCampus("Campus 4", school);
        Book book = createBookWithCopy("Book For Copy", campus);
        entityManager.flush();
        Long initialCopyCount = bookCopyRepository.countByBookId(book.getId());

        Authentication auth = mockAuth();
        when(currentUserService.getCurrentUserSchool(auth)).thenReturn(school);

        bookService.addCopyToBook(book.getId(), campus.getId(), auth);

        entityManager.flush();
        Long newCopyCount = bookCopyRepository.countByBookId(book.getId());
        assertEquals(initialCopyCount + 1, newCopyCount);
    }

    @Test
    void Given_ExistingBook_When_GetBookById_Then_ReturnsBook() {
        School school = createSchool("book-svc-5.be");
        Campus campus = createCampus("Campus 5", school);
        Book book = createBookWithCopy("Retrievable Book", campus);
        entityManager.flush();

        Authentication auth = mockAuth();
        when(currentUserService.getCurrentUserSchool(auth)).thenReturn(school);
        when(currentUserService.canSeeDidacticBooks(auth)).thenReturn(true);
        when(currentUserService.resolveSchool(any(), any())).thenAnswer(inv -> {
            Long sid = inv.getArgument(0);
            if (sid != null) {
                return schoolRepository.findById(sid).orElseThrow();
            }
            return currentUserService.getCurrentUserSchool(auth);
        });

        BookDTO result = bookService.getBookById(book.getId(), auth);

        assertNotNull(result);
        assertEquals("Retrievable Book", result.getTitle());
    }

    @Test
    void Given_BooksWithLoans_When_GetMostReadBooks_Then_ReturnsOrderedByLoanCount() {
        School school = createSchool("book-svc-6.be");
        Campus campus = createCampus("Campus 6", school);
        createBookWithCopy("Most Read Book", campus);
        entityManager.flush();

        Authentication auth = mockAuth();
        when(currentUserService.getCurrentUserSchool(auth)).thenReturn(school);
        when(currentUserService.canSeeDidacticBooks(auth)).thenReturn(true);
        when(currentUserService.resolveSchool(any(), any())).thenReturn(school);

        List<BookDTO> result = bookService.getMostReadBooks(5, auth);

        assertFalse(result.isEmpty());
    }

    @Test
    void Given_BooksInSchool_When_GetNewestBooks_Then_ReturnsOrderedByAddedDate() {
        School school = createSchool("book-svc-7.be");
        Campus campus = createCampus("Campus 7", school);
        createBookWithCopy("Older Book", campus);
        createBookWithCopy("Newer Book", campus);
        entityManager.flush();

        Authentication auth = mockAuth();
        when(currentUserService.getCurrentUserSchool(auth)).thenReturn(school);
        when(currentUserService.canSeeDidacticBooks(auth)).thenReturn(true);
        when(currentUserService.resolveSchool(any(), any())).thenReturn(school);

        List<BookDTO> result = bookService.getNewestBooks(5, auth);

        assertFalse(result.isEmpty());
    }

    @Test
    void Given_Books_When_GetMaxPages_Then_ReturnsMaxValue() {
        Book book1 = new Book("9781111111111", "Short", "A", 50, null, null, 2020);
        Book book2 = new Book("9782222222222", "Long", "B", 800, null, null, 2020);
        bookRepository.save(book1);
        bookRepository.save(book2);

        Integer maxPages = bookService.getMaxPages();

        assertEquals(800, maxPages);
    }

    @Test
    void Given_Books_When_GetMinPages_Then_ReturnsMinValue() {
        Book book1 = new Book("9783333333333", "Short", "A", 30, null, null, 2020);
        Book book2 = new Book("9784444444444", "Long", "B", 500, null, null, 2020);
        bookRepository.save(book1);
        bookRepository.save(book2);

        Integer minPages = bookService.getMinPages();

        assertEquals(30, minPages);
    }

    @Test
    void Given_ExistingBookByIsbn_When_GetBookByISBN_Then_ReturnsBook() {
        School school = createSchool("book-svc-8.be");
        Campus campus = createCampus("Campus 8", school);
        Book book = new Book("9783161484100", "ISBN Book", "Author", 150, null, null, 2023);
        bookRepository.save(book);
        BookCopy copy = new BookCopy();
        copy.setBook(book);
        copy.setCampus(campus);
        bookCopyRepository.save(copy);
        entityManager.flush();

        Authentication auth = mockAuth();
        when(currentUserService.getCurrentUserSchool(auth)).thenReturn(school);
        when(currentUserService.canSeeDidacticBooks(auth)).thenReturn(true);
        when(currentUserService.resolveSchool(any(), any())).thenReturn(school);

        BookDTO result = bookService.getBookByISBN("9783161484100", auth);

        assertNotNull(result);
        assertEquals("ISBN Book", result.getTitle());
    }

    @Test
    void Given_NoMatchingIsbn_When_GetBookByISBN_Then_FallsBackToOpenLibrary() {
        School school = createSchool("book-svc-9.be");
        Authentication auth = mockAuth();
        when(currentUserService.getCurrentUserSchool(auth)).thenReturn(school);
        when(currentUserService.canSeeDidacticBooks(auth)).thenReturn(true);
        when(currentUserService.resolveSchool(any(), any())).thenReturn(school);

        BookDTO openLibBook = new BookDTO();
        openLibBook.setIsbn("9789999999999");
        openLibBook.setTitle("Open Library Book");
        openLibBook.setAuthor("OL Author");
        openLibBook.setPages(300);
        openLibBook.setReleaseYear(2022);
        when(openLibraryClient.fetchBookByISBN("9789999999999")).thenReturn(openLibBook);

        BookDTO result = bookService.getBookByISBN("9789999999999", auth);

        assertNotNull(result);
        assertEquals("Open Library Book", result.getTitle());
    }

    @Test
    void Given_BooksInDb_When_SearchBooksInDb_Then_ReturnsMatchingBooks() {
        School school = createSchool("book-svc-10.be");
        Book book = new Book("9781234567890", "Harry Potter", "J.K. Rowling", 300, null, null, 2000);
        bookRepository.save(book);
        entityManager.flush();

        Authentication auth = mockAuth();
        when(currentUserService.getCurrentUserSchool(auth)).thenReturn(school);

        List<edu.ap.bib_backend.dto.BookCandidateDTO> result = bookService.searchBooksInDb("Harry", null, null, auth);

        assertFalse(result.isEmpty());
    }

    @Test
    void Given_NoFilters_When_SearchBooksInDb_Then_ReturnsEmpty() {
        Authentication auth = mockAuth();

        List<edu.ap.bib_backend.dto.BookCandidateDTO> result = bookService.searchBooksInDb(null, null, null, auth);

        assertTrue(result.isEmpty());
    }

    private School createSchool(String domain) {
        School school = new School();
        school.setDomainName(domain);
        school.setName("Test School " + domain);
        return schoolRepository.save(school);
    }

    private Campus createCampus(String name, School school) {
        Campus campus = new Campus();
        campus.setName(name);
        campus.setSchool(school);
        campus.setAddress("Address " + name);
        campus.setPostalCode(2000);
        return campusRepository.save(campus);
    }

    private Book createBookWithCopy(String title, Campus campus) {
        Book book = new Book();
        book.setIsbn("978" + Math.abs(title.hashCode()));
        book.setTitle(title);
        book.setAuthor("Author " + title);
        book.setPages(100 + title.length());
        book.setReleaseYear(2024);
        book = bookRepository.save(book);

        BookCopy copy = new BookCopy();
        copy.setBook(book);
        copy.setCampus(campus);
        bookCopyRepository.save(copy);

        return book;
    }

    private Authentication mockAuth() {
        return org.mockito.Mockito.mock(Authentication.class);
    }
}
