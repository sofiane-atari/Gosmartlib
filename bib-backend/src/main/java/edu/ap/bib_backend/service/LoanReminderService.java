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

import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.entity.BookCopy;
import edu.ap.bib_backend.jpa.entity.Loan;
import edu.ap.bib_backend.jpa.entity.User;
import edu.ap.bib_backend.jpa.repository.LoanRepository;
import edu.ap.bib_backend.jpa.repository.UserRepository;
import edu.ap.bib_backend.service.message.HtmlMessageBuilder;
import edu.ap.bib_backend.service.oneroster.ExternalUserDisplayService;

@Service
public class LoanReminderService {

    private static final Logger log = LoggerFactory.getLogger(LoanReminderService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMMM yyyy",
            new Locale("nl", "BE"));
    private static final ZoneId BRUSSELS_ZONE = ZoneId.of("Europe/Brussels");

    private static final String BOOK_CARD_STYLE = "display: flex; align-items: center; padding: 12px; " +
            "margin: 0 0 10px 0; background-color: #faf5f7; " +
            "border-radius: 8px; border: 1px solid #f0e4e8;";
    private static final String COVER_WRAPPER_STYLE = "flex-shrink: 0; width: 56px; height: 80px; margin-right: 14px; "
            +
            "background-color: #f0e4e8; border-radius: 4px; overflow: hidden;";
    private static final String COVER_PLACEHOLDER_STYLE = "width: 100%; height: 100%; display: flex; align-items: center; "
            +
            "justify-content: center; color: #8a7580; font-size: 20px;";
    private static final String GENRE_BADGE_STYLE = "display: inline-block; padding: 1px 8px; margin: 0 4px 2px 0; " +
            "font-size: 11px; color: #8a1e41; background-color: #fdf0f5; " +
            "border-radius: 10px;";
    private static final String LINK_STYLE = "color: #1a0a10; text-decoration: none; font-weight: 600;";

    private final LoanRepository loanRepository;
    private final UserRepository userRepository;
    private final SmartschoolTokenRefreshService smartschoolTokenRefreshService;
    private final SmartschoolMessageService smartschoolMessageService;
    private final ExternalUserDisplayService externalUserDisplayService;
    private final TemplateRenderer templateRenderer;
    private final String frontendUrl;

    public LoanReminderService(
            LoanRepository loanRepository,
            UserRepository userRepository,
            SmartschoolTokenRefreshService smartschoolTokenRefreshService,
            SmartschoolMessageService smartschoolMessageService,
            ExternalUserDisplayService externalUserDisplayService,
            TemplateRenderer templateRenderer,
            @Value("${FRONTEND_URL:http://localhost:4200}") String frontendUrl) {
        this.loanRepository = loanRepository;
        this.userRepository = userRepository;
        this.smartschoolTokenRefreshService = smartschoolTokenRefreshService;
        this.smartschoolMessageService = smartschoolMessageService;
        this.externalUserDisplayService = externalUserDisplayService;
        this.templateRenderer = templateRenderer;
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
        String booksHtml = buildBooksHtml(loan);
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
        String booksHtml = buildBooksHtml(loan);
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

    private String buildBooksHtml(Loan loan) {
        if (loan.getCopy() == null ||
                loan.getCopy().getBook() == null) {
            return "";
        }
        Book book = loan.getCopy().getBook();

        StringBuilder sb = new StringBuilder();
        String bookUrl = frontendUrl + "/book/" + book.getId();

        sb.append("<div style=\"").append(BOOK_CARD_STYLE)
                .append("\" class=\"msg-book-card\">");

        sb.append("<a href=\"").append(HtmlMessageBuilder.escapeHtml(bookUrl))
                .append("\" class=\"msg-cover-link\" style=\"text-decoration: none; display: block;\">");
        sb.append("<div style=\"").append(COVER_WRAPPER_STYLE).append("\">");
        appendCoverImage(sb, book);
        sb.append("</div>");
        sb.append("</a>");

        sb.append("<div style=\"flex: 1; min-width: 0;\">");
        sb.append("<a href=\"").append(HtmlMessageBuilder.escapeHtml(bookUrl))
                .append("\" class=\"msg-book-link\" style=\"").append(LINK_STYLE).append("\">");
        sb.append("<p style=\"margin: 0 0 3px 0; font-size: 14px; ")
                .append("overflow: hidden; text-overflow: ellipsis; white-space: nowrap;\">");
        sb.append(HtmlMessageBuilder.escapeHtml(book.getTitle()));
        sb.append("</p>");
        sb.append("</a>");

        if (book.getAuthor() != null && !book.getAuthor().isBlank()) {
            sb.append("<p style=\"margin: 0 0 2px 0; font-size: 13px; color: #8a7580;\">");
            sb.append(HtmlMessageBuilder.escapeHtml(book.getAuthor()));
            sb.append("</p>");
        }

        appendGenres(sb, book);

        sb.append("</div>");
        sb.append("</div>");

        return sb.toString();
    }

    private void appendCoverImage(StringBuilder sb, Book book) {
        if (book.getImageUrl() != null && !book.getImageUrl().isBlank()) {
            sb.append("<img src=\"").append(HtmlMessageBuilder.escapeHtml(book.getImageUrl()))
                    .append("\" alt=\"").append(HtmlMessageBuilder.escapeHtml(book.getTitle()))
                    .append("\" style=\"width: 100%; height: 100%; object-fit: cover; display: block;\">");
        } else {
            sb.append("<div style=\"").append(COVER_PLACEHOLDER_STYLE).append("\">&#128218;</div>");
        }
    }

    private void appendGenres(StringBuilder sb, Book book) {
        if (book.getGenres() == null || book.getGenres().isEmpty())
            return;
        sb.append("<div style=\"margin-top: 4px;\">");
        for (var genre : book.getGenres()) {
            sb.append("<span style=\"").append(GENRE_BADGE_STYLE).append("\">");
            sb.append(HtmlMessageBuilder.escapeHtml(genre.getName()));
            sb.append("</span>");
        }
        sb.append("</div>");
    }

    private record BookGroup(Book book, long count) {
    }
}
