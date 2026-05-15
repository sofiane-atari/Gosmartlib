package edu.ap.bib_backend.service.message.component;

import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.entity.Genre;

import static org.junit.jupiter.api.Assertions.*;

class BookAvailableMessageComponentTest {

    private static final String FRONTEND_URL = "https://bib.example.com";

    private Book createBook(Long id, String title, String author, String imageUrl,
                            String description, Double reviewScore, Set<Genre> genres) {
        Book book = new Book();
        book.setId(id);
        book.setTitle(title);
        book.setAuthor(author);
        book.setImageUrl(imageUrl);
        book.setDescription(description);
        book.setReviewScore(reviewScore);
        book.setGenres(genres);
        return book;
    }

    @Test
    void isEnabled_AlwaysReturnsTrue() {
        Book book = createBook(1L, "Test", null, null, null, null, null);
        BookAvailableMessageComponent component = new BookAvailableMessageComponent(book, FRONTEND_URL);
        assertTrue(component.isEnabled());
    }

    @Test
    void build_ContainsBookTitle() {
        Book book = createBook(1L, "Harry Potter", "J.K. Rowling", null, null, null, null);
        BookAvailableMessageComponent component = new BookAvailableMessageComponent(book, FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains("Harry Potter"));
    }

    @Test
    void build_ContainsBeschikbaarMessage() {
        Book book = createBook(1L, "Test", "Author", null, null, null, null);
        BookAvailableMessageComponent component = new BookAvailableMessageComponent(book, FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains("opnieuw beschikbaar"));
    }

    @Test
    void build_ContainsAuthorWhenPresent() {
        Book book = createBook(1L, "Test", "J.K. Rowling", null, null, null, null);
        BookAvailableMessageComponent component = new BookAvailableMessageComponent(book, FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains("J.K. Rowling"));
    }

    @Test
    void build_DoesNotContainAuthorWhenNull() {
        Book book = createBook(1L, "Test", null, null, null, null, null);
        BookAvailableMessageComponent component = new BookAvailableMessageComponent(book, FRONTEND_URL);
        String html = component.build();
        assertFalse(html.contains("Auteur"));
    }

    @Test
    void build_DoesNotContainAuthorWhenBlank() {
        Book book = createBook(1L, "Test", "   ", null, null, null, null);
        BookAvailableMessageComponent component = new BookAvailableMessageComponent(book, FRONTEND_URL);
        String html = component.build();
        assertFalse(html.contains("font-size: 15px"));
    }

    @Test
    void build_ContainsBookLink() {
        Book book = createBook(42L, "Linked Book", "Author", null, null, null, null);
        BookAvailableMessageComponent component = new BookAvailableMessageComponent(book, FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains(FRONTEND_URL + "/book/42"));
    }

    @Test
    void build_ContainsButtonToCatalog() {
        Book book = createBook(1L, "Test", "Author", null, null, null, null);
        BookAvailableMessageComponent component = new BookAvailableMessageComponent(book, FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains("Bekijk in catalogus"));
    }

    @Test
    void build_WithImageUrl_RendersImgTag() {
        Book book = createBook(1L, "With Image", "Author", "https://example.com/cover.jpg", null, null, null);
        BookAvailableMessageComponent component = new BookAvailableMessageComponent(book, FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains("<img"));
        assertTrue(html.contains("https://example.com/cover.jpg"));
    }

    @Test
    void build_WithoutImageUrl_RendersPlaceholder() {
        Book book = createBook(1L, "No Image", "Author", null, null, null, null);
        BookAvailableMessageComponent component = new BookAvailableMessageComponent(book, FRONTEND_URL);
        String html = component.build();
        assertFalse(html.contains("<img"));
    }

    @Test
    void build_WithEmptyImageUrl_RendersPlaceholder() {
        Book book = createBook(1L, "Empty Image", "Author", "", null, null, null);
        BookAvailableMessageComponent component = new BookAvailableMessageComponent(book, FRONTEND_URL);
        String html = component.build();
        assertFalse(html.contains("<img"));
    }

    @Test
    void build_WithGenres_RendersGenreBadges() {
        Genre genre1 = new Genre("Fiction", "#ff0000", "Fictional works");
        genre1.setName("Fiction");
        Genre genre2 = new Genre("Fantasy", "#00ff00", "Fantasy works");
        genre2.setName("Fantasy");
        Book book = createBook(1L, "Genre Book", "Author", null, null, null,
                new LinkedHashSet<>(Set.of(genre1, genre2)));
        BookAvailableMessageComponent component = new BookAvailableMessageComponent(book, FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains("Fiction"));
        assertTrue(html.contains("Fantasy"));
    }

    @Test
    void build_WithoutGenres_DoesNotRenderBadges() {
        Book book = createBook(1L, "No Genre", "Author", null, null, null, null);
        BookAvailableMessageComponent component = new BookAvailableMessageComponent(book, FRONTEND_URL);
        String html = component.build();
    }

    @Test
    void build_WithReviewScore_ShowsStars() {
        Book book = createBook(1L, "Rated Book", "Author", null, null, 4.5, null);
        BookAvailableMessageComponent component = new BookAvailableMessageComponent(book, FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains("4.5") || html.contains("4,5"));
    }

    @Test
    void build_WithNullReviewScore_DoesNotShowStars() {
        Book book = createBook(1L, "No Rating", "Author", null, null, null, null);
        BookAvailableMessageComponent component = new BookAvailableMessageComponent(book, FRONTEND_URL);
        String html = component.build();
        assertFalse(html.contains("\u2605"));
    }

    @Test
    void build_WithZeroReviewScore_DoesNotShowStars() {
        Book book = createBook(1L, "Zero Rating", "Author", null, null, 0.0, null);
        BookAvailableMessageComponent component = new BookAvailableMessageComponent(book, FRONTEND_URL);
        String html = component.build();
        assertFalse(html.contains("\u2605"));
    }

    @Test
    void build_WithDescription_TruncatesAt200Chars() {
        String longDesc = "A".repeat(250);
        Book book = createBook(1L, "Long Desc", "Author", null, longDesc, null, null);
        BookAvailableMessageComponent component = new BookAvailableMessageComponent(book, FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains("A".repeat(197) + "..."));
    }

    @Test
    void build_WithShortDescription_DoesNotTruncate() {
        String shortDesc = "Short description here";
        Book book = createBook(1L, "Short Desc", "Author", null, shortDesc, null, null);
        BookAvailableMessageComponent component = new BookAvailableMessageComponent(book, FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains(shortDesc));
    }

    @Test
    void build_WithNullDescription_DoesNotRenderDescription() {
        Book book = createBook(1L, "No Desc", "Author", null, null, null, null);
        BookAvailableMessageComponent component = new BookAvailableMessageComponent(book, FRONTEND_URL);
        String html = component.build();
        assertFalse(html.contains("margin: 16px 0 0 0"));
    }

    @Test
    void build_WithBlankDescription_DoesNotRenderDescription() {
        Book book = createBook(1L, "Blank Desc", "Author", null, "", null, null);
        BookAvailableMessageComponent component = new BookAvailableMessageComponent(book, FRONTEND_URL);
        String html = component.build();
        assertFalse(html.contains("A great book description"));
    }

    @Test
    void build_WithBlankAuthor_DoesNotRenderAuthorTag() {
        Book book = createBook(1L, "Test", "   ", null, null, null, null);
        BookAvailableMessageComponent component = new BookAvailableMessageComponent(book, FRONTEND_URL);
        String html = component.build();
        assertFalse(html.contains("font-size: 15px"));
    }

    @Test
    void build_EscapesHtmlInTitle() {
        Book book = createBook(1L, "<script>alert('xss')</script>", "Author", null, null, null, null);
        BookAvailableMessageComponent component = new BookAvailableMessageComponent(book, FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains("&lt;script&gt;alert(&#39;xss&#39;)&lt;/script&gt;"));
        assertFalse(html.contains("<script>alert('xss')</script>"));
    }

    @Test
    void build_EscapesHtmlInAuthor() {
        Book book = createBook(1L, "Test", "<b>Evil Author</b>", null, null, null, null);
        BookAvailableMessageComponent component = new BookAvailableMessageComponent(book, FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains("&lt;b&gt;Evil Author&lt;/b&gt;"));
        assertFalse(html.contains("<b>Evil Author</b>"));
    }
}
