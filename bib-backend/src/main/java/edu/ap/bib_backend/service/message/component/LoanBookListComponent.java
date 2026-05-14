package edu.ap.bib_backend.service.message.component;

import java.util.LinkedHashMap;
import java.util.Map;

import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.entity.Loan;
import edu.ap.bib_backend.service.message.HtmlMessageBuilder;
import edu.ap.bib_backend.service.message.MessageComponent;

public class LoanBookListComponent implements MessageComponent {

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

    private final Loan loan;
    private final String frontendUrl;

    public LoanBookListComponent(Loan loan, String frontendUrl) {
        this.loan = loan;
        this.frontendUrl = frontendUrl;
    }

    @Override
    public boolean isEnabled() {
        return loan.getCopies() != null && !loan.getCopies().isEmpty();
    }

    @Override
    public String build() {
        Map<Long, BookGroup> bookGroups = groupBooksById();

        if (bookGroups.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        sb.append("<hr style=\"border: none; border-top: 1px solid #f0e4e8; margin: 0 0 20px 0;\">");

        sb.append("<h3 style=\"color: #8a1e41; font-size: 16px; font-weight: 600; margin: 0 0 16px 0;\">");
        sb.append("Uitgeleende boeken");
        sb.append("</h3>");

        for (BookGroup group : bookGroups.values()) {
            Book book = group.book();
            long count = group.count();

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

            if (count > 1) {
                sb.append("<p style=\"margin: 4px 0 0 0; font-size: 12px; color: #8a7580;\">");
                sb.append(count).append(" exemplaren");
                sb.append("</p>");
            }

            sb.append("</div>");
            sb.append("</div>");
        }

        return sb.toString();
    }

    private Map<Long, BookGroup> groupBooksById() {
        Map<Long, BookGroup> groupedBooks = new LinkedHashMap<>();

        loan.getCopies().stream()
                .filter(copy -> copy != null && copy.getBook() != null)
                .forEach(copy -> {
                    Book book = copy.getBook();

                    groupedBooks.compute(
                            book.getId(),
                            (bookId, existingGroup) -> {
                                if (existingGroup == null) {
                                    return new BookGroup(book, 1);
                                }

                                return new BookGroup(
                                        existingGroup.book(),
                                        existingGroup.count() + 1);
                            });
                });

        return groupedBooks;
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
        if (book.getGenres() == null || book.getGenres().isEmpty()) {
            return;
        }

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
