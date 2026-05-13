package edu.ap.bib_backend.service.message.component;

import java.util.List;

import edu.ap.bib_backend.dto.BookDTO;
import edu.ap.bib_backend.service.message.HtmlMessageBuilder;
import edu.ap.bib_backend.service.message.MessageComponent;

public class BookRecommendationComponent implements MessageComponent {

    private static final int MAX_DISPLAY = 6;

    private static final String CARD_STYLE =
            "flex: 1 1 calc(50% - 10px); min-width: 200px; padding: 12px; " +
            "background-color: #ffffff; border-radius: 8px; " +
            "border: 1px solid #f0e4e8; box-sizing: border-box;";

    private static final String COVER_STYLE =
            "flex-shrink: 0; width: 48px; height: 68px; margin-right: 10px; " +
            "background-color: #f5eef1; border-radius: 3px; overflow: hidden;";

    private static final String COVER_PLACEHOLDER_STYLE =
            "width: 100%; height: 100%; display: flex; align-items: center; " +
            "justify-content: center; color: #8a7580; font-size: 16px;";

    private static final String LINK_STYLE =
            "color: #1a0a10; text-decoration: none; font-weight: 600;";

    private final List<BookDTO> recommendedBooks;
    private final String frontendUrl;

    public BookRecommendationComponent(List<BookDTO> recommendedBooks, String frontendUrl) {
        this.recommendedBooks = recommendedBooks != null ? recommendedBooks : List.of();
        this.frontendUrl = frontendUrl;
    }

    @Override
    public boolean isEnabled() {
        return !recommendedBooks.isEmpty();
    }

    @Override
    public String build() {
        StringBuilder sb = new StringBuilder();

        sb.append("<hr style=\"border: none; border-top: 1px solid #f0e4e8; margin: 20px 0;\">");

        sb.append("<h3 style=\"color: #8a1e41; font-size: 16px; font-weight: 600; margin: 0 0 6px 0;\">");
        sb.append("Aanbevolen boeken");
        sb.append("</h3>");

        sb.append("<p style=\"font-size: 13px; color: #8a7580; margin: 0 0 16px 0; line-height: 1.5;\">");
        sb.append("Op basis van jouw uitleenkeuze vind je hier enkele suggesties:");
        sb.append("</p>");

        sb.append("<div style=\"display: flex; flex-wrap: wrap; gap: 10px;\">");

        int count = Math.min(recommendedBooks.size(), MAX_DISPLAY);
        for (int i = 0; i < count; i++) {
            BookDTO book = recommendedBooks.get(i);
            appendBookCard(sb, book);
        }

        sb.append("</div>");

        sb.append("<p style=\"font-size: 12px; color: #8a7580; margin: 12px 0 0 0; text-align: center;\">");
        sb.append("Ontdek deze en andere boeken in je bibliotheek.");
        sb.append("</p>");

        return sb.toString();
    }

    private void appendBookCard(StringBuilder sb, BookDTO book) {
        String bookUrl = frontendUrl + "/book/" + book.getId();

        sb.append("<div style=\"").append(CARD_STYLE).append("\">");
        sb.append("<div style=\"display: flex; align-items: flex-start;\">");

        sb.append("<a href=\"").append(HtmlMessageBuilder.escapeHtml(bookUrl))
          .append("\" style=\"text-decoration: none; display: block;\">");
        sb.append("<div style=\"").append(COVER_STYLE).append("\">");
        appendCoverImage(sb, book);
        sb.append("</div>");
        sb.append("</a>");

        sb.append("<div style=\"flex: 1; min-width: 0;\">");
        sb.append("<a href=\"").append(HtmlMessageBuilder.escapeHtml(bookUrl))
          .append("\" style=\"").append(LINK_STYLE).append("\">");
        sb.append("<p style=\"margin: 0 0 2px 0; font-size: 13px; ")
          .append("overflow: hidden; text-overflow: ellipsis; white-space: nowrap;\">");
        sb.append(HtmlMessageBuilder.escapeHtml(book.getTitle()));
        sb.append("</p>");
        sb.append("</a>");

        if (book.getAuthor() != null && !book.getAuthor().isBlank()) {
            sb.append("<p style=\"margin: 0 0 2px 0; font-size: 12px; color: #8a7580;\">");
            sb.append(HtmlMessageBuilder.escapeHtml(book.getAuthor()));
            sb.append("</p>");
        }

        if (book.getReviewScore() > 0) {
            sb.append("<p style=\"margin: 2px 0 0 0; font-size: 12px; color: #c98500;\">");
            sb.append("\u2605 ").append(String.format("%.1f", book.getReviewScore()));
            sb.append("</p>");
        }

        sb.append("</div>");
        sb.append("</div>");
        sb.append("</div>");
    }

    private void appendCoverImage(StringBuilder sb, BookDTO book) {
        if (book.getImageUrl() != null && !book.getImageUrl().isBlank()) {
            sb.append("<img src=\"").append(HtmlMessageBuilder.escapeHtml(book.getImageUrl()))
              .append("\" alt=\"").append(HtmlMessageBuilder.escapeHtml(book.getTitle()))
              .append("\" style=\"width: 100%; height: 100%; object-fit: cover; display: block;\">");
        } else {
            sb.append("<div style=\"").append(COVER_PLACEHOLDER_STYLE).append("\">&#128218;</div>");
        }
    }
}
