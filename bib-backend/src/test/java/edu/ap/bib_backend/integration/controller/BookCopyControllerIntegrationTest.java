package edu.ap.bib_backend.integration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ap.bib_backend.BibBackendApplication;
import edu.ap.bib_backend.dto.BookCopyConditionUpdateDTO;
import edu.ap.bib_backend.jpa.entity.*;
import edu.ap.bib_backend.jpa.enums.BookCondition;
import edu.ap.bib_backend.jpa.enums.Role;
import edu.ap.bib_backend.jpa.repository.*;
import edu.ap.bib_backend.jpa.repository.review.ReviewReportRepository;
import edu.ap.bib_backend.jpa.repository.review.ReviewRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = BibBackendApplication.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class BookCopyControllerIntegrationTest {

    private static final String USER_ID = "bookcopy-ctrl-user";

    private UsernamePasswordAuthenticationToken currentPrincipal;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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

    private School school;
    private Campus campus;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        school = new School();
        school.setDomainName("bookcopy-ctrl-test.be");
        school.setName("BookCopy Ctrl Test School");
        school = schoolRepository.save(school);

        campus = new Campus();
        campus.setName("BookCopy Ctrl Campus");
        campus.setSchool(school);
        campus.setAddress("Test Address");
        campus.setPostalCode(2000);
        campus = campusRepository.save(campus);

        RoleEntity role = getOrCreateRole(Role.LIBRARIAN);

        User user = new User();
        user.setId(USER_ID);
        user.setRole(role);
        user.setSchool(school);
        userRepository.save(user);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void givenCopyExists_whenUpdateCondition_thenReturnsUpdated() throws Exception {
        setAuthentication("ROLE_LIBRARIAN");
        BookCopy copy = createCopy();

        BookCopyConditionUpdateDTO request = new BookCopyConditionUpdateDTO(
                copy.getId(), null, BookCondition.DAMAGED);

        mockMvc.perform(patch("/book-copies/{copyId}/condition", copy.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .principal(currentPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.condition").value("DAMAGED"));
    }

    @Test
    void givenCopyExists_whenGetCopiesByBookIdForSchool_thenReturnsOk() throws Exception {
        setAuthentication("ROLE_LIBRARIAN");
        BookCopy copy = createCopy();

        mockMvc.perform(get("/book-copies/book/{bookId}/by-school", copy.getBook().getId())
                        .principal(currentPrincipal))
                .andExpect(status().isOk());
    }

    @Test
    void givenCopyExists_whenDeleteCopyAsAdmin_thenReturnsNoContent() throws Exception {
        setAuthentication("ROLE_ADMIN");
        BookCopy copy = createCopy();

        mockMvc.perform(delete("/book-copies/{copyId}", copy.getId())
                        .principal(currentPrincipal))
                .andExpect(status().isNoContent());
    }

    @Test
    void givenBookWithCopies_whenDeleteAllCopiesAsAdmin_thenReturnsNoContent() throws Exception {
        setAuthentication("ROLE_ADMIN");
        BookCopy copy = createCopy();

        mockMvc.perform(delete("/book-copies/book/{bookId}", copy.getBook().getId())
                        .principal(currentPrincipal))
                .andExpect(status().isNoContent());
    }

    private BookCopy createCopy() {
        Book book = new Book();
        book.setIsbn("9783000000001");
        book.setTitle("BookCopy Ctrl Book");
        book.setAuthor("Author");
        book.setPages(100);
        book.setReleaseYear(2024);
        book = bookRepository.save(book);

        BookCopy copy = new BookCopy();
        copy.setBook(book);
        copy.setCampus(campus);
        return bookCopyRepository.save(copy);
    }

    private void setAuthentication(String authority) {
        currentPrincipal = new UsernamePasswordAuthenticationToken(
                USER_ID, null,
                List.of(new SimpleGrantedAuthority(authority)));
        SecurityContextHolder.getContext().setAuthentication(currentPrincipal);
    }

    private RoleEntity getOrCreateRole(Role role) {
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
