package edu.ap.bib_backend.service.message;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HtmlMessageBuilder implements MessageBuilder {

    private static final Logger log = LoggerFactory.getLogger(HtmlMessageBuilder.class);

    private static final String STYLE_CONTAINER =
            "font-family: 'Segoe UI', Calibri, Arial, sans-serif; " +
            "max-width: 600px; margin: 0 auto; color: #1a0a10; " +
            "border: 1px solid #e8d5dc; border-radius: 12px; " +
            "overflow: hidden; box-shadow: 0 2px 12px rgba(138, 30, 65, 0.08);";

    private static final String STYLE_HEADER =
            "background: linear-gradient(135deg, #8a1e41 0%, #a82550 100%); " +
            "color: white; padding: 28px 20px; text-align: center;";

    private static final String STYLE_BODY =
            "padding: 24px 28px; background-color: #ffffff;";

    private static final String STYLE_FOOTER =
            "background-color: #faf5f7; padding: 16px 28px; " +
            "text-align: center; font-size: 12px; color: #8a7580; " +
            "border-top: 1px solid #f0e4e8;";

    private final String title;
    private final String frontendUrl;
    private final MessageStyles messageStyles;
    private final List<MessageComponent> components = new ArrayList<>();

    public HtmlMessageBuilder(String title, String frontendUrl, MessageStyles messageStyles) {
        this.title = title;
        this.frontendUrl = frontendUrl;
        this.messageStyles = messageStyles;
    }

    public HtmlMessageBuilder(String title, String frontendUrl) {
        this(title, frontendUrl, new DefaultMessageStyles());
    }

    @Override
    public MessageBuilder addComponent(MessageComponent component) {
        if (component != null && component.isEnabled()) {
            components.add(component);
        }
        return this;
    }

    @Override
    public String build() {
        StringBuilder sb = new StringBuilder();

        sb.append("<div style=\"").append(STYLE_CONTAINER).append("\">");

        sb.append(messageStyles.getStyleBlock());

        sb.append("<div style=\"").append(STYLE_HEADER).append("\">");
        sb.append("<h2 style=\"margin: 0; font-size: 22px; font-weight: 600;\">")
                .append(escapeHtml(title))
                .append("</h2>");
        sb.append("</div>");

        sb.append("<div style=\"").append(STYLE_BODY).append("\">");
        for (MessageComponent component : components) {
            sb.append(component.build());
        }
        sb.append("</div>");

        sb.append("<div style=\"").append(STYLE_FOOTER).append("\">");
        sb.append("Dit is een automatisch gegenereerd bericht. Gelieve niet te reageren.");
        sb.append("</div>");

        sb.append("</div>");

        String html = sb.toString();
        log.debug("HtmlMessageBuilder built message: title='{}', length={} chars, components={}",
                title, html.length(), components.size());
        return html;
    }

    public String getFrontendUrl() {
        return frontendUrl;
    }

    public MessageStyles getMessageStyles() {
        return messageStyles;
    }

    public static String escapeHtml(String text) {
        if (text == null) return "";
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
