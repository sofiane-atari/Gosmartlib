package edu.ap.bib_backend.service.message.component;

import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.service.message.HtmlMessageBuilder;
import edu.ap.bib_backend.service.message.MessageComponent;

public class BookAvailableMessageComponent implements MessageComponent {

    private static final String COVER_STYLE =
            "max-width: 200px; max-height: 280px; width: auto; height: auto; " +
            "border-radius: 8px; box-shadow: 0 4px 16px rgba(0,0,0,0.15); " +
            "display: block; margin: 0 auto;";

    private static final String COVER_PLACEHOLDER_STYLE =
            "width: 160px; height: 220px; display: flex; align-items: center; " +
            "justify-content: center; background-color: #f0e4e8; " +
            "border-radius: 8px; color: #8a7580; font-size: 48px; " +
            "margin: 0 auto;";

    private static final String GENRE_BADGE_STYLE =
            "display: inline-block; padding: 3px 12px; margin: 0 5px 4px 0; " +
            "font-size: 12px; color: #8a1e41; background-color: #fdf0f5; " +
            "border-radius: 12px;";

    private static final String BUTTON_STYLE =
            "background-color: #8a1e41; color: white; padding: 13px 32px; " +
            "text-decoration: none; border-radius: 6px; font-weight: 600; " +
            "font-size: 14px; display: inline-block; border: none; cursor: pointer; " +
            "transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important;";

    private final Book book;
    private final String frontendUrl;

    public BookAvailableMessageComponent(Book book, String frontendUrl) {
        this.book = book;
        this.frontendUrl = frontendUrl;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String build() {
        StringBuilder sb = new StringBuilder();

        sb.append("<div style=\"text-align: center; padding: 16px 0;\">");

        sb.append("<p style=\"margin: 0 0 20px 0; font-size: 18px; line-height: 1.5; color: #8a1e41; font-weight: 600;\">");
        sb.append("Het boek is opnieuw beschikbaar in de bibliotheek!");
        sb.append("</p>");

        appendCover(sb);

        sb.append("<h2 style=\"margin: 20px 0 4px 0; font-size: 22px; font-weight: 700; color: #1a0a10; line-height: 1.3;\">");
        sb.append(HtmlMessageBuilder.escapeHtml(book.getTitle()));
        sb.append("</h2>");

        if (book.getAuthor() != null && !book.getAuthor().isBlank()) {
            sb.append("<p style=\"margin: 0 0 8px 0; font-size: 15px; color: #8a7580;\">");
            sb.append(HtmlMessageBuilder.escapeHtml(book.getAuthor()));
            sb.append("</p>");
        }

        appendGenres(sb);

        appendReviewScore(sb);

        if (book.getDescription() != null && !book.getDescription().isBlank()) {
            String desc = book.getDescription();
            if (desc.length() > 200) {
                desc = desc.substring(0, 197) + "...";
            }
            sb.append("<p style=\"margin: 16px 0 0 0; font-size: 14px; color: #555; line-height: 1.6; text-align: center;\">");
            sb.append(HtmlMessageBuilder.escapeHtml(desc));
            sb.append("</p>");
        }

        String bookUrl = frontendUrl + "/book/" + book.getId();
        sb.append("<div style=\"margin-top: 24px;\">");
        sb.append("<a href=\"").append(HtmlMessageBuilder.escapeHtml(bookUrl))
          .append("\" class=\"msg-btn-primary\" style=\"").append(BUTTON_STYLE).append("\">");
        sb.append("Bekijk in catalogus");
        sb.append("</a>");
        sb.append("</div>");

        sb.append("</div>");

        return sb.toString();
    }

    private void appendCover(StringBuilder sb) {
        if (book.getImageUrl() != null && !book.getImageUrl().isBlank()) {
            sb.append("<div style=\"margin: 0 auto;\">");
            sb.append("<img src=\"").append(HtmlMessageBuilder.escapeHtml(book.getImageUrl()))
              .append("\" alt=\"").append(HtmlMessageBuilder.escapeHtml(book.getTitle()))
              .append("\" style=\"").append(COVER_STYLE).append("\">");
            sb.append("</div>");
        } else {
            sb.append("<div style=\"").append(COVER_PLACEHOLDER_STYLE).append("\">&#128218;</div>");
        }
    }

    private void appendGenres(StringBuilder sb) {
        if (book.getGenres() == null || book.getGenres().isEmpty()) return;
        sb.append("<div style=\"margin: 8px 0 0 0;\">");
        for (var genre : book.getGenres()) {
            sb.append("<span style=\"").append(GENRE_BADGE_STYLE).append("\">");
            sb.append(HtmlMessageBuilder.escapeHtml(genre.getName()));
            sb.append("</span>");
        }
        sb.append("</div>");
    }

    private void appendReviewScore(StringBuilder sb) {
        Double score = book.getReviewScore();
        if (score == null || score <= 0) return;
        sb.append("<p style=\"margin: 8px 0 0 0; font-size: 14px; color: #c98500;\">");
        sb.append("\u2605 ").append(String.format("%.1f", score));
        sb.append("</p>");
    }
}
