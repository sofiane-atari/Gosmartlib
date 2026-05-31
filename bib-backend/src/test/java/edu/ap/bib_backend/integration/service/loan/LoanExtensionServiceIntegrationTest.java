package edu.ap.bib_backend.integration.service.loan;

import edu.ap.bib_backend.BibBackendApplication;
import edu.ap.bib_backend.dto.loan.LoanDTO;
import edu.ap.bib_backend.dto.loan.LoanExtensionRequestDTO;
import edu.ap.bib_backend.exception.ResourceNotFoundException;
import edu.ap.bib_backend.jpa.entity.*;
import edu.ap.bib_backend.jpa.enums.LoanExtensionType;
import edu.ap.bib_backend.jpa.enums.Role;
import edu.ap.bib_backend.jpa.repository.*;
import edu.ap.bib_backend.service.LoanExtensionService;
import edu.ap.bib_backend.service.user.CurrentUserService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = BibBackendApplication.class)
@ActiveProfiles("test")
@Transactional
class LoanExtensionServiceIntegrationTest {

    private static final ZoneId APPLICATION_ZONE = ZoneId.of("Europe/Brussels");

    @Autowired
    private LoanExtensionService loanExtensionService;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private LoanExtensionRepository loanExtensionRepository;

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

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private CurrentUserService currentUserService;

    private RoleEntity studentRole;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        RoleEntity existing = roleEntityRepository.findByRole(Role.STUDENT).orElse(null);
        if (existing != null) {
            studentRole = existing;
        } else {
            try {
                jdbcTemplate.update("INSERT INTO roles (role) VALUES ('STUDENT')");
            } catch (Exception e) {
                // role might already exist from a previous test
            }
            studentRole = roleEntityRepository.findByRole(Role.STUDENT)
                    .orElseThrow(() -> new IllegalStateException("Failed to create STUDENT role"));
        }
    }

    @Test
    void Given_EligibleLoan_When_ExtendLoanWithOneWeek_Then_ReturnDateExtended() {
        School school = createSchool("loan-ext-1.be");
        Campus campus = createCampus("Ext Campus", school);
        Loan loan = createActiveLoan(campus, school, LocalDate.now(APPLICATION_ZONE).plusDays(7));
        Authentication auth = mockAuth("loan-ext-user");
        when(currentUserService.getCurrentUserSchool(auth)).thenReturn(school);
        when(currentUserService.extractCurrentUserId(auth)).thenReturn("loan-ext-user");

        LocalDate expectedReturnDate = loan.getReturnDate().plusWeeks(1);
        LoanExtensionRequestDTO request = new LoanExtensionRequestDTO(LoanExtensionType.ONE_WEEK, null);
        LoanDTO result = loanExtensionService.extendLoan(loan.getId(), request, auth);

        assertNotNull(result);
        assertEquals(expectedReturnDate.toString(), result.returnDate());
    }

    @Test
    void Given_EligibleLoan_When_ExtendLoanWithCustomDate_Then_ReturnDateUpdated() {
        School school = createSchool("loan-ext-2.be");
        Campus campus = createCampus("Ext Campus 2", school);
        Loan loan = createActiveLoan(campus, school, LocalDate.now(APPLICATION_ZONE).plusDays(7));
        Authentication auth = mockAuth("loan-ext-user-2");
        when(currentUserService.getCurrentUserSchool(auth)).thenReturn(school);
        when(currentUserService.extractCurrentUserId(auth)).thenReturn("loan-ext-user-2");

        LocalDate futureDate = LocalDate.now(APPLICATION_ZONE).plusDays(30);
        LoanExtensionRequestDTO request = new LoanExtensionRequestDTO(LoanExtensionType.CUSTOM_DATE, futureDate.toString());
        LoanDTO result = loanExtensionService.extendLoan(loan.getId(), request, auth);

        assertEquals(futureDate.toString(), result.returnDate());
    }

    @Test
    void Given_ReturnedLoan_When_ExtendLoan_Then_ThrowsIllegalState() {
        School school = createSchool("loan-ext-3.be");
        Campus campus = createCampus("Ext Campus 3", school);
        Loan loan = createReturnedLoan(campus, school);
        Authentication auth = mockAuth("loan-ext-user-3");
        when(currentUserService.getCurrentUserSchool(auth)).thenReturn(school);

        LoanExtensionRequestDTO request = new LoanExtensionRequestDTO(LoanExtensionType.ONE_WEEK, null);
        assertThrows(IllegalStateException.class, () -> loanExtensionService.extendLoan(loan.getId(), request, auth));
    }

    @Test
    void Given_LoanWithoutReturnDate_When_ExtendLoan_Then_ThrowsIllegalState() {
        School school = createSchool("loan-ext-4.be");
        Campus campus = createCampus("Ext Campus 4", school);
        Loan loan = createLoanWithoutReturnDate(campus, school);
        Authentication auth = mockAuth("loan-ext-user-4");
        when(currentUserService.getCurrentUserSchool(auth)).thenReturn(school);

        LoanExtensionRequestDTO request = new LoanExtensionRequestDTO(LoanExtensionType.ONE_WEEK, null);
        assertThrows(IllegalStateException.class, () -> loanExtensionService.extendLoan(loan.getId(), request, auth));
    }

    @Test
    void Given_LoanAtMaxExtensions_When_ExtendLoanAgain_Then_ThrowsIllegalState() {
        School school = createSchool("loan-ext-5.be");
        Campus campus = createCampus("Ext Campus 5", school);
        Loan loan = createActiveLoan(campus, school, LocalDate.now(APPLICATION_ZONE).plusDays(7));
        Authentication auth = mockAuth("loan-ext-user-5");
        when(currentUserService.getCurrentUserSchool(auth)).thenReturn(school);
        when(currentUserService.extractCurrentUserId(auth)).thenReturn("loan-ext-user-5");

        LoanExtensionRequestDTO request = new LoanExtensionRequestDTO(LoanExtensionType.ONE_WEEK, null);
        loanExtensionService.extendLoan(loan.getId(), request, auth);
        loanExtensionService.extendLoan(loan.getId(), request, auth);
        entityManager.flush();

        assertThrows(IllegalStateException.class, () -> loanExtensionService.extendLoan(loan.getId(), request, auth));
    }

    @Test
    void Given_LoanFromOtherSchool_When_ExtendLoan_Then_ThrowsAccessDenied() {
        School school1 = createSchool("loan-ext-6a.be");
        School school2 = createSchool("loan-ext-6b.be");
        Campus campus1 = createCampus("Ext Campus 6", school1);
        Loan loan = createActiveLoan(campus1, school1, LocalDate.now(APPLICATION_ZONE).plusDays(7));
        Authentication auth = mockAuth("loan-ext-user-6");
        when(currentUserService.getCurrentUserSchool(auth)).thenReturn(school2);

        LoanExtensionRequestDTO request = new LoanExtensionRequestDTO(LoanExtensionType.ONE_WEEK, null);
        assertThrows(AccessDeniedException.class, () -> loanExtensionService.extendLoan(loan.getId(), request, auth));
    }

    @Test
    void Given_NonExistentLoan_When_ExtendLoan_Then_ThrowsResourceNotFound() {
        Authentication auth = mockAuth("loan-ext-user-7");
        School school = createSchool("loan-ext-7.be");
        when(currentUserService.getCurrentUserSchool(auth)).thenReturn(school);

        LoanExtensionRequestDTO request = new LoanExtensionRequestDTO(LoanExtensionType.ONE_WEEK, null);
        assertThrows(ResourceNotFoundException.class, () -> loanExtensionService.extendLoan(99999L, request, auth));
    }

    @Test
    void Given_EligibleLoan_When_ExtendLoanWithPastCustomDate_Then_ThrowsIllegalArgument() {
        School school = createSchool("loan-ext-8.be");
        Campus campus = createCampus("Ext Campus 8", school);
        Loan loan = createActiveLoan(campus, school, LocalDate.now(APPLICATION_ZONE).plusDays(7));
        Authentication auth = mockAuth("loan-ext-user-8");
        when(currentUserService.getCurrentUserSchool(auth)).thenReturn(school);

        LocalDate pastDate = LocalDate.now(APPLICATION_ZONE).minusDays(1);
        LoanExtensionRequestDTO request = new LoanExtensionRequestDTO(LoanExtensionType.CUSTOM_DATE, pastDate.toString());

        assertThrows(IllegalArgumentException.class, () -> loanExtensionService.extendLoan(loan.getId(), request, auth));
    }

    @Test
    void Given_EligibleLoan_When_ExtendLoanWithReturnDateBeforeOld_Then_ThrowsIllegalArgument() {
        School school = createSchool("loan-ext-9.be");
        Campus campus = createCampus("Ext Campus 9", school);
        LocalDate futureDate = LocalDate.now(APPLICATION_ZONE).plusDays(30);
        Loan loan = createActiveLoan(campus, school, futureDate);
        Authentication auth = mockAuth("loan-ext-user-9");
        when(currentUserService.getCurrentUserSchool(auth)).thenReturn(school);

        LocalDate earlierDate = futureDate.minusDays(1);
        LoanExtensionRequestDTO request = new LoanExtensionRequestDTO(LoanExtensionType.CUSTOM_DATE, earlierDate.toString());

        assertThrows(IllegalArgumentException.class, () -> loanExtensionService.extendLoan(loan.getId(), request, auth));
    }

    @Test
    void Given_ExtensionRecord_When_LoanExtended_Then_ExtensionIsPersisted() {
        School school = createSchool("loan-ext-10.be");
        Campus campus = createCampus("Ext Campus 10", school);
        Loan loan = createActiveLoan(campus, school, LocalDate.now(APPLICATION_ZONE).plusDays(7));
        Authentication auth = mockAuth("loan-ext-user-10");
        when(currentUserService.getCurrentUserSchool(auth)).thenReturn(school);
        when(currentUserService.extractCurrentUserId(auth)).thenReturn("loan-ext-user-10");

        LoanExtensionRequestDTO request = new LoanExtensionRequestDTO(LoanExtensionType.ONE_WEEK, null);

        loanExtensionService.extendLoan(loan.getId(), request, auth);

        entityManager.flush();
        entityManager.clear();

        long extensionCount = loanExtensionRepository.countByLoanId(loan.getId());
        assertEquals(1, extensionCount);
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

    private Loan createActiveLoan(Campus campus, School school, LocalDate returnDate) {
        Book book = new Book();
        book.setIsbn("978" + System.nanoTime());
        book.setTitle("Loan Extension Book");
        book.setAuthor("Author");
        book.setPages(100);
        book.setReleaseYear(2024);
        book = bookRepository.save(book);

        BookCopy copy = new BookCopy();
        copy.setBook(book);
        copy.setCampus(campus);
        copy = bookCopyRepository.save(copy);

        User user = new User();
        user.setId("loan-ext-" + System.nanoTime());
        user.setRole(studentRole);
        user.setSchool(school);
        user = userRepository.save(user);

        Loan loan = new Loan();
        loan.setCopy(copy);
        loan.setUser(user);
        loan.setCampus(campus);
        loan.setLoanDate(LocalDate.now(APPLICATION_ZONE));
        loan.setReturnDate(returnDate);
        loan.setBatchId("batch-" + System.nanoTime());
        return loanRepository.save(loan);
    }

    private Loan createReturnedLoan(Campus campus, School school) {
        Loan loan = createActiveLoan(campus, school, LocalDate.now(APPLICATION_ZONE).plusDays(7));
        loan.setReturnedDate(LocalDate.now(APPLICATION_ZONE));
        return loanRepository.save(loan);
    }

    private Loan createLoanWithoutReturnDate(Campus campus, School school) {
        Loan loan = createActiveLoan(campus, school, LocalDate.now(APPLICATION_ZONE).plusDays(7));
        loan.setReturnDate(null);
        return loanRepository.save(loan);
    }

    private Authentication mockAuth(String userId) {
        Authentication auth = org.mockito.Mockito.mock(Authentication.class);
        when(auth.getName()).thenReturn(userId);
        return auth;
    }
}
