package edu.ap.bib_backend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.ap.bib_backend.jpa.entity.Loan;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.repository.LoanRepository;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import edu.ap.bib_backend.service.message.SmartschoolMessageTemplateService;
import edu.ap.bib_backend.service.oneroster.ExternalUserDisplayService;

@Service
public class LoanReminderService {

    private static final Logger log = LoggerFactory.getLogger(LoanReminderService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMMM yyyy",
            new Locale("nl", "BE"));
    private static final ZoneId BRUSSELS_ZONE = ZoneId.of("Europe/Brussels");

    private final LoanRepository loanRepository;
    private final UserRepository userRepository;
    private final SmartschoolTokenRefreshService smartschoolTokenRefreshService;
    private final SmartschoolMessageService smartschoolMessageService;
    private final ExternalUserDisplayService externalUserDisplayService;
    private final TemplateRenderer templateRenderer;
    private final SmartschoolMessageTemplateService smartschoolMessageTemplateService;
    private final String frontendUrl;

    public LoanReminderService(
            LoanRepository loanRepository,
            UserRepository userRepository,
            SmartschoolTokenRefreshService smartschoolTokenRefreshService,
            SmartschoolMessageService smartschoolMessageService,
            ExternalUserDisplayService externalUserDisplayService,
            TemplateRenderer templateRenderer,
            SmartschoolMessageTemplateService smartschoolMessageTemplateService,
            @Value("${FRONTEND_URL:http://localhost:4200}") String frontendUrl) {
        this.loanRepository = loanRepository;
        this.userRepository = userRepository;
        this.smartschoolTokenRefreshService = smartschoolTokenRefreshService;
        this.smartschoolMessageService = smartschoolMessageService;
        this.externalUserDisplayService = externalUserDisplayService;
        this.templateRenderer = templateRenderer;
        this.smartschoolMessageTemplateService = smartschoolMessageTemplateService;
        this.frontendUrl = frontendUrl;
    }

    @Transactional(readOnly = true)
    public void sendUpcomingReturnReminders() {
        LocalDate targetDate = LocalDate.now(BRUSSELS_ZONE).plusDays(2);
        log.info("Checking for loans due on {}", targetDate);

        List<Loan> loans = loanRepository.findByReturnedDateIsNullAndReturnDate(targetDate);
        log.info("Found {} loan(s) due in 2 days", loans.size());

        for (Loan loan : loans) {
            try {
                sendReminder(loan, "upcoming-return");
            } catch (Exception e) {
                log.warn("Failed to send upcoming return reminder for loan {}: {}", loan.getId(), e.getMessage());
            }
        }
    }

    @Transactional(readOnly = true)
    public void sendOverdueUserReminders() {
        LocalDate today = LocalDate.now(BRUSSELS_ZONE);
        LocalDate oneWeekAgo = today.minusDays(7);
        log.info("Checking for overdue loans (not yet 1 week overdue)");

        List<Loan> loans = loanRepository
                .findByReturnedDateIsNullAndReturnDateBeforeAndReturnDateGreaterThanEqual(today, oneWeekAgo);
        log.info("Found {} loan(s) overdue (less than 1 week)", loans.size());

        for (Loan loan : loans) {
            try {
                sendReminder(loan, "overdue-user");
            } catch (Exception e) {
                log.warn("Failed to send overdue reminder for loan {}: {}", loan.getId(), e.getMessage());
            }
        }
    }

    @Transactional
    public void sendOverdueLibrarianAlerts() {
        LocalDate oneWeekAgo = LocalDate.now(BRUSSELS_ZONE).minusDays(7);
        log.info("Checking for loans overdue by at least 1 week without librarian notification");

        List<Loan> loans = loanRepository.findOverdueLoansWithoutLibrarianNotification(oneWeekAgo);
        log.info("Found {} loan(s) overdue by 1+ week without librarian notification", loans.size());

        for (Loan loan : loans) {
            try {
                sendLibrarianAlert(loan);
            } catch (Exception e) {
                log.warn("Failed to send librarian alert for loan {}: {}", loan.getId(), e.getMessage());
            }
        }
    }

    @Transactional
    public void sendReminder(Loan loan, String templateName) {
        User user = loan.getUser();
        if (user == null || user.getId() == null) {
            log.warn("Loan {} has no user, skipping reminder", loan.getId());
            return;
        }

        String token = smartschoolTokenRefreshService.getValidSmartschoolAccessToken(user.getId());
        if (token == null || token.isBlank()) {
            log.warn("No valid Smartschool token for user {}, skipping reminder", user.getId());
            return;
        }

        String userDisplayName = resolveUserDisplayName(user);
        String booksHtml = smartschoolMessageTemplateService.renderLoanBookList(List.of(loan), frontendUrl);
        long daysOverdue = loan.getReturnDate() != null
                ? LocalDate.now(BRUSSELS_ZONE).toEpochDay() - loan.getReturnDate().toEpochDay()
                : 0;

        String title = switch (templateName) {
            case "upcoming-return" -> "Retourdatum nadert";
            case "overdue-user" -> "Boek(en) te laat teruggebracht";
            default -> "Herinnering";
        };

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("TITLE", title);
        placeholders.put("USER_NAME", userDisplayName);
        placeholders.put("LOAN_DATE", loan.getLoanDate() != null ? loan.getLoanDate().format(DATE_FORMATTER) : "-");
        placeholders.put("RETURN_DATE",
                loan.getReturnDate() != null ? loan.getReturnDate().format(DATE_FORMATTER) : "-");
        placeholders.put("BOOK_COUNT", String.valueOf(loan.getCopy() != null ? 1 : 0));
        placeholders.put("BOOKS_HTML", booksHtml);
        placeholders.put("FRONTEND_URL", frontendUrl);

        if ("upcoming-return".equals(templateName)) {
            long daysUntilReturn = LocalDate.now(BRUSSELS_ZONE).toEpochDay() - loan.getReturnDate().toEpochDay();
            daysUntilReturn = Math.abs(daysUntilReturn);
            placeholders.put("DAYS_UNTIL_RETURN", String.valueOf(daysUntilReturn));
            placeholders.put("DAYS_UNTIL_RETURN_SUFFIX", daysUntilReturn != 1 ? "en" : "");
        }

        if ("overdue-user".equals(templateName)) {
            placeholders.put("DAYS_OVERDUE", String.valueOf(daysOverdue));
            placeholders.put("DAYS_OVERDUE_SUFFIX", daysOverdue != 1 ? "en" : "");
        }

        String html = templateRenderer.render("templates/messages/" + templateName + ".html", placeholders);
        if (html.isBlank()) {
            log.warn("Empty HTML rendered for template {}, skipping", templateName);
            return;
        }

        smartschoolMessageService.sendMessage(token, title, html);
        log.info("Sent {} reminder for loan {} to user {}", templateName, loan.getId(), user.getId());
    }

    protected void sendLibrarianAlert(Loan loan) {
        if (loan.getLibrarianId() == null || loan.getLibrarianId().isBlank()) {
            log.warn("Loan {} has no librarianId, cannot send librarian alert", loan.getId());
            return;
        }

        User librarian = userRepository.findById(loan.getLibrarianId()).orElse(null);
        if (librarian == null) {
            log.warn("Librarian with id {} not found for loan {}", loan.getLibrarianId(), loan.getId());
            return;
        }

        String token = smartschoolTokenRefreshService.getValidSmartschoolAccessToken(loan.getLibrarianId());
        if (token == null || token.isBlank()) {
            log.warn("No valid Smartschool token for librarian {}, skipping alert", loan.getLibrarianId());
            return;
        }

        User borrower = loan.getUser();
        String userDisplayName = resolveUserDisplayName(borrower);
        String booksHtml = smartschoolMessageTemplateService.renderLoanBookList(List.of(loan), frontendUrl);
        long daysOverdue = loan.getReturnDate() != null
                ? LocalDate.now(BRUSSELS_ZONE).toEpochDay() - loan.getReturnDate().toEpochDay()
                : 0;

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("TITLE", "Boek(en) meer dan een week te laat");
        placeholders.put("USER_NAME", userDisplayName);
        placeholders.put("LOAN_DATE", loan.getLoanDate() != null ? loan.getLoanDate().format(DATE_FORMATTER) : "-");
        placeholders.put("RETURN_DATE",
                loan.getReturnDate() != null ? loan.getReturnDate().format(DATE_FORMATTER) : "-");
        placeholders.put("DAYS_OVERDUE", String.valueOf(daysOverdue));
        placeholders.put("DAYS_OVERDUE_SUFFIX", daysOverdue != 1 ? "en" : "");
        placeholders.put("BOOKS_HTML", booksHtml);
        placeholders.put("FRONTEND_URL", frontendUrl);

        String html = templateRenderer.render("templates/messages/overdue-librarian.html", placeholders);
        if (html.isBlank()) {
            log.warn("Empty HTML rendered for librarian alert template, skipping");
            return;
        }

        smartschoolMessageService.sendMessage(token, "Boek(en) meer dan een week te laat", html);
        loan.setLibrarianNotifiedAt(LocalDateTime.now(BRUSSELS_ZONE));
        loanRepository.save(loan);
        log.info("Sent librarian alert for loan {} to librarian {}", loan.getId(), loan.getLibrarianId());
    }

    private String resolveUserDisplayName(User user) {
        if (user == null)
            return "Gebruiker";
        if (user.getSchool() == null || user.getSchool().getDomainName() == null
                || user.getOneRosterSourcedId() == null) {
            return user.getOneRosterSourcedId() != null ? user.getOneRosterSourcedId() : user.getId();
        }
        return externalUserDisplayService.getDisplayName(
                user.getSchool().getDomainName(), user.getOneRosterSourcedId());
    }

}
