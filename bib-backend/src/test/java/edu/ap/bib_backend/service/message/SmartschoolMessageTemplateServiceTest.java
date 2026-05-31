package edu.ap.bib_backend.service.message;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.ap.bib_backend.dto.BookDTO;
import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.entity.BookCopy;
import edu.ap.bib_backend.jpa.entity.Genre;
import edu.ap.bib_backend.jpa.entity.Loan;
import edu.ap.bib_backend.service.TemplateRenderer;

class SmartschoolMessageTemplateServiceTest {

    private static final String FRONTEND_URL = "https://bib.example.com";

    private SmartschoolMessageTemplateService templateService;

    @BeforeEach
    void setUp() {
        templateService = new SmartschoolMessageTemplateService(new TemplateRenderer());
    }

    @Test
    void givenLoanAndRecommendation_whenRenderLoanConfirmation_thenRendersLoanTemplate() {
        // Arrange
        Loan loan = createLoan(createBook(1L, "Test Boek", "Test Auteur"));
        BookDTO recommendation = createBookDto(2L, "Aanrader", "Auteur", 4.5);

        // Act
        String html = templateService.renderLoanConfirmation(List.of(loan), List.of(recommendation), FRONTEND_URL);

        // Assert
        assertTrue(html.contains("Bevestiging nieuwe uitlening"));
        assertTrue(html.contains("Test Boek"));
        assertTrue(html.contains("Test Auteur"));
        assertTrue(html.contains("Aanbevolen boeken"));
        assertTrue(html.contains("Aanrader"));
    }

    @Test
    void givenBookWithRecommendation_whenRenderBookAvailable_thenRendersBookAvailableTemplate() {
        // Arrange
        Book book = createBook(1L, "Beschikbaar Boek", "Auteur Naam");
        book.setDescription("Een korte beschrijving.");
        book.setReviewScore(4.0);

        // Act
        String html = templateService.renderBookAvailable(book, List.of(createBookDto(2L, "Suggestie", "Auteur", 0)), FRONTEND_URL);

        // Assert
        assertTrue(html.contains("Boek beschikbaar"));
        assertTrue(html.contains("opnieuw beschikbaar"));
        assertTrue(html.contains("Beschikbaar Boek"));
        assertTrue(html.contains("Bekijk in catalogus"));
        assertTrue(html.contains("Suggestie"));
    }

    @Test
    void givenBookWithHtmlInTitle_whenRenderLoanBookList_thenEscapesText() {
        // Arrange
        Loan loan = createLoan(createBook(1L, "<script>alert(1)</script>", "Auteur & Co"));

        // Act
        String html = templateService.renderLoanBookList(List.of(loan), FRONTEND_URL);

        // Assert
        assertTrue(html.contains("&lt;script&gt;alert(1)&lt;/script&gt;"));
        assertTrue(html.contains("Auteur &amp; Co"));
        assertFalse(html.contains("<script>alert(1)</script>"));
    }

    @Test
    void givenSevenRecommendations_whenRenderLoanConfirmation_thenShowsMaximumSixRecommendations() {
        // Arrange
        Loan loan = createLoan(createBook(1L, "Boek", "Auteur"));
        List<BookDTO> recommendations = java.util.stream.IntStream.rangeClosed(1, 7)
                .mapToObj(index -> createBookDto((long) index, "Suggestie " + index, "Auteur", 0))
                .toList();

        // Act
        String html = templateService.renderLoanConfirmation(List.of(loan), recommendations, FRONTEND_URL);

        // Assert
        assertTrue(html.contains("Suggestie 6"));
        assertFalse(html.contains("Suggestie 7"));
    }

    @Test
    void givenBookWithGenres_whenRenderLoanBookList_thenRendersGenreBadges() {
        // Arrange
        Genre genre = new Genre();
        genre.setName("Fantasy");
        Book book = createBook(1L, "Genre Boek", "Auteur");
        book.setGenres(new LinkedHashSet<>(Set.of(genre)));
        Loan loan = createLoan(book);

        // Act
        String html = templateService.renderLoanBookList(List.of(loan), FRONTEND_URL);

        // Assert
        assertTrue(html.contains("Fantasy"));
        assertTrue(html.contains("border-radius: 10px"));
    }

    private Loan createLoan(Book book) {
        BookCopy copy = new BookCopy();
        copy.setBook(book);

        Loan loan = new Loan();
        loan.setLoanDate(LocalDate.of(2026, 5, 1));
        loan.setReturnDate(LocalDate.of(2026, 5, 15));
        loan.setCopy(copy);
        return loan;
    }

    private Book createBook(Long id, String title, String author) {
        Book book = new Book();
        book.setId(id);
        book.setTitle(title);
        book.setAuthor(author);
        return book;
    }

    private BookDTO createBookDto(Long id, String title, String author, double reviewScore) {
        BookDTO book = new BookDTO();
        book.setId(id);
        book.setTitle(title);
        book.setAuthor(author);
        book.setReviewScore(reviewScore);
        return book;
    }
}
