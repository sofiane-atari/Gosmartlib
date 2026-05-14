package edu.ap.bib_backend.service.message.component;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import edu.ap.bib_backend.dto.BookDTO;

import static org.junit.jupiter.api.Assertions.*;

class BookRecommendationComponentTest {

    private static final String FRONTEND_URL = "https://bib.example.com";

    private BookDTO createBookDTO(Long id, String title, String author, String imageUrl, double reviewScore) {
        BookDTO book = new BookDTO();
        book.setId(id);
        book.setTitle(title);
        book.setAuthor(author);
        book.setImageUrl(imageUrl);
        book.setReviewScore(reviewScore);
        return book;
    }

    @Test
    void isEnabled_withNullList_returnsFalse() {
        BookRecommendationComponent component = new BookRecommendationComponent(null, FRONTEND_URL);
        assertFalse(component.isEnabled());
    }

    @Test
    void isEnabled_withEmptyList_returnsFalse() {
        BookRecommendationComponent component = new BookRecommendationComponent(List.of(), FRONTEND_URL);
        assertFalse(component.isEnabled());
    }

    @Test
    void isEnabled_withNonEmptyList_returnsTrue() {
        BookDTO book = createBookDTO(1L, "Test", "Author", null, 0);
        BookRecommendationComponent component = new BookRecommendationComponent(List.of(book), FRONTEND_URL);
        assertTrue(component.isEnabled());
    }

    @Test
    void build_containsAanbevolenBoekenHeading() {
        BookDTO book = createBookDTO(1L, "Test", "Author", null, 0);
        BookRecommendationComponent component = new BookRecommendationComponent(List.of(book), FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains("Aanbevolen boeken"));
    }

    @Test
    void build_containsTitle() {
        BookDTO book = createBookDTO(1L, "Recommended Book", "Author", null, 0);
        BookRecommendationComponent component = new BookRecommendationComponent(List.of(book), FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains("Recommended Book"));
    }

    @Test
    void build_containsAuthor() {
        BookDTO book = createBookDTO(1L, "Test", "J.K. Rowling", null, 0);
        BookRecommendationComponent component = new BookRecommendationComponent(List.of(book), FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains("J.K. Rowling"));
    }

    @Test
    void build_containsBookLink() {
        BookDTO book = createBookDTO(42L, "Linked", "Author", null, 0);
        BookRecommendationComponent component = new BookRecommendationComponent(List.of(book), FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains(FRONTEND_URL + "/book/42"));
    }

    @Test
    void build_withImageUrl_rendersImgTag() {
        BookDTO book = createBookDTO(1L, "With Image", "Author", "https://example.com/cover.jpg", 0);
        BookRecommendationComponent component = new BookRecommendationComponent(List.of(book), FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains("<img"));
        assertTrue(html.contains("https://example.com/cover.jpg"));
    }

    @Test
    void build_withoutImageUrl_rendersPlaceholder() {
        BookDTO book = createBookDTO(1L, "No Image", "Author", null, 0);
        BookRecommendationComponent component = new BookRecommendationComponent(List.of(book), FRONTEND_URL);
        String html = component.build();
        assertFalse(html.contains("<img"));
    }

    @Test
    void build_withEmptyImageUrl_rendersPlaceholder() {
        BookDTO book = createBookDTO(1L, "Empty Image", "Author", "", 0);
        BookRecommendationComponent component = new BookRecommendationComponent(List.of(book), FRONTEND_URL);
        String html = component.build();
        assertFalse(html.contains("<img"));
    }

    @Test
    void build_withReviewScore_showsStars() {
        BookDTO book = createBookDTO(1L, "Rated", "Author", null, 4.5);
        BookRecommendationComponent component = new BookRecommendationComponent(List.of(book), FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains("4,5") || html.contains("4.5"));
    }

    @Test
    void build_withZeroReviewScore_doesNotShowStars() {
        BookDTO book = createBookDTO(1L, "No Rating", "Author", null, 0);
        BookRecommendationComponent component = new BookRecommendationComponent(List.of(book), FRONTEND_URL);
        String html = component.build();
        assertFalse(html.contains("★"));
    }

    @Test
    void build_limitsToSixBooks() {
        List<BookDTO> books = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            books.add(createBookDTO((long) i, "Book " + i, "Author", null, 0));
        }
        BookRecommendationComponent component = new BookRecommendationComponent(books, FRONTEND_URL);
        String html = component.build();

        for (int i = 1; i <= 6; i++) {
            assertTrue(html.contains("Book " + i), "Book " + i + " should appear");
        }
        for (int i = 7; i <= 10; i++) {
            assertFalse(html.contains("Book " + i), "Book " + i + " should NOT appear");
        }
    }

    @Test
    void build_containsMsgBookCardClass() {
        BookDTO book = createBookDTO(1L, "Card", "Author", null, 0);
        BookRecommendationComponent component = new BookRecommendationComponent(List.of(book), FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains("msg-book-card"));
    }

    @Test
    void build_containsMsgBookLinkClass() {
        BookDTO book = createBookDTO(1L, "Link", "Author", null, 0);
        BookRecommendationComponent component = new BookRecommendationComponent(List.of(book), FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains("msg-book-link"));
    }

    @Test
    void build_containsMsgCoverLinkClass() {
        BookDTO book = createBookDTO(1L, "Cover", "Author", null, 0);
        BookRecommendationComponent component = new BookRecommendationComponent(List.of(book), FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains("msg-cover-link"));
    }

    @Test
    void build_containsSuggestieText() {
        BookDTO book = createBookDTO(1L, "Test", "Author", null, 0);
        BookRecommendationComponent component = new BookRecommendationComponent(List.of(book), FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains("suggesties"));
    }

    @Test
    void build_escapesHtmlInTitle() {
        BookDTO book = createBookDTO(1L, "<b>Bold</b>", "Author", null, 0);
        BookRecommendationComponent component = new BookRecommendationComponent(List.of(book), FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains("&lt;b&gt;Bold&lt;/b&gt;"));
        assertFalse(html.contains("<b>Bold</b>"));
    }
}
