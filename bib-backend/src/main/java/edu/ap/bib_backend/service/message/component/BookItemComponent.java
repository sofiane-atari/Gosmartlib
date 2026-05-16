package edu.ap.bib_backend.service.message.component;

import java.util.Collection;
import java.util.List;

import edu.ap.bib_backend.service.message.HtmlMessageBuilder;
import edu.ap.bib_backend.service.message.MessageComponent;

public class BookItemComponent implements MessageComponent {

    private static final String CARD_STYLE =
            "display: flex; align-items: center; padding: 12px; " +
            "margin: 0 0 10px 0; background-color: #faf5f7; " +
            "border-radius: 8px; border: 1px solid #f0e4e8;";

    private static final String COVER_STYLE =
            "flex-shrink: 0; width: 56px; height: 80px; margin-right: 14px; " +
            "background-color: #f0e4e8; border-radius: 4px; overflow: hidden;";

    private static final String COVER_PLACEHOLDER_STYLE =
            "width: 100%; height: 100%; display: flex; align-items: center; " +
            "justify-content: center; color: #8a7580; font-size: 20px;";

    private static final String GENRE_BADGE_STYLE =
            "display: inline-block; padding: 1px 8px; margin: 0 4px 2px 0; " +
            "font-size: 11px; color: #8a1e41; background-color: #fdf0f5; " +
            "border-radius: 10px;";

    private static final String LINK_STYLE =
            "color: #1a0a10; text-decoration: none; font-weight: 600;";

    private final Long bookId;
    private final String title;
    private final String author;
    private final String imageUrl;
    private final Collection<String> genres;
    private final double reviewScore;
    private final String frontendUrl;

    public BookItemComponent(Long bookId, String title, String author, String imageUrl,
                             Collection<String> genres, double reviewScore, String frontendUrl) {
        this.bookId = bookId;
        this.title = title;
        this.author = author;
        this.imageUrl = imageUrl;
        this.genres = genres != null ? genres : List.of();
        this.reviewScore = reviewScore;
        this.frontendUrl = frontendUrl;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String build() {
        String bookUrl = frontendUrl + "/book/" + bookId;

        StringBuilder sb = new StringBuilder();

        sb.append("<div style=\"").append(CARD_STYLE)
          .append("\" class=\"msg-book-card\">");

        sb.append("<a href=\"").append(HtmlMessageBuilder.escapeHtml(bookUrl))
          .append("\" class=\"msg-cover-link\" style=\"text-decoration: none; display: block;\">");
        sb.append("<div style=\"").append(COVER_STYLE).append("\">");
        appendCoverImage(sb);
        sb.append("</div>");
        sb.append("</a>");

        sb.append("<div style=\"flex: 1; min-width: 0;\">");
        appendTitle(sb, bookUrl);
        appendAuthor(sb);
        appendGenres(sb);
        appendReviewScore(sb);
        sb.append("</div>");

        sb.append("</div>");

        return sb.toString();
    }

    private void appendCoverImage(StringBuilder sb) {
        if (imageUrl != null && !imageUrl.isBlank()) {
            sb.append("<img src=\"").append(HtmlMessageBuilder.escapeHtml(imageUrl))
              .append("\" alt=\"").append(HtmlMessageBuilder.escapeHtml(title))
              .append("\" style=\"width: 100%; height: 100%; object-fit: cover; display: block;\">");
        } else {
            sb.append("<div style=\"").append(COVER_PLACEHOLDER_STYLE).append("\">&#128218;</div>");
        }
    }

    private void appendTitle(StringBuilder sb, String bookUrl) {
        sb.append("<a href=\"").append(HtmlMessageBuilder.escapeHtml(bookUrl))
          .append("\" class=\"msg-book-link\" style=\"").append(LINK_STYLE).append("\">");
        sb.append("<p style=\"margin: 0 0 3px 0; font-size: 14px; ")
          .append("overflow: hidden; text-overflow: ellipsis; white-space: nowrap;\">");
        sb.append(HtmlMessageBuilder.escapeHtml(title));
        sb.append("</p>");
        sb.append("</a>");
    }

    private void appendAuthor(StringBuilder sb) {
        if (author == null || author.isBlank()) return;
        sb.append("<p style=\"margin: 0 0 2px 0; font-size: 13px; color: #8a7580;\">");
        sb.append(HtmlMessageBuilder.escapeHtml(author));
        sb.append("</p>");
    }

    private void appendGenres(StringBuilder sb) {
        if (genres.isEmpty()) return;
        sb.append("<div style=\"margin-top: 4px;\">");
        for (String genre : genres) {
            sb.append("<span style=\"").append(GENRE_BADGE_STYLE).append("\">");
            sb.append(HtmlMessageBuilder.escapeHtml(genre));
            sb.append("</span>");
        }
        sb.append("</div>");
    }

    private void appendReviewScore(StringBuilder sb) {
        if (reviewScore <= 0) return;
        sb.append("<p style=\"margin: 2px 0 0 0; font-size: 12px; color: #c98500;\">");
        sb.append("\u2605 ").append(String.format("%.1f", reviewScore));
        sb.append("</p>");
    }
}
