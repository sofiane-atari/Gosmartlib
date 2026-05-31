package edu.ap.bib_backend.service.message;

import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import edu.ap.bib_backend.dto.BookDTO;
import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.entity.BookCopy;
import edu.ap.bib_backend.jpa.entity.Genre;
import edu.ap.bib_backend.jpa.entity.Loan;
import edu.ap.bib_backend.service.TemplateRenderer;

@Service
public class SmartschoolMessageTemplateService {

    private static final int MAX_RECOMMENDATIONS = 6;
    private static final String TEMPLATE_ROOT = "templates/messages/";
    private static final String FRAGMENT_ROOT = TEMPLATE_ROOT + "fragments/";
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.of("nl", "BE"));

    private final TemplateRenderer templateRenderer;

    public SmartschoolMessageTemplateService(TemplateRenderer templateRenderer) {
        this.templateRenderer = templateRenderer;
    }

    public String renderLoanConfirmation(
            List<Loan> loans,
            List<BookDTO> recommendations,
            String frontendUrl
    ) {
        List<Loan> safeLoans = safeList(loans);
        Loan firstLoan = safeLoans.isEmpty() ? null : safeLoans.get(0);
        String title = safeLoans.size() == 1
                ? "Bevestiging nieuwe uitlening"
                : "Bevestiging nieuwe uitleningen";

        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("TITLE", escapeHtml(title));
        placeholders.put("LOAN_DATE", firstLoan != null && firstLoan.getLoanDate() != null
                ? escapeHtml(firstLoan.getLoanDate().format(DATE_FORMATTER))
                : "-");
        placeholders.put("RETURN_DATE", firstLoan != null && firstLoan.getReturnDate() != null
                ? escapeHtml(firstLoan.getReturnDate().format(DATE_FORMATTER))
                : "-");
        placeholders.put("BOOK_COUNT", String.valueOf(safeLoans.size()));
        placeholders.put("FRONTEND_URL", escapeHtml(frontendUrl));
        placeholders.put("BOOKS_SECTION_HTML", renderLoanBookListSection(safeLoans, frontendUrl));
        placeholders.put("RECOMMENDATIONS_HTML", renderRecommendations(recommendations, frontendUrl));

        return templateRenderer.render(TEMPLATE_ROOT + "loan-confirmation.html", placeholders);
    }

    public String renderBookAvailable(
            Book book,
            List<BookDTO> recommendations,
            String frontendUrl
    ) {
        if (book == null) {
            return "";
        }

        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("TITLE", "Boek beschikbaar");
        placeholders.put("BOOK_TITLE", escapeHtml(book.getTitle()));
        placeholders.put("BOOK_AUTHOR", escapeHtml(book.getAuthor()));
        placeholders.put("BOOK_AUTHOR_DISPLAY", displayWhen(StringUtils.hasText(book.getAuthor()), "block"));
        placeholders.put("BOOK_IMAGE_URL", escapeHtml(book.getImageUrl()));
        placeholders.put("BOOK_IMAGE_DISPLAY", displayWhen(StringUtils.hasText(book.getImageUrl()), "block"));
        placeholders.put("BOOK_PLACEHOLDER_DISPLAY", displayWhen(!StringUtils.hasText(book.getImageUrl()), "flex"));
        placeholders.put("GENRE_BADGES_HTML", renderBookAvailableGenreBadges(book.getGenres()));
        placeholders.put("GENRES_DISPLAY", displayWhen(hasGenres(book.getGenres()), "block"));
        placeholders.put("REVIEW_SCORE", renderReviewScoreValue(book.getReviewScore()));
        placeholders.put("REVIEW_SCORE_DISPLAY", displayWhen(hasReviewScore(book.getReviewScore()), "block"));
        placeholders.put("BOOK_DESCRIPTION", renderShortenedDescription(book.getDescription()));
        placeholders.put("DESCRIPTION_DISPLAY", displayWhen(StringUtils.hasText(book.getDescription()), "block"));
        placeholders.put("BOOK_URL", escapeHtml(frontendUrl + "/book/" + book.getId()));
        placeholders.put("RECOMMENDATIONS_HTML", renderRecommendations(recommendations, frontendUrl));

        return templateRenderer.render(TEMPLATE_ROOT + "book-available.html", placeholders);
    }

    public String renderLoanBookList(List<Loan> loans, String frontendUrl) {
        return groupBooksById(loans).values().stream()
                .map(group -> renderBookCard(group.book(), group.count(), frontendUrl))
                .collect(Collectors.joining());
    }

    private String renderLoanBookListSection(List<Loan> loans, String frontendUrl) {
        String booksHtml = renderLoanBookList(loans, frontendUrl);
        if (!StringUtils.hasText(booksHtml)) {
            return "";
        }

        return renderFragment("loan-book-list-section.html", Map.of("BOOKS_HTML", booksHtml));
    }

    private String renderBookCard(Book book, long count, String frontendUrl) {
        String bookUrl = frontendUrl + "/book/" + book.getId();

        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("BOOK_URL", escapeHtml(bookUrl));
        placeholders.put("BOOK_TITLE", escapeHtml(book.getTitle()));
        placeholders.put("BOOK_AUTHOR", escapeHtml(book.getAuthor()));
        placeholders.put("BOOK_AUTHOR_DISPLAY", displayWhen(StringUtils.hasText(book.getAuthor()), "block"));
        placeholders.put("BOOK_IMAGE_URL", escapeHtml(book.getImageUrl()));
        placeholders.put("BOOK_IMAGE_DISPLAY", displayWhen(StringUtils.hasText(book.getImageUrl()), "block"));
        placeholders.put("BOOK_PLACEHOLDER_DISPLAY", displayWhen(!StringUtils.hasText(book.getImageUrl()), "flex"));
        placeholders.put("GENRE_BADGES_HTML", renderGenreBadges(book.getGenres()));
        placeholders.put("GENRES_DISPLAY", displayWhen(hasGenres(book.getGenres()), "block"));
        placeholders.put("COPY_COUNT", String.valueOf(count));
        placeholders.put("COPY_COUNT_DISPLAY", displayWhen(count > 1, "block"));

        return renderFragment("book-card.html", placeholders);
    }

    private String renderShortenedDescription(String description) {
        if (!StringUtils.hasText(description)) {
            return "";
        }

        String shortenedDescription = description;
        if (shortenedDescription.length() > 200) {
            shortenedDescription = shortenedDescription.substring(0, 197) + "...";
        }

        return escapeHtml(shortenedDescription);
    }

    private String renderRecommendations(List<BookDTO> recommendations, String frontendUrl) {
        List<BookDTO> safeRecommendations = safeList(recommendations).stream()
                .limit(MAX_RECOMMENDATIONS)
                .toList();

        if (safeRecommendations.isEmpty()) {
            return "";
        }

        String cardsHtml = safeRecommendations.stream()
                .map(book -> renderRecommendationCard(book, frontendUrl))
                .collect(Collectors.joining());

        return renderFragment("recommendations-section.html", Map.of(
                "RECOMMENDATION_CARDS_HTML", cardsHtml));
    }

    private String renderRecommendationCard(BookDTO book, String frontendUrl) {
        String bookUrl = frontendUrl + "/book/" + book.getId();

        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("BOOK_URL", escapeHtml(bookUrl));
        placeholders.put("BOOK_TITLE", escapeHtml(book.getTitle()));
        placeholders.put("BOOK_AUTHOR", escapeHtml(book.getAuthor()));
        placeholders.put("BOOK_AUTHOR_DISPLAY", displayWhen(StringUtils.hasText(book.getAuthor()), "block"));
        placeholders.put("BOOK_IMAGE_URL", escapeHtml(book.getImageUrl()));
        placeholders.put("BOOK_IMAGE_DISPLAY", displayWhen(StringUtils.hasText(book.getImageUrl()), "block"));
        placeholders.put("BOOK_PLACEHOLDER_DISPLAY", displayWhen(!StringUtils.hasText(book.getImageUrl()), "flex"));
        placeholders.put("REVIEW_SCORE", renderReviewScoreValue(book.getReviewScore()));
        placeholders.put("REVIEW_SCORE_DISPLAY", displayWhen(hasReviewScore(book.getReviewScore()), "block"));

        return renderFragment("recommendation-card.html", placeholders);
    }

    private String renderGenreBadges(Collection<Genre> genres) {
        if (genres == null || genres.isEmpty()) {
            return "";
        }

        String badgesHtml = genres.stream()
                .map(Genre::getName)
                .filter(StringUtils::hasText)
                .map(genre -> renderFragment("genre-badge.html", Map.of("GENRE_NAME", escapeHtml(genre))))
                .collect(Collectors.joining());

        return badgesHtml;
    }

    private String renderBookAvailableGenreBadges(Collection<Genre> genres) {
        if (genres == null || genres.isEmpty()) {
            return "";
        }

        String badgesHtml = genres.stream()
                .map(Genre::getName)
                .filter(StringUtils::hasText)
                .map(genre -> renderFragment("book-available-genre-badge.html", Map.of("GENRE_NAME", escapeHtml(genre))))
                .collect(Collectors.joining());

        return badgesHtml;
    }

    private String renderReviewScoreValue(Double reviewScore) {
        return hasReviewScore(reviewScore) ? escapeHtml(String.format("%.1f", reviewScore)) : "";
    }

    private Map<Long, BookGroup> groupBooksById(List<Loan> loans) {
        Map<Long, BookGroup> groupedBooks = new LinkedHashMap<>();

        safeList(loans).stream()
                .map(Loan::getCopy)
                .filter(copy -> copy != null && copy.getBook() != null)
                .forEach(copy -> addBookToGroup(groupedBooks, copy));

        return groupedBooks;
    }

    private void addBookToGroup(Map<Long, BookGroup> groupedBooks, BookCopy copy) {
        Book book = copy.getBook();

        groupedBooks.compute(book.getId(), (bookId, existingGroup) -> {
            if (existingGroup == null) {
                return new BookGroup(book, 1);
            }

            return new BookGroup(existingGroup.book(), existingGroup.count() + 1);
        });
    }

    private String renderFragment(String templateName, Map<String, String> placeholders) {
        return templateRenderer.render(FRAGMENT_ROOT + templateName, placeholders);
    }

    private boolean hasGenres(Collection<Genre> genres) {
        return genres != null && genres.stream()
                .map(Genre::getName)
                .anyMatch(StringUtils::hasText);
    }

    private boolean hasReviewScore(Double reviewScore) {
        return reviewScore != null && reviewScore > 0;
    }

    private String displayWhen(boolean visible, String display) {
        return visible ? display : "none";
    }

    private <T> List<T> safeList(List<T> items) {
        return items == null ? List.of() : items;
    }

    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }

        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private record BookGroup(Book book, long count) {
    }
}
