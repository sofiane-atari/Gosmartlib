package edu.ap.bib_backend.service.message.component;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

import edu.ap.bib_backend.jpa.entity.Loan;
import edu.ap.bib_backend.service.message.HtmlMessageBuilder;
import edu.ap.bib_backend.service.message.MessageComponent;

public class LoanGeneralInfoComponent implements MessageComponent {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("d MMMM yyyy", new Locale("nl", "BE"));

    private final Loan loan;
    private final String frontendUrl;

    public LoanGeneralInfoComponent(Loan loan, String frontendUrl) {
        this.loan = loan;
        this.frontendUrl = frontendUrl;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String build() {
        int bookCount = loan.getCopies() != null ? loan.getCopies().size() : 0;

        StringBuilder sb = new StringBuilder();

        sb.append("<p style=\"font-size: 15px; line-height: 1.6; margin: 0 0 12px 0;\">");
        sb.append("Beste leerling,");
        sb.append("</p>");

        sb.append("<p style=\"font-size: 15px; line-height: 1.6; margin: 0 0 20px 0;\">");
        sb.append("Je hebt met succes een nieuwe uitlening geregistreerd. Hieronder vind je een overzicht.");
        sb.append("</p>");

        sb.append("<div style=\"background-color: #fdf8fa; padding: 18px 20px; ");
        sb.append("border-left: 4px solid #8a1e41; margin: 0 0 24px 0; border-radius: 0 8px 8px 0;\">");

        sb.append("<table style=\"width: 100%; border-collapse: collapse; font-size: 14px;\">");

        sb.append("<tr>");
        sb.append("<td style=\"padding: 6px 12px 6px 0; color: #8a7580; width: 160px;\">Uitleendatum:</td>");
        sb.append("<td style=\"padding: 6px 0; font-weight: 600;\">");
        sb.append(loan.getLoanDate() != null ? loan.getLoanDate().format(DATE_FORMATTER) : "-");
        sb.append("</td>");
        sb.append("</tr>");

        sb.append("<tr>");
        sb.append("<td style=\"padding: 6px 12px 6px 0; color: #8a7580;\">Retourdatum:</td>");
        sb.append("<td style=\"padding: 6px 0; font-weight: 600;\">");
        sb.append(loan.getReturnDate() != null ? loan.getReturnDate().format(DATE_FORMATTER) : "-");
        sb.append("</td>");
        sb.append("</tr>");

        sb.append("<tr>");
        sb.append("<td style=\"padding: 6px 12px 6px 0; color: #8a7580;\">Aantal geleende boeken:</td>");
        sb.append("<td style=\"padding: 6px 0; font-weight: 600;\">");
        sb.append(bookCount);
        sb.append("</td>");
        sb.append("</tr>");

        sb.append("</table>");
        sb.append("</div>");

        sb.append("<div style=\"text-align: center; margin: 0 0 24px 0;\">");
        sb.append("<a href=\"").append(HtmlMessageBuilder.escapeHtml(frontendUrl))
                .append("/history\" class=\"msg-btn-primary\" style=\"background-color: #8a1e41; color: white; ")
                .append("padding: 13px 28px; text-decoration: none; border-radius: 6px; ")
                .append("font-weight: 600; font-size: 14px; display: inline-block; ")
                .append("border: none; cursor: pointer;\">");
        sb.append("Bekijk mijn uitleenhistoriek");
        sb.append("</a>");
        sb.append("</div>");

        return sb.toString();
    }
}
