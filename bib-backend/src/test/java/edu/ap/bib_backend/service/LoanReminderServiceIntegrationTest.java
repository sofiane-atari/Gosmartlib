package edu.ap.bib_backend.service;

import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.entity.BookCopy;
import edu.ap.bib_backend.jpa.entity.Campus;
import edu.ap.bib_backend.jpa.entity.Loan;
import edu.ap.bib_backend.jpa.entity.RoleEntity;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.enums.Role;
import edu.ap.bib_backend.jpa.repository.BookCopyRepository;
import edu.ap.bib_backend.jpa.repository.BookRepository;
import edu.ap.bib_backend.jpa.repository.CampusRepository;
import edu.ap.bib_backend.jpa.repository.LoanRepository;
import edu.ap.bib_backend.jpa.repository.RoleEntityRepository;
import edu.ap.bib_backend.jpa.repository.SchoolRepository;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import edu.ap.bib_backend.service.oneroster.ExternalUserDisplayService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LoanReminderServiceIntegrationTest {

    private static final ZoneId BRUSSELS_ZONE = ZoneId.of("Europe/Brussels");

    @Autowired
    private LoanReminderService loanReminderService;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookCopyRepository bookCopyRepository;

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private CampusRepository campusRepository;

    @Autowired
    private RoleEntityRepository roleEntityRepository;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private SmartschoolTokenRefreshService smartschoolTokenRefreshService;

    @MockitoBean
    private SmartschoolMessageService smartschoolMessageService;

    @MockitoBean
    private ExternalUserDisplayService externalUserDisplayService;

    @Test
    void Given_LoanDueIn2Days_When_SendUpcomingReturnReminders_Then_SendsMessageViaSmartschool() {
        School school = new School();
        school.setDomainName("reminder-test.be");
        school.setName("Reminder Test School");
        school = schoolRepository.save(school);

        Campus campus = new Campus();
        campus.setName("Test Campus");
        campus.setSchool(school);
        campus = campusRepository.save(campus);

        RoleEntity role = new RoleEntity();
        role.setRole(Role.STUDENT);
        roleEntityRepository.save(role);

        User user = new User();
        user.setId("reminder-user-1");
        user.setRole(role);
        user.setSchool(school);
        user.setCampus(campus);
        user.setOneRosterSourcedId("sourced-user-1");
        userRepository.save(user);

        Book book = new Book();
        book.setTitle("Reminder Test Book");
        book.setCampus(campus);
        book = bookRepository.save(book);

        BookCopy copy = new BookCopy();
        copy.setBook(book);
        copy.setCampus(campus);
        copy = bookCopyRepository.save(copy);

        Loan loan = new Loan();
        loan.setUser(user);
        loan.setCampus(campus);
        loan.setLoanDate(LocalDate.now(BRUSSELS_ZONE));
        loan.setReturnDate(LocalDate.now(BRUSSELS_ZONE).plusDays(2));
        loan.getCopies().add(copy);
        loanRepository.save(loan);

        entityManager.flush();
        entityManager.clear();

        when(smartschoolTokenRefreshService.getValidSmartschoolAccessToken(user.getId())).thenReturn("test-token");
        when(externalUserDisplayService.getDisplayName(school.getDomainName(), user.getOneRosterSourcedId())).thenReturn("Test User");

        assertDoesNotThrow(() -> loanReminderService.sendUpcomingReturnReminders());

        verify(smartschoolMessageService).sendMessage(eq("test-token"), eq("Retourdatum nadert"), anyString());
    }

    @Test
    void Given_LoanOverdueLessThan1Week_When_SendOverdueUserReminders_Then_SendsMessageViaSmartschool() {
        School school = new School();
        school.setDomainName("overdue-test.be");
        school.setName("Overdue Test School");
        school = schoolRepository.save(school);

        Campus campus = new Campus();
        campus.setName("Test Campus");
        campus.setSchool(school);
        campus = campusRepository.save(campus);

        RoleEntity overdueRole = new RoleEntity();
        overdueRole.setRole(Role.STUDENT);
        roleEntityRepository.save(overdueRole);

        User user = new User();
        user.setId("overdue-user-1");
        user.setRole(overdueRole);
        user.setSchool(school);
        user.setCampus(campus);
        user.setOneRosterSourcedId("sourced-overdue");
        userRepository.save(user);

        Book book = new Book();
        book.setTitle("Overdue Test Book");
        book.setCampus(campus);
        book = bookRepository.save(book);

        BookCopy copy = new BookCopy();
        copy.setBook(book);
        copy.setCampus(campus);
        copy = bookCopyRepository.save(copy);

        Loan loan = new Loan();
        loan.setUser(user);
        loan.setCampus(campus);
        loan.setLoanDate(LocalDate.now(BRUSSELS_ZONE).minusDays(10));
        loan.setReturnDate(LocalDate.now(BRUSSELS_ZONE).minusDays(3));
        loan.getCopies().add(copy);
        loanRepository.save(loan);

        entityManager.flush();
        entityManager.clear();

        when(smartschoolTokenRefreshService.getValidSmartschoolAccessToken(user.getId())).thenReturn("overdue-token");
        when(externalUserDisplayService.getDisplayName(school.getDomainName(), user.getOneRosterSourcedId())).thenReturn("Overdue User");

        assertDoesNotThrow(() -> loanReminderService.sendOverdueUserReminders());

        verify(smartschoolMessageService).sendMessage(eq("overdue-token"), eq("Boek(en) te laat teruggebracht"), anyString());
    }

    @Test
    void Given_LoanOverdue1WeekWithLibrarian_When_SendOverdueLibrarianAlerts_Then_SendsAlertAndSavesNotification() {
        School school = new School();
        school.setDomainName("librarian-alert-test.be");
        school.setName("Alert Test School");
        school = schoolRepository.save(school);

        Campus campus = new Campus();
        campus.setName("Test Campus");
        campus.setSchool(school);
        campus = campusRepository.save(campus);

        RoleEntity borrowerRole = new RoleEntity();
        borrowerRole.setRole(Role.STUDENT);
        roleEntityRepository.save(borrowerRole);

        RoleEntity librarianRole = new RoleEntity();
        librarianRole.setRole(Role.LIBRARIAN);
        roleEntityRepository.save(librarianRole);

        User user = new User();
        user.setId("alert-borrower-1");
        user.setRole(borrowerRole);
        user.setSchool(school);
        user.setCampus(campus);
        user.setOneRosterSourcedId("sourced-borrower");
        userRepository.save(user);

        User librarian = new User();
        librarian.setId("alert-librarian-1");
        librarian.setRole(librarianRole);
        librarian.setSchool(school);
        librarian.setCampus(campus);
        userRepository.save(librarian);

        Book book = new Book();
        book.setTitle("Alert Test Book");
        book.setCampus(campus);
        book = bookRepository.save(book);

        BookCopy copy = new BookCopy();
        copy.setBook(book);
        copy.setCampus(campus);
        copy = bookCopyRepository.save(copy);

        Loan loan = new Loan();
        loan.setUser(user);
        loan.setCampus(campus);
        loan.setLibrarianId("alert-librarian-1");
        loan.setLoanDate(LocalDate.now(BRUSSELS_ZONE).minusDays(14));
        loan.setReturnDate(LocalDate.now(BRUSSELS_ZONE).minusDays(10));
        loan.getCopies().add(copy);
        loanRepository.save(loan);

        entityManager.flush();
        entityManager.clear();

        when(smartschoolTokenRefreshService.getValidSmartschoolAccessToken("alert-librarian-1")).thenReturn("lib-token");
        when(externalUserDisplayService.getDisplayName(school.getDomainName(), user.getOneRosterSourcedId())).thenReturn("Borrower User");

        assertDoesNotThrow(() -> loanReminderService.sendOverdueLibrarianAlerts());

        verify(smartschoolMessageService).sendMessage(eq("lib-token"), eq("Boek(en) meer dan een week te laat"), anyString());
    }

    @Test
    void Given_NoMatchingLoans_When_SendAllReminders_Then_NoMessagesSent() {
        assertDoesNotThrow(() -> loanReminderService.sendUpcomingReturnReminders());
        assertDoesNotThrow(() -> loanReminderService.sendOverdueUserReminders());
        assertDoesNotThrow(() -> loanReminderService.sendOverdueLibrarianAlerts());

        verify(smartschoolMessageService, never()).sendMessage(anyString(), anyString(), anyString());
    }
}
