package edu.ap.bib_backend.service.message.component;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BookItemComponentTest {

    private static final String FRONTEND_URL = "https://bib.example.com";

    @Test
    void isEnabled_AlwaysReturnsTrue() {
        BookItemComponent component = new BookItemComponent(
                1L, "Test", "Author", null, null, 0, FRONTEND_URL);
        assertTrue(component.isEnabled());
    }

    @Test
    void build_ContainsTitle() {
        BookItemComponent component = new BookItemComponent(
                1L, "Harry Potter", "J.K. Rowling", null, null, 0, FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains("Harry Potter"));
    }

    @Test
    void build_ContainsBookLink() {
        BookItemComponent component = new BookItemComponent(
                42L, "Linked Book", "Author", null, null, 0, FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains(FRONTEND_URL + "/book/42"));
    }

    @Test
    void build_ContainsAuthorWhenPresent() {
        BookItemComponent component = new BookItemComponent(
                1L, "Test", "J.K. Rowling", null, null, 0, FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains("J.K. Rowling"));
    }

    @Test
    void build_DoesNotContainAuthorWhenNull() {
        BookItemComponent component = new BookItemComponent(
                1L, "Test", null, null, null, 0, FRONTEND_URL);
        String html = component.build();
        assertFalse(html.contains("font-size: 13px"));
    }

    @Test
    void build_DoesNotContainAuthorWhenBlank() {
        BookItemComponent component = new BookItemComponent(
                1L, "Test", "   ", null, null, 0, FRONTEND_URL);
        String html = component.build();
        assertFalse(html.contains("font-size: 13px"));
    }

    @Test
    void build_WithImageUrl_RendersImgTag() {
        BookItemComponent component = new BookItemComponent(
                1L, "With Image", "Author", "https://example.com/cover.jpg", null, 0, FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains("<img"));
        assertTrue(html.contains("https://example.com/cover.jpg"));
    }

    @Test
    void build_WithoutImageUrl_RendersPlaceholder() {
        BookItemComponent component = new BookItemComponent(
                1L, "No Image", "Author", null, null, 0, FRONTEND_URL);
        String html = component.build();
        assertFalse(html.contains("<img"));
    }

    @Test
    void build_WithEmptyImageUrl_RendersPlaceholder() {
        BookItemComponent component = new BookItemComponent(
                1L, "Empty Image", "Author", "", null, 0, FRONTEND_URL);
        String html = component.build();
        assertFalse(html.contains("<img"));
    }

    @Test
    void build_WithGenres_RendersGenreBadges() {
        BookItemComponent component = new BookItemComponent(
                1L, "Genre Book", "Author", null, List.of("Fiction", "Fantasy"), 0, FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains("Fiction"));
        assertTrue(html.contains("Fantasy"));
    }

    @Test
    void build_WithoutGenres_DoesNotRenderBadges() {
        BookItemComponent component = new BookItemComponent(
                1L, "No Genre", "Author", null, null, 0, FRONTEND_URL);
        String html = component.build();
    }

    @Test
    void build_WithEmptyGenres_DoesNotRenderBadges() {
        BookItemComponent component = new BookItemComponent(
                1L, "Empty Genre", "Author", null, List.of(), 0, FRONTEND_URL);
        String html = component.build();
    }

    @Test
    void build_WithReviewScore_ShowsStars() {
        BookItemComponent component = new BookItemComponent(
                1L, "Rated Book", "Author", null, null, 4.5, FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains("4.5") || html.contains("4,5"));
    }

    @Test
    void build_WithZeroReviewScore_DoesNotShowStars() {
        BookItemComponent component = new BookItemComponent(
                1L, "No Rating", "Author", null, null, 0, FRONTEND_URL);
        String html = component.build();
        assertFalse(html.contains("\u2605"));
    }

    @Test
    void build_ContainsMsgBookCardClass() {
        BookItemComponent component = new BookItemComponent(
                1L, "Card", "Author", null, null, 0, FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains("msg-book-card"));
    }

    @Test
    void build_ContainsMsgBookLinkClass() {
        BookItemComponent component = new BookItemComponent(
                1L, "Link", "Author", null, null, 0, FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains("msg-book-link"));
    }

    @Test
    void build_ContainsMsgCoverLinkClass() {
        BookItemComponent component = new BookItemComponent(
                1L, "Cover", "Author", null, null, 0, FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains("msg-cover-link"));
    }

    @Test
    void build_EscapesHtmlInTitle() {
        BookItemComponent component = new BookItemComponent(
                1L, "<script>alert('xss')</script>", "Author", null, null, 0, FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains("&lt;script&gt;alert(&#39;xss&#39;)&lt;/script&gt;"));
        assertFalse(html.contains("<script>alert('xss')</script>"));
    }

    @Test
    void build_EscapesHtmlInImageUrl() {
        BookItemComponent component = new BookItemComponent(
                1L, "Test", "Author", "https://example.com/cover.jpg?q='&quot;", null, 0, FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains("https://example.com/cover.jpg?q="));
    }
}
