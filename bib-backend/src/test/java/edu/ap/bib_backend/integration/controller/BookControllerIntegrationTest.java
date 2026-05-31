package edu.ap.bib_backend.integration.controller;

import edu.ap.bib_backend.BibBackendApplication;
import edu.ap.bib_backend.jpa.entity.*;
import edu.ap.bib_backend.jpa.repository.*;
import edu.ap.bib_backend.jpa.repository.review.ReviewReportRepository;
import edu.ap.bib_backend.jpa.repository.review.ReviewRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = BibBackendApplication.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class BookControllerIntegrationTest {

    private static final String USER_ID = "book-ctrl-user";

    private UsernamePasswordAuthenticationToken currentPrincipal;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookCopyRepository bookCopyRepository;

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private CampusRepository campusRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleEntityRepository roleEntityRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private LoanExtensionRepository loanExtensionRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ReviewReportRepository reviewReportRepository;

    @Autowired
    private MailSubscriptionRepository mailSubscriptionRepository;

    @Autowired
    private OneRosterCredentialRepository oneRosterCredentialRepository;

    @Autowired
    private OneRosterTokenRepository oneRosterTokenRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private StudentClassStatisticRepository studentClassStatisticRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private ReadingListRepository readingListRepository;

    @Autowired
    private ReadingListBookRepository readingListBookRepository;

    @Autowired
    private GenreRepository genreRepository;

    private School school;
    private Campus campus;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        school = new School();
        school.setDomainName("book-ctrl-test.be");
        school.setName("Book Ctrl Test School");
        school = schoolRepository.save(school);

        campus = new Campus();
        campus.setName("Book Ctrl Campus");
        campus.setSchool(school);
        campus.setAddress("Test Address");
        campus.setPostalCode(2000);
        campus = campusRepository.save(campus);

        RoleEntity role = getOrCreateRole(edu.ap.bib_backend.jpa.enums.Role.LIBRARIAN);

        User user = new User();
        user.setId(USER_ID);
        user.setRole(role);
        user.setSchool(school);
        userRepository.save(user);

        setAuthentication();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void givenBooksExist_whenGetAllBooks_thenReturnsBooks() throws Exception {
        createBookWithCopy("Controller Book A", "9781000000001");
        createBookWithCopy("Controller Book B", "9781000000002");

        mockMvc.perform(get("/books").principal(currentPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void givenBookExists_whenGetBookById_thenReturnsBook() throws Exception {
        Book book = createBookWithCopy("Single Book", "9781000000003");

        mockMvc.perform(get("/books/{id}", book.getId()).principal(currentPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Single Book"));
    }

    @Test
    void givenBooksExist_whenGetMaxPages_thenReturnsMax() throws Exception {
        Book book = new Book("9781000000004", "Pages Test", "Author", 500, null, null, 2020);
        bookRepository.save(book);

        mockMvc.perform(get("/books/pages/max").principal(currentPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(500));
    }

    @Test
    void givenBooksExist_whenGetMinPages_thenReturnsMin() throws Exception {
        Book book = new Book("9781000000005", "Min Pages", "Author", 25, null, null, 2020);
        bookRepository.save(book);

        mockMvc.perform(get("/books/pages/min").principal(currentPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(25));
    }

    @Test
    void givenBooksExist_whenGetMostReadBooks_thenReturnsOk() throws Exception {
        createBookWithCopy("Most Read Ctrl", "9781000000006");

        mockMvc.perform(get("/books/most-read").principal(currentPrincipal))
                .andExpect(status().isOk());
    }

    @Test
    void givenBooksExist_whenGetNewestBooks_thenReturnsOk() throws Exception {
        createBookWithCopy("Newest Ctrl", "9781000000007");

        mockMvc.perform(get("/books/newest").principal(currentPrincipal))
                .andExpect(status().isOk());
    }

    @Test
    void givenBookByIsbn_whenGetBookByISBN_thenReturnsBook() throws Exception {
        createBookWithCopy("ISBN Book Ctrl", "9781000000008");

        mockMvc.perform(get("/books/isbn/{isbn}", "9781000000008").principal(currentPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isbn").value("9781000000008"));
    }

    @Test
    void givenFilterParams_whenGetFilteredBooks_thenReturnsPage() throws Exception {
        createBookWithCopy("Searchable Ctrl", "9781000000009");

        mockMvc.perform(get("/books/catalog")
                        .param("searchTerm", "Searchable")
                        .principal(currentPrincipal))
                .andExpect(status().isOk());
    }

    @Test
    void givenRelatedBook_whenGetRelatedBooksCarousel_thenReturnsRelated() throws Exception {
        Genre genre = new Genre();
        genre.setName("Test Genre " + System.nanoTime());
        genre.setColor("#654321");
        genre = genreRepository.save(genre);

        Book book1 = createBookWithCopy("Source Book", "9782000000001");
        Book book2 = createBookWithCopy("Related Book", "9782000000002");
        book1.getGenres().add(genre);
        book2.getGenres().add(genre);
        bookRepository.save(book1);
        bookRepository.save(book2);

        mockMvc.perform(get("/books/{id}/related-carousel", book1.getId()).principal(currentPrincipal))
                .andExpect(status().isOk());
    }

    private Book createBookWithCopy(String title, String isbn) {
        Book book = new Book();
        book.setIsbn(isbn);
        book.setTitle(title);
        book.setAuthor("Author " + title);
        book.setPages(100);
        book.setReleaseYear(2024);
        book = bookRepository.save(book);

        BookCopy copy = new BookCopy();
        copy.setBook(book);
        copy.setCampus(campus);
        bookCopyRepository.save(copy);

        return book;
    }

    private void setAuthentication() {
        currentPrincipal = new UsernamePasswordAuthenticationToken(
                USER_ID, null,
                List.of(new SimpleGrantedAuthority("ROLE_LIBRARIAN")));
        SecurityContextHolder.getContext().setAuthentication(currentPrincipal);
    }

    private RoleEntity getOrCreateRole(edu.ap.bib_backend.jpa.enums.Role role) {
        return roleEntityRepository.findByRole(role)
                .orElseGet(() -> {
                    RoleEntity newRole = new RoleEntity();
                    newRole.setRole(role);
                    return roleEntityRepository.save(newRole);
                });
    }

    private void cleanDatabase() {
        readingListBookRepository.deleteAll();
        readingListRepository.deleteAll();
        reviewReportRepository.deleteAll();
        reviewRepository.deleteAll();
        mailSubscriptionRepository.deleteAll();
        loanExtensionRepository.deleteAll();
        loanRepository.deleteAll();
        oneRosterTokenRepository.deleteAll();
        oneRosterCredentialRepository.deleteAll();
        studentClassStatisticRepository.deleteAll();
        bookCopyRepository.deleteAll();
        bookRepository.deleteAll();
        tagRepository.deleteAll();
        userRepository.deleteAll();
        groupRepository.deleteAll();
        campusRepository.deleteAll();
        schoolRepository.deleteAll();
    }
}
