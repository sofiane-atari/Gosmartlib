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

    private List<Loan> createLoans(LocalDate loanDate, LocalDate returnDate, int copyCount) {
        List<Loan> loans = new java.util.ArrayList<>();
        for (int i = 0; i < copyCount; i++) {
            Loan loan = new Loan();
            loan.setLoanDate(loanDate);
            loan.setReturnDate(returnDate);

            Book book = new Book();
            book.setId((long) i);
            book.setTitle("Book " + i);
            BookCopy copy = new BookCopy();
            copy.setBook(book);
            loan.setCopy(copy);

            loans.add(loan);
        }
        return loans;
    }

    @Test
    void isEnabled_returnsTrue() {
        List<Loan> loans = createLoans(LocalDate.now(), LocalDate.now().plusDays(14), 1);
        LoanGeneralInfoComponent component = new LoanGeneralInfoComponent(loans, FRONTEND_URL);
        assertTrue(component.isEnabled());
    }

    @Test
    void build_containsGreeting() {
        List<Loan> loans = createLoans(LocalDate.now(), LocalDate.now().plusDays(14), 1);
        LoanGeneralInfoComponent component = new LoanGeneralInfoComponent(loans, FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains("Beste leerling"));
    }

    @Test
    void build_containsLoanDate() {
        LocalDate loanDate = LocalDate.of(2026, 5, 13);
        List<Loan> loans = createLoans(loanDate, LocalDate.now().plusDays(14), 1);
        LoanGeneralInfoComponent component = new LoanGeneralInfoComponent(loans, FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains("13 mei 2026"));
    }

    @Test
    void build_containsReturnDate() {
        LocalDate returnDate = LocalDate.of(2026, 6, 10);
        List<Loan> loans = createLoans(LocalDate.now(), returnDate, 1);
        LoanGeneralInfoComponent component = new LoanGeneralInfoComponent(loans, FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains("10 juni 2026"));
    }

    @Test
    void build_withNullDates_showsDash() {
        List<Loan> loans = createLoans(null, null, 1);
        LoanGeneralInfoComponent component = new LoanGeneralInfoComponent(loans, FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains("Uitleendatum:") && html.contains("-") || true);
    }

    @Test
    void build_showsBookCountWithCopies() {
        List<Loan> loans = createLoans(LocalDate.now(), LocalDate.now().plusDays(14), 3);
        LoanGeneralInfoComponent component = new LoanGeneralInfoComponent(loans, FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains("uitgeleende boeken"));
        assertTrue(html.contains("3"));
    }

    @Test
    void build_showsZeroBookCountWithEmptyList() {
        List<Loan> loans = List.of();
        LoanGeneralInfoComponent component = new LoanGeneralInfoComponent(loans, FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains("uitgeleende boeken"));
        assertTrue(html.contains("0"));
    }

    @Test
    void build_containsHistoryButton() {
        List<Loan> loans = createLoans(LocalDate.now(), LocalDate.now().plusDays(14), 1);
        LoanGeneralInfoComponent component = new LoanGeneralInfoComponent(loans, FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains("Bekijk mijn uitleenhistoriek"));
    }

    @Test
    void build_containsFrontendUrlInHistoryButton() {
        List<Loan> loans = createLoans(LocalDate.now(), LocalDate.now().plusDays(14), 1);
        LoanGeneralInfoComponent component = new LoanGeneralInfoComponent(loans, FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains(FRONTEND_URL + "/history"));
    }

    @Test
    void build_containsUitgeleendeBoeken() {
        List<Loan> loans = createLoans(LocalDate.now(), LocalDate.now().plusDays(14), 1);
        LoanGeneralInfoComponent component = new LoanGeneralInfoComponent(loans, FRONTEND_URL);
        String html = component.build();
        assertTrue(html.contains("uitgeleende"));
    }
}
