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

    private List<Loan> createLoans(List<BookCopy> copies) {
        if (copies == null) {
            return null;
        }

        List<Loan> loans = new ArrayList<>();
        for (BookCopy copy : copies) {
            Loan loan = new Loan();
            loan.setCopy(copy);
            loans.add(loan);
        }
        return loans;
    }

    @Test
    void isEnabled_withNullList_returnsFalse() {
        LoanBookListComponent component = new LoanBookListComponent(null, FRONTEND_URL);
        assertFalse(component.isEnabled());
    }

    @Test
    void isEnabled_withEmptyList_returnsFalse() {
        LoanBookListComponent component = new LoanBookListComponent(List.of(), FRONTEND_URL);
        assertFalse(component.isEnabled());
    }

    @Test
    void isEnabled_withLoans_returnsTrue() {
        Book book = createBook(1L, "Test Book", "Author", null, null);
        LoanBookListComponent component = new LoanBookListComponent(
                createLoans(List.of(createBookCopy(book))), FRONTEND_URL);
        assertTrue(component.isEnabled());
    }

    @Test
    void build_containsTitle() {
        Book book = createBook(1L, "Test Book", "Author", null, null);
        LoanBookListComponent component = new LoanBookListComponent(
                createLoans(List.of(createBookCopy(book))), FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains("Test Book"));
    }

    @Test
    void build_containsAuthor() {
        Book book = createBook(1L, "Test Book", "J.K. Rowling", null, null);
        LoanBookListComponent component = new LoanBookListComponent(
                createLoans(List.of(createBookCopy(book))), FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains("J.K. Rowling"));
    }

    @Test
    void build_containsBookLink() {
        Book book = createBook(42L, "Linked Book", "Author", null, null);
        LoanBookListComponent component = new LoanBookListComponent(
                createLoans(List.of(createBookCopy(book))), FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains(FRONTEND_URL + "/book/42"));
    }

    @Test
    void build_withImageUrl_rendersImgTag() {
        Book book = createBook(1L, "With Image", "Author", "https://example.com/cover.jpg", null);
        LoanBookListComponent component = new LoanBookListComponent(
                createLoans(List.of(createBookCopy(book))), FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains("<img"));
        assertTrue(html.contains("https://example.com/cover.jpg"));
    }

    @Test
    void build_withoutImageUrl_rendersPlaceholder() {
        Book book = createBook(1L, "No Image", "Author", null, null);
        LoanBookListComponent component = new LoanBookListComponent(
                createLoans(List.of(createBookCopy(book))), FRONTEND_URL);
        String html = component.build();
        assertFalse(html.contains("<img"));
    }

    @Test
    void build_withEmptyImageUrl_rendersPlaceholder() {
        Book book = createBook(1L, "Empty Image", "Author", "", null);
        LoanBookListComponent component = new LoanBookListComponent(
                createLoans(List.of(createBookCopy(book))), FRONTEND_URL);
        String html = component.build();
        assertFalse(html.contains("<img"));
    }

    @Test
    void build_withGenres_rendersGenreBadges() {
        Set<Genre> genres = new LinkedHashSet<>();
        Genre genre = new Genre();
        genre.setName("Fantasy");
        genre.setColor("#8E44AD");
        genres.add(genre);
        Book book = createBook(1L, "Fantasy Book", "Author", null, genres);
        LoanBookListComponent component = new LoanBookListComponent(
                createLoans(List.of(createBookCopy(book))), FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains("Fantasy"));
    }

    @Test
    void build_containsUitgeleendeBoeken() {
        Book book = createBook(1L, "Test Book", "Author", null, null);
        LoanBookListComponent component = new LoanBookListComponent(
                createLoans(List.of(createBookCopy(book))), FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains("Uitgeleende boeken"));
    }

    @Test
    void build_htmlEncodingEscapesTitle() {
        Book book = createBook(1L, "Test & Book", "Author <script>", null, null);
        LoanBookListComponent component = new LoanBookListComponent(
                createLoans(List.of(createBookCopy(book))), FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains("Test &amp; Book"));
    }
}
