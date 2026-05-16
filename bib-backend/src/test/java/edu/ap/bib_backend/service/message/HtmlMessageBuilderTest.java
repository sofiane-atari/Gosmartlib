package edu.ap.bib_backend.service.message;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HtmlMessageBuilderTest {

    private static final String FRONTEND_URL = "https://bib.example.com";

    private static MessageComponent enabledComponent(String html) {
        return new MessageComponent() {
            @Override public boolean isEnabled() { return true; }
            @Override public String build() { return html; }
        };
    }

    @Test
    void build_withNoComponents_returnsValidHtmlShell() {
        HtmlMessageBuilder builder = new HtmlMessageBuilder("Test Title", FRONTEND_URL);
        String html = builder.build();

        assertTrue(html.startsWith("<div"), "should start with wrapper div");
        assertTrue(html.contains("Test Title"), "should contain the title");
        assertTrue(html.contains("automatisch gegenereerd"), "should contain footer text");
        assertTrue(html.contains("</div>"), "should close wrapper div");
    }

    @Test
    void build_withNoComponents_containsStyleBlock() {
        HtmlMessageBuilder builder = new HtmlMessageBuilder("Title", FRONTEND_URL);
        String html = builder.build();
        assertTrue(html.contains("<style>"));
    }

    @Test
    void addComponent_withEnabledComponent_includesItsContent() {
        HtmlMessageBuilder builder = new HtmlMessageBuilder("Title", FRONTEND_URL);
        builder.addComponent(enabledComponent("<p>Hello</p>"));

        String html = builder.build();
        assertTrue(html.contains("<p>Hello</p>"));
    }

    @Test
    void addComponent_withDisabledComponent_excludesItsContent() {
        HtmlMessageBuilder builder = new HtmlMessageBuilder("Title", FRONTEND_URL);
        builder.addComponent(new MessageComponent() {
            @Override public boolean isEnabled() { return false; }
            @Override public String build() { return "<p>ShouldNotAppear</p>"; }
        });

        String html = builder.build();
        assertFalse(html.contains("ShouldNotAppear"));
    }

    @Test
    void addComponent_withNullComponent_doesNotThrow() {
        HtmlMessageBuilder builder = new HtmlMessageBuilder("Title", FRONTEND_URL);
        builder.addComponent(null);
        String html = builder.build();
        assertNotNull(html);
    }

    @Test
    void addComponent_returnsBuilderForFluentApi() {
        HtmlMessageBuilder builder = new HtmlMessageBuilder("Title", FRONTEND_URL);
        MessageBuilder result = builder.addComponent(enabledComponent("<p>X</p>"));
        assertSame(builder, result);
    }

    @Test
    void build_includesMultipleComponentsInOrder() {
        HtmlMessageBuilder builder = new HtmlMessageBuilder("Title", FRONTEND_URL);
        builder.addComponent(enabledComponent("<p>First</p>"));
        builder.addComponent(enabledComponent("<p>Second</p>"));

        String html = builder.build();
        int first = html.indexOf("<p>First</p>");
        int second = html.indexOf("<p>Second</p>");
        assertTrue(first >= 0 && second >= 0, "both components should appear");
        assertTrue(first < second, "First component should come before Second");
    }

    @Test
    void build_usesCustomMessageStyles() {
        String customStyle = "<style>.custom{color:red;}</style>";
        MessageStyles customStyles = () -> customStyle;

        HtmlMessageBuilder builder = new HtmlMessageBuilder("Title", FRONTEND_URL, customStyles);
        String html = builder.build();

        assertTrue(html.contains(customStyle));
    }

    @Test
    void build_doesNotContainDefaultStyleWhenCustomProvided() {
        MessageStyles customStyles = () -> "<style>.custom{}</style>";
        HtmlMessageBuilder builder = new HtmlMessageBuilder("Title", FRONTEND_URL, customStyles);
        String html = builder.build();

        assertFalse(html.contains("msg-btn-primary"), "should not contain default CSS classes");
    }

    @Test
    void getFrontendUrl_returnsUrl() {
        HtmlMessageBuilder builder = new HtmlMessageBuilder("Title", FRONTEND_URL);
        assertEquals(FRONTEND_URL, builder.getFrontendUrl());
    }

    @Test
    void getMessageStyles_returnsDefaultInstance() {
        HtmlMessageBuilder builder = new HtmlMessageBuilder("Title", FRONTEND_URL);
        assertInstanceOf(DefaultMessageStyles.class, builder.getMessageStyles());
    }

    @Test
    void getMessageStyles_returnsCustomInstance() {
        MessageStyles custom = () -> "";
        HtmlMessageBuilder builder = new HtmlMessageBuilder("Title", FRONTEND_URL, custom);
        assertSame(custom, builder.getMessageStyles());
    }

    @Test
    void escapeHtml_withNull_returnsEmptyString() {
        assertEquals("", HtmlMessageBuilder.escapeHtml(null));
    }

    @Test
    void escapeHtml_withAmpersand_escapesIt() {
        assertEquals("AT&amp;T", HtmlMessageBuilder.escapeHtml("AT&T"));
    }

    @Test
    void escapeHtml_withLessThan_escapesIt() {
        assertEquals("&lt;script&gt;", HtmlMessageBuilder.escapeHtml("<script>"));
    }

    @Test
    void escapeHtml_withDoubleQuote_escapesIt() {
        assertEquals("&quot;hello&quot;", HtmlMessageBuilder.escapeHtml("\"hello\""));
    }

    @Test
    void escapeHtml_withSingleQuote_escapesIt() {
        assertEquals("&#39;hello&#39;", HtmlMessageBuilder.escapeHtml("'hello'"));
    }

    @Test
    void escapeHtml_withPlainText_returnsSame() {
        assertEquals("Hello World", HtmlMessageBuilder.escapeHtml("Hello World"));
    }

    @Test
    void escapeHtml_withMultipleSpecialChars_escapesAll() {
        String input = "<a href=\"test&foo='bar'\">";
        String expected = "&lt;a href=&quot;test&amp;foo=&#39;bar&#39;&quot;&gt;";
        assertEquals(expected, HtmlMessageBuilder.escapeHtml(input));
    }
}
