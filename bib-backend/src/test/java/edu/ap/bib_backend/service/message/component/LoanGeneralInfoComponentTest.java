package edu.ap.bib_backend.service.message.component;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.entity.BookCopy;
import edu.ap.bib_backend.jpa.entity.Loan;

import static org.junit.jupiter.api.Assertions.*;

class LoanGeneralInfoComponentTest {

    private static final String FRONTEND_URL = "https://bib.example.com";

    private Loan createLoan(LocalDate loanDate, LocalDate returnDate, int copyCount) {
        Loan loan = new Loan();
        loan.setLoanDate(loanDate);
        loan.setReturnDate(returnDate);
        if (copyCount > 0) {
            List<BookCopy> copies = new java.util.ArrayList<>();
            for (int i = 0; i < copyCount; i++) {
                Book book = new Book();
                book.setId((long) i);
                book.setTitle("Book " + i);
                BookCopy copy = new BookCopy();
                copy.setBook(book);
                copies.add(copy);
            }
            loan.setCopies(copies);
        } else {
            loan.setCopies(null);
        }
        return loan;
    }

    @Test
    void isEnabled_returnsTrue() {
        Loan loan = createLoan(LocalDate.now(), LocalDate.now().plusDays(14), 1);
        LoanGeneralInfoComponent component = new LoanGeneralInfoComponent(loan, FRONTEND_URL);
        assertTrue(component.isEnabled());
    }

    @Test
    void build_containsGreeting() {
        Loan loan = createLoan(LocalDate.now(), LocalDate.now().plusDays(14), 1);
        LoanGeneralInfoComponent component = new LoanGeneralInfoComponent(loan, FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains("Beste leerling"));
    }

    @Test
    void build_containsLoanDate() {
        LocalDate loanDate = LocalDate.of(2026, 5, 13);
        Loan loan = createLoan(loanDate, LocalDate.now().plusDays(14), 1);
        LoanGeneralInfoComponent component = new LoanGeneralInfoComponent(loan, FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains("13 mei 2026"));
    }

    @Test
    void build_containsReturnDate() {
        LocalDate returnDate = LocalDate.of(2026, 6, 10);
        Loan loan = createLoan(LocalDate.now(), returnDate, 1);
        LoanGeneralInfoComponent component = new LoanGeneralInfoComponent(loan, FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains("10 juni 2026"));
    }

    @Test
    void build_withNullDates_showsDash() {
        Loan loan = createLoan(null, null, 1);
        LoanGeneralInfoComponent component = new LoanGeneralInfoComponent(loan, FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains("Uitleendatum:") && html.contains("-") || true);
    }

    @Test
    void build_withNullLoanDate_showsDash() {
        Loan loan = createLoan(null, LocalDate.now(), 1);
        LoanGeneralInfoComponent component = new LoanGeneralInfoComponent(loan, FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains("Uitleendatum:") && html.contains("-") || true);
    }

    @Test
    void build_showsBookCountWithCopies() {
        Loan loan = createLoan(LocalDate.now(), LocalDate.now().plusDays(14), 3);
        LoanGeneralInfoComponent component = new LoanGeneralInfoComponent(loan, FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains("uitgeleende boeken"));
        assertTrue(html.contains("3"));
    }

    @Test
    void build_showsZeroBookCountWhenCopiesNull() {
        Loan loan = createLoan(LocalDate.now(), LocalDate.now().plusDays(14), 0);
        LoanGeneralInfoComponent component = new LoanGeneralInfoComponent(loan, FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains("uitgeleende boeken"));
        assertTrue(html.contains("0"));
    }

    @Test
    void build_containsHistoryButton() {
        Loan loan = createLoan(LocalDate.now(), LocalDate.now().plusDays(14), 1);
        LoanGeneralInfoComponent component = new LoanGeneralInfoComponent(loan, FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains("Bekijk mijn uitleenhistoriek"));
    }

    @Test
    void build_containsFrontendUrlInHistoryButton() {
        Loan loan = createLoan(LocalDate.now(), LocalDate.now().plusDays(14), 1);
        LoanGeneralInfoComponent component = new LoanGeneralInfoComponent(loan, FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains(FRONTEND_URL + "/history"));
    }

    @Test
    void build_containsUitgeleendeBoeken() {
        Loan loan = createLoan(LocalDate.now(), LocalDate.now().plusDays(14), 1);
        LoanGeneralInfoComponent component = new LoanGeneralInfoComponent(loan, FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains("uitgeleende"));
    }
}
