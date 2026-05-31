package edu.ap.bib_backend.service;

import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.entity.BookCopy;
import edu.ap.bib_backend.jpa.entity.Loan;
import edu.ap.bib_backend.jpa.entity.School;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.repository.LoanRepository;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import edu.ap.bib_backend.service.message.SmartschoolMessageTemplateService;
import edu.ap.bib_backend.service.oneroster.ExternalUserDisplayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanReminderServiceTest {

    private static final ZoneId BRUSSELS_ZONE = ZoneId.of("Europe/Brussels");

    @Mock
    private LoanRepository loanRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SmartschoolTokenRefreshService smartschoolTokenRefreshService;
    @Mock
    private SmartschoolMessageService smartschoolMessageService;
    @Mock
    private ExternalUserDisplayService externalUserDisplayService;
    @Mock
    private TemplateRenderer templateRenderer;
    @Mock
    private SmartschoolMessageTemplateService smartschoolMessageTemplateService;

    @Captor
    private ArgumentCaptor<String> templatePathCaptor;
    @Captor
    private ArgumentCaptor<Map<String, String>> placeholdersCaptor;

    private LoanReminderService loanReminderService;
    private School school;
    private User user;
    private User librarian;
    private Book book;
    private BookCopy copy;
    private Loan loan;

    @BeforeEach
    void setUp() {
        loanReminderService = new LoanReminderService(
                loanRepository, userRepository,
                smartschoolTokenRefreshService, smartschoolMessageService,
                externalUserDisplayService, templateRenderer,
                smartschoolMessageTemplateService,
                "http://localhost:4200"
        );

        lenient().when(smartschoolMessageTemplateService.renderLoanBookList(anyList(), anyString()))
                .thenReturn("<div>Test Boek Test Auteur</div>");

        school = new School();
        school.setId(1L);
        school.setDomainName("testschool.be");

        user = new User();
        user.setId("user123");
        user.setSchool(school);
        user.setOneRosterSourcedId("sourced-user-123");

        librarian = new User();
        librarian.setId("lib456");
        librarian.setSchool(school);

        book = new Book();
        book.setId(1L);
        book.setTitle("Test Boek");
        book.setAuthor("Test Auteur");

        copy = new BookCopy();
        copy.setId(1L);
        copy.setBook(book);

        loan = new Loan();
        loan.setId(100L);
        loan.setUser(user);
        loan.setLoanDate(LocalDate.now(BRUSSELS_ZONE).minusDays(5));
        loan.setReturnDate(LocalDate.now(BRUSSELS_ZONE).plusDays(2));
        loan.setCopy(copy);
    }

    @Test
    void Given_LoansDueIn2Days_When_SendUpcomingReturnReminders_Then_SendsReminderForEachLoan() {
        when(loanRepository.findByReturnedDateIsNullAndReturnDate(any())).thenReturn(List.of(loan));
        when(smartschoolTokenRefreshService.getValidSmartschoolAccessToken(user.getId())).thenReturn("token-123");
        when(externalUserDisplayService.getDisplayName(school.getDomainName(), user.getOneRosterSourcedId())).thenReturn("Jan Janssens");
        when(templateRenderer.render(anyString(), any())).thenReturn("<html>upcoming</html>");

        loanReminderService.sendUpcomingReturnReminders();

        verify(smartschoolMessageService).sendMessage("token-123", "Retourdatum nadert", "<html>upcoming</html>");
    }

    @Test
    void Given_NoLoansDueIn2Days_When_SendUpcomingReturnReminders_Then_NoReminderSent() {
        when(loanRepository.findByReturnedDateIsNullAndReturnDate(any())).thenReturn(List.of());

        loanReminderService.sendUpcomingReturnReminders();

        verify(smartschoolMessageService, never()).sendMessage(anyString(), anyString(), anyString());
    }

    @Test
    void Given_OneLoanFailsAndAnotherSucceeds_When_SendUpcomingReturnReminders_Then_ContinuesWithNext() {
        School otherSchool = new School();
        otherSchool.setId(2L);
        otherSchool.setDomainName("other.be");

        User otherUser = new User();
        otherUser.setId("other456");
        otherUser.setSchool(otherSchool);
        otherUser.setOneRosterSourcedId("sourced-other");

        Loan otherLoan = new Loan();
        otherLoan.setId(200L);
        otherLoan.setUser(otherUser);
        otherLoan.setLoanDate(LocalDate.now(BRUSSELS_ZONE).minusDays(3));
        otherLoan.setReturnDate(LocalDate.now(BRUSSELS_ZONE).plusDays(2));
        otherLoan.setCopy(copy);

        when(loanRepository.findByReturnedDateIsNullAndReturnDate(any())).thenReturn(List.of(loan, otherLoan));
        when(smartschoolTokenRefreshService.getValidSmartschoolAccessToken(user.getId())).thenReturn(null);
        when(smartschoolTokenRefreshService.getValidSmartschoolAccessToken(otherUser.getId())).thenReturn("token-other");
        when(externalUserDisplayService.getDisplayName(otherSchool.getDomainName(), otherUser.getOneRosterSourcedId())).thenReturn("Other User");
        when(templateRenderer.render(anyString(), any())).thenReturn("<html>ok</html>");

        loanReminderService.sendUpcomingReturnReminders();

        verify(smartschoolMessageService, times(1)).sendMessage(eq("token-other"), eq("Retourdatum nadert"), anyString());
    }

    @Test
    void Given_LoansOverdueLessThan1Week_When_SendOverdueUserReminders_Then_SendsReminderForEachLoan() {
        loan.setReturnDate(LocalDate.now(BRUSSELS_ZONE).minusDays(3));

        when(loanRepository.findByReturnedDateIsNullAndReturnDateBeforeAndReturnDateGreaterThanEqual(any(), any()))
                .thenReturn(List.of(loan));
        when(smartschoolTokenRefreshService.getValidSmartschoolAccessToken(user.getId())).thenReturn("token-123");
        when(externalUserDisplayService.getDisplayName(school.getDomainName(), user.getOneRosterSourcedId())).thenReturn("Jan Janssens");
        when(templateRenderer.render(anyString(), any())).thenReturn("<html>overdue</html>");

        loanReminderService.sendOverdueUserReminders();

        verify(smartschoolMessageService).sendMessage("token-123", "Boek(en) te laat teruggebracht", "<html>overdue</html>");
    }

    @Test
    void Given_NoOverdueLoans_When_SendOverdueUserReminders_Then_NoReminderSent() {
        when(loanRepository.findByReturnedDateIsNullAndReturnDateBeforeAndReturnDateGreaterThanEqual(any(), any()))
                .thenReturn(List.of());

        loanReminderService.sendOverdueUserReminders();

        verify(smartschoolMessageService, never()).sendMessage(anyString(), anyString(), anyString());
    }

    @Test
    void Given_LoansOverdue1WeekWithoutNotification_When_SendOverdueLibrarianAlerts_Then_SendsAlertAndSetsNotifiedAt() {
        loan.setReturnDate(LocalDate.now(BRUSSELS_ZONE).minusDays(10));
        loan.setLibrarianId("lib456");

        when(loanRepository.findOverdueLoansWithoutLibrarianNotification(any())).thenReturn(List.of(loan));
        when(userRepository.findById("lib456")).thenReturn(Optional.of(librarian));
        when(smartschoolTokenRefreshService.getValidSmartschoolAccessToken("lib456")).thenReturn("token-lib");
        when(externalUserDisplayService.getDisplayName(school.getDomainName(), user.getOneRosterSourcedId())).thenReturn("Jan Janssens");
        when(templateRenderer.render(anyString(), any())).thenReturn("<html>alert</html>");

        loanReminderService.sendOverdueLibrarianAlerts();

        verify(smartschoolMessageService).sendMessage("token-lib", "Boek(en) meer dan een week te laat", "<html>alert</html>");
        verify(loanRepository).save(loan);
        assertNotNull(loan.getLibrarianNotifiedAt());
    }

    @Test
    void Given_LoanWithoutLibrarianId_When_SendOverdueLibrarianAlerts_Then_SkipsLoan() {
        loan.setReturnDate(LocalDate.now(BRUSSELS_ZONE).minusDays(10));
        loan.setLibrarianId(null);

        when(loanRepository.findOverdueLoansWithoutLibrarianNotification(any())).thenReturn(List.of(loan));

        loanReminderService.sendOverdueLibrarianAlerts();

        verify(userRepository, never()).findById(anyString());
        verify(smartschoolMessageService, never()).sendMessage(anyString(), anyString(), anyString());
    }

    @Test
    void Given_LibrarianNotFound_When_SendLibrarianAlert_Then_Skips() {
        loan.setReturnDate(LocalDate.now(BRUSSELS_ZONE).minusDays(10));
        loan.setLibrarianId("lib456");

        when(loanRepository.findOverdueLoansWithoutLibrarianNotification(any())).thenReturn(List.of(loan));
        when(userRepository.findById("lib456")).thenReturn(Optional.empty());

        loanReminderService.sendOverdueLibrarianAlerts();

        verify(smartschoolMessageService, never()).sendMessage(anyString(), anyString(), anyString());
    }

    @Test
    void Given_LibrarianHasNoToken_When_SendLibrarianAlert_Then_Skips() {
        loan.setReturnDate(LocalDate.now(BRUSSELS_ZONE).minusDays(10));
        loan.setLibrarianId("lib456");

        when(loanRepository.findOverdueLoansWithoutLibrarianNotification(any())).thenReturn(List.of(loan));
        when(userRepository.findById("lib456")).thenReturn(Optional.of(librarian));
        when(smartschoolTokenRefreshService.getValidSmartschoolAccessToken("lib456")).thenReturn(null);

        loanReminderService.sendOverdueLibrarianAlerts();

        verify(smartschoolMessageService, never()).sendMessage(anyString(), anyString(), anyString());
    }

    @Test
    void Given_NoOverdueLoansWithoutNotification_When_SendOverdueLibrarianAlerts_Then_NoAlertSent() {
        when(loanRepository.findOverdueLoansWithoutLibrarianNotification(any())).thenReturn(List.of());

        loanReminderService.sendOverdueLibrarianAlerts();

        verify(smartschoolMessageService, never()).sendMessage(anyString(), anyString(), anyString());
    }

    @ParameterizedTest
    @CsvSource({
            "upcoming-return, Retourdatum nadert",
            "overdue-user, Boek(en) te laat teruggebracht",
            "unknown, Herinnering"
    })
    void Given_ValidLoan_When_SendReminderWithDifferentTemplates_Then_UsesCorrectTitle(String templateName, String expectedTitle) {
        when(smartschoolTokenRefreshService.getValidSmartschoolAccessToken(user.getId())).thenReturn("token-123");
        when(externalUserDisplayService.getDisplayName(anyString(), anyString())).thenReturn("Jan");
        when(templateRenderer.render(anyString(), any())).thenReturn("<html>content</html>");

        loanReminderService.sendReminder(loan, templateName);

        verify(smartschoolMessageService).sendMessage("token-123", expectedTitle, "<html>content</html>");
    }

    @Test
    void Given_LoanWithNullUser_When_SendReminder_Then_Skips() {
        loan.setUser(null);

        loanReminderService.sendReminder(loan, "upcoming-return");

        verify(smartschoolTokenRefreshService, never()).getValidSmartschoolAccessToken(anyString());
        verify(smartschoolMessageService, never()).sendMessage(anyString(), anyString(), anyString());
    }

    @Test
    void Given_UserWithoutSmartschoolToken_When_SendReminder_Then_Skips() {
        when(smartschoolTokenRefreshService.getValidSmartschoolAccessToken(user.getId())).thenReturn(null);

        loanReminderService.sendReminder(loan, "upcoming-return");

        verify(templateRenderer, never()).render(anyString(), any());
        verify(smartschoolMessageService, never()).sendMessage(anyString(), anyString(), anyString());
    }

    @Test
    void Given_EmptyRenderedTemplate_When_SendReminder_Then_Skips() {
        when(smartschoolTokenRefreshService.getValidSmartschoolAccessToken(user.getId())).thenReturn("token-123");
        when(externalUserDisplayService.getDisplayName(anyString(), anyString())).thenReturn("Jan");
        when(templateRenderer.render(anyString(), any())).thenReturn("");

        loanReminderService.sendReminder(loan, "upcoming-return");

        verify(smartschoolMessageService, never()).sendMessage(anyString(), anyString(), anyString());
    }

    @Test
    void Given_UpcomingReturnReminder_When_SendReminder_Then_RendersUpcomingTemplate() {
        when(smartschoolTokenRefreshService.getValidSmartschoolAccessToken(user.getId())).thenReturn("token-123");
        when(externalUserDisplayService.getDisplayName(school.getDomainName(), user.getOneRosterSourcedId())).thenReturn("Jan Janssens");
        when(templateRenderer.render(anyString(), any())).thenReturn("<html></html>");

        loanReminderService.sendReminder(loan, "upcoming-return");

        verify(templateRenderer).render(eq("templates/messages/upcoming-return.html"), any());
    }

    @Test
    void Given_OverdueUserReminder_When_SendReminder_Then_RendersOverdueUserTemplate() {
        loan.setReturnDate(LocalDate.now(BRUSSELS_ZONE).minusDays(3));

        when(smartschoolTokenRefreshService.getValidSmartschoolAccessToken(user.getId())).thenReturn("token-123");
        when(externalUserDisplayService.getDisplayName(school.getDomainName(), user.getOneRosterSourcedId())).thenReturn("Jan Janssens");
        when(templateRenderer.render(anyString(), any())).thenReturn("<html></html>");

        loanReminderService.sendReminder(loan, "overdue-user");

        verify(templateRenderer).render(eq("templates/messages/overdue-user.html"), any());
    }

    @Test
    void Given_UpcomingReturnReminder_When_SendReminder_Then_SetsCorrectPlaceholders() {
        when(smartschoolTokenRefreshService.getValidSmartschoolAccessToken(user.getId())).thenReturn("token-123");
        when(externalUserDisplayService.getDisplayName(school.getDomainName(), user.getOneRosterSourcedId())).thenReturn("Jan Janssens");
        when(templateRenderer.render(anyString(), placeholdersCaptor.capture())).thenReturn("<html></html>");

        loan.setReturnDate(LocalDate.now(BRUSSELS_ZONE).plusDays(2));

        loanReminderService.sendReminder(loan, "upcoming-return");

        Map<String, String> placeholders = placeholdersCaptor.getValue();
        assertEquals("Jan Janssens", placeholders.get("USER_NAME"));
        assertEquals("2", placeholders.get("DAYS_UNTIL_RETURN"));
        assertNotNull(placeholders.get("BOOKS_HTML"));
        assertTrue(placeholders.get("BOOKS_HTML").contains("Test Boek"));
        assertTrue(placeholders.get("BOOKS_HTML").contains("Test Auteur"));
    }

    @Test
    void Given_OverdueReminder_When_SendReminder_Then_SetsCorrectOverduePlaceholders() {
        loan.setReturnDate(LocalDate.now(BRUSSELS_ZONE).minusDays(3));

        when(smartschoolTokenRefreshService.getValidSmartschoolAccessToken(user.getId())).thenReturn("token-123");
        when(externalUserDisplayService.getDisplayName(school.getDomainName(), user.getOneRosterSourcedId())).thenReturn("Jan Janssens");
        when(templateRenderer.render(anyString(), placeholdersCaptor.capture())).thenReturn("<html></html>");

        loanReminderService.sendReminder(loan, "overdue-user");

        Map<String, String> placeholders = placeholdersCaptor.getValue();
        assertEquals("3", placeholders.get("DAYS_OVERDUE"));
        assertEquals("en", placeholders.get("DAYS_OVERDUE_SUFFIX"));
    }

    @Test
    void Given_SingleDayOverdue_When_SendReminder_Then_SuffixIsEmpty() {
        loan.setReturnDate(LocalDate.now(BRUSSELS_ZONE).minusDays(1));

        when(smartschoolTokenRefreshService.getValidSmartschoolAccessToken(user.getId())).thenReturn("token-123");
        when(externalUserDisplayService.getDisplayName(anyString(), anyString())).thenReturn("Jan");
        when(templateRenderer.render(anyString(), placeholdersCaptor.capture())).thenReturn("<html></html>");

        loanReminderService.sendReminder(loan, "overdue-user");

        assertEquals("", placeholdersCaptor.getValue().get("DAYS_OVERDUE_SUFFIX"));
    }

    @Test
    void Given_UserWithoutSchool_When_SendReminder_Then_FallsBackToUserId() {
        user.setSchool(null);
        user.setOneRosterSourcedId(null);

        when(smartschoolTokenRefreshService.getValidSmartschoolAccessToken(user.getId())).thenReturn("token-123");
        when(templateRenderer.render(anyString(), any())).thenReturn("<html></html>");

        loanReminderService.sendReminder(loan, "upcoming-return");

        verify(templateRenderer).render(anyString(), placeholdersCaptor.capture());
        assertEquals("user123", placeholdersCaptor.getValue().get("USER_NAME"));
    }

    @Test
    void Given_UserWithSchoolAndSourcedId_When_SendReminder_Then_UsesExternalDisplayName() {
        when(smartschoolTokenRefreshService.getValidSmartschoolAccessToken(user.getId())).thenReturn("token-123");
        when(externalUserDisplayService.getDisplayName(school.getDomainName(), user.getOneRosterSourcedId())).thenReturn("Jan Janssens");
        when(templateRenderer.render(anyString(), any())).thenReturn("<html></html>");

        loanReminderService.sendReminder(loan, "upcoming-return");

        verify(templateRenderer).render(anyString(), placeholdersCaptor.capture());
        assertEquals("Jan Janssens", placeholdersCaptor.getValue().get("USER_NAME"));
    }

    @Test
    void Given_UserWithoutOneRosterSourcedId_When_SendReminder_Then_FallsBackToUserId() {
        user.setOneRosterSourcedId(null);

        when(smartschoolTokenRefreshService.getValidSmartschoolAccessToken(user.getId())).thenReturn("token-123");
        when(templateRenderer.render(anyString(), any())).thenReturn("<html></html>");

        loanReminderService.sendReminder(loan, "upcoming-return");

        verify(templateRenderer).render(anyString(), placeholdersCaptor.capture());
        assertEquals("user123", placeholdersCaptor.getValue().get("USER_NAME"));
    }

    @Test
    void Given_OneDayOverdue_When_SendReminder_Then_SuffixIsEmpty() {
        loan.setReturnDate(LocalDate.now(BRUSSELS_ZONE).minusDays(1));

        when(smartschoolTokenRefreshService.getValidSmartschoolAccessToken(user.getId())).thenReturn("token-123");
        when(externalUserDisplayService.getDisplayName(anyString(), anyString())).thenReturn("Jan");
        when(templateRenderer.render(anyString(), placeholdersCaptor.capture())).thenReturn("<html></html>");

        loanReminderService.sendReminder(loan, "overdue-user");

        assertEquals("", placeholdersCaptor.getValue().get("DAYS_OVERDUE_SUFFIX"));
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 7})
    void Given_MultipleDaysOverdue_When_SendReminder_Then_SuffixIsEn(int daysOverdue) {
        loan.setReturnDate(LocalDate.now(BRUSSELS_ZONE).minusDays(daysOverdue));

        when(smartschoolTokenRefreshService.getValidSmartschoolAccessToken(user.getId())).thenReturn("token-123");
        when(externalUserDisplayService.getDisplayName(anyString(), anyString())).thenReturn("Jan");
        when(templateRenderer.render(anyString(), placeholdersCaptor.capture())).thenReturn("<html></html>");

        loanReminderService.sendReminder(loan, "overdue-user");

        assertEquals("en", placeholdersCaptor.getValue().get("DAYS_OVERDUE_SUFFIX"));
    }
}
