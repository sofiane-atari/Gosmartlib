package edu.ap.bib_backend.service.message.component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.entity.BookCopy;
import edu.ap.bib_backend.jpa.entity.Genre;
import edu.ap.bib_backend.jpa.entity.Loan;

import static org.junit.jupiter.api.Assertions.*;

class LoanBookListComponentTest {

    private static final String FRONTEND_URL = "https://bib.example.com";

    private Book createBook(Long id, String title, String author, String imageUrl, Set<Genre> genres) {
        Book book = new Book();
        book.setId(id);
        book.setTitle(title);
        book.setAuthor(author);
        book.setImageUrl(imageUrl);
        book.setGenres(genres);
        return book;
    }

    private BookCopy createBookCopy(Book book) {
        BookCopy copy = new BookCopy();
        copy.setBook(book);
        return copy;
    }

    private Loan createLoanWithCopies(List<BookCopy> copies) {
        Loan loan = new Loan();
        loan.setCopies(copies);
        return loan;
    }

    @Test
    void isEnabled_withNullCopies_returnsFalse() {
        Loan loan = createLoanWithCopies(null);
        LoanBookListComponent component = new LoanBookListComponent(loan, FRONTEND_URL);
        assertFalse(component.isEnabled());
    }

    @Test
    void isEnabled_withEmptyCopies_returnsFalse() {
        Loan loan = createLoanWithCopies(List.of());
        LoanBookListComponent component = new LoanBookListComponent(loan, FRONTEND_URL);
        assertFalse(component.isEnabled());
    }

    @Test
    void isEnabled_withCopies_returnsTrue() {
        Book book = createBook(1L, "Test Book", "Author", null, null);
        Loan loan = createLoanWithCopies(List.of(createBookCopy(book)));
        LoanBookListComponent component = new LoanBookListComponent(loan, FRONTEND_URL);
        assertTrue(component.isEnabled());
    }

    @Test
    void build_containsTitle() {
        Book book = createBook(1L, "Test Book", "Author", null, null);
        Loan loan = createLoanWithCopies(List.of(createBookCopy(book)));
        LoanBookListComponent component = new LoanBookListComponent(loan, FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains("Test Book"));
    }

    @Test
    void build_containsAuthor() {
        Book book = createBook(1L, "Test Book", "J.K. Rowling", null, null);
        Loan loan = createLoanWithCopies(List.of(createBookCopy(book)));
        LoanBookListComponent component = new LoanBookListComponent(loan, FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains("J.K. Rowling"));
    }

    @Test
    void build_containsBookLink() {
        Book book = createBook(42L, "Linked Book", "Author", null, null);
        Loan loan = createLoanWithCopies(List.of(createBookCopy(book)));
        LoanBookListComponent component = new LoanBookListComponent(loan, FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains(FRONTEND_URL + "/book/42"));
    }

    @Test
    void build_withImageUrl_rendersImgTag() {
        Book book = createBook(1L, "With Image", "Author", "https://example.com/cover.jpg", null);
        Loan loan = createLoanWithCopies(List.of(createBookCopy(book)));
        LoanBookListComponent component = new LoanBookListComponent(loan, FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains("<img"));
        assertTrue(html.contains("https://example.com/cover.jpg"));
    }

    @Test
    void build_withoutImageUrl_rendersPlaceholder() {
        Book book = createBook(1L, "No Image", "Author", null, null);
        Loan loan = createLoanWithCopies(List.of(createBookCopy(book)));
        LoanBookListComponent component = new LoanBookListComponent(loan, FRONTEND_URL);
        String html = component.build();
        assertFalse(html.contains("<img"));
    }

    @Test
    void build_withEmptyImageUrl_rendersPlaceholder() {
        Book book = createBook(1L, "Empty Image", "Author", "", null);
        Loan loan = createLoanWithCopies(List.of(createBookCopy(book)));
        LoanBookListComponent component = new LoanBookListComponent(loan, FRONTEND_URL);
        String html = component.build();
        assertFalse(html.contains("<img"));
    }

    @Test
    void build_withGenres_rendersGenreBadges() {
        Genre genre1 = new Genre("Fiction", "#ff0000", "Fictional works");
        genre1.setName("Fiction");
        Genre genre2 = new Genre("Fantasy", "#00ff00", "Fantasy works");
        genre2.setName("Fantasy");
        Book book = createBook(1L, "Genre Book", "Author", null, new LinkedHashSet<>(Set.of(genre1, genre2)));
        Loan loan = createLoanWithCopies(List.of(createBookCopy(book)));
        LoanBookListComponent component = new LoanBookListComponent(loan, FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains("Fiction"));
        assertTrue(html.contains("Fantasy"));
    }

    @Test
    void build_withoutGenres_doesNotRenderBadges() {
        Book book = createBook(1L, "No Genre", "Author", null, null);
        Loan loan = createLoanWithCopies(List.of(createBookCopy(book)));
        LoanBookListComponent component = new LoanBookListComponent(loan, FRONTEND_URL);
        String html = component.build();
    }

    @Test
    void build_withMultipleCopiesOfSameBook_showsCount() {
        Book book = createBook(1L, "Popular Book", "Author", null, null);
        BookCopy copy1 = createBookCopy(book);
        BookCopy copy2 = createBookCopy(book);
        Loan loan = createLoanWithCopies(List.of(copy1, copy2));
        LoanBookListComponent component = new LoanBookListComponent(loan, FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains("2 exemplaren"));
    }

    @Test
    void build_withSingleCopy_doesNotShowCount() {
        Book book = createBook(1L, "Solo Book", "Author", null, null);
        Loan loan = createLoanWithCopies(List.of(createBookCopy(book)));
        LoanBookListComponent component = new LoanBookListComponent(loan, FRONTEND_URL);
        String html = component.build();
        assertFalse(html.contains("exemplaren"));
    }

    @Test
    void build_containsUitgeleendeBoekenHeading() {
        Book book = createBook(1L, "Any", "Author", null, null);
        Loan loan = createLoanWithCopies(List.of(createBookCopy(book)));
        LoanBookListComponent component = new LoanBookListComponent(loan, FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains("Uitgeleende boeken"));
    }

    @Test
    void build_containsMsgBookCardClass() {
        Book book = createBook(1L, "Card Test", "Author", null, null);
        Loan loan = createLoanWithCopies(List.of(createBookCopy(book)));
        LoanBookListComponent component = new LoanBookListComponent(loan, FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains("msg-book-card"));
    }

    @Test
    void build_containsMsgCoverLinkClass() {
        Book book = createBook(1L, "Cover Test", "Author", null, null);
        Loan loan = createLoanWithCopies(List.of(createBookCopy(book)));
        LoanBookListComponent component = new LoanBookListComponent(loan, FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains("msg-cover-link"));
    }

    @Test
    void build_containsMsgBookLinkClass() {
        Book book = createBook(1L, "Link Test", "Author", null, null);
        Loan loan = createLoanWithCopies(List.of(createBookCopy(book)));
        LoanBookListComponent component = new LoanBookListComponent(loan, FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains("msg-book-link"));
    }

    @Test
    void build_withAllNullCopies_returnsEmpty() {
        List<BookCopy> copies = new ArrayList<>();
        copies.add(null);
        Loan loan = createLoanWithCopies(copies);
        LoanBookListComponent component = new LoanBookListComponent(loan, FRONTEND_URL);
        String html = component.build();
        assertEquals("", html);
    }

    @Test
    void build_escapesHtmlInTitle() {
        Book book = createBook(1L, "<script>alert('xss')</script>", "Author", null, null);
        Loan loan = createLoanWithCopies(List.of(createBookCopy(book)));
        LoanBookListComponent component = new LoanBookListComponent(loan, FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains("&lt;script&gt;alert(&#39;xss&#39;)&lt;/script&gt;"));
        assertFalse(html.contains("<script>"));
    }
}
