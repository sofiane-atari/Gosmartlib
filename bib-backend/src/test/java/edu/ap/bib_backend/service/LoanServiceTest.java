package edu.ap.bib_backend.service;

import edu.ap.bib_backend.dto.LoanDTO;
import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.entity.BookCopy;
import edu.ap.bib_backend.jpa.entity.Loan;
import edu.ap.bib_backend.jpa.repository.BookCopyRepository;
import edu.ap.bib_backend.jpa.repository.LoanRepository;
import edu.ap.bib_backend.service.oneroster.ExternalUserDisplayService;
import edu.ap.bib_backend.mapper.LoanMapper;
import edu.ap.bib_backend.service.book.BookService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private LoanMapper loanMapper;

    @Mock
    private BookService bookService;

    @Mock
    private ExternalUserDisplayService externalUserDisplayService;

    @Mock
    private BookCopyRepository bookCopyRepository;

    @InjectMocks
    private LoanService loanService;

    @Test
    void Given_ValidLoanDTO_When_Create_Then_ReturnsCreatedLoanDTO() {
        // Arrange
        String returnDate = LocalDate.now(ZoneId.of("Europe/Brussels")).plusDays(2).toString();
        LoanDTO inputLoan = new LoanDTO(null, "user-1", null, List.of(15L), null, null, returnDate);

        Loan mappedLoan = new Loan();
        LoanDTO expectedResult = new LoanDTO(null, "user-1", null, List.of(15L), null, null, returnDate);

        BookCopy selectedCopy = new BookCopy();
        Book book = new Book();
        book.setBookCopies(new ArrayList<>(List.of(selectedCopy)));

        when(loanMapper.toEntity(inputLoan)).thenReturn(mappedLoan);
        when(loanRepository.save(mappedLoan)).thenReturn(mappedLoan);
        when(bookService.getBookById(15L, true)).thenReturn(book);
        when(bookCopyRepository.findAvailableCopiesByBookId(15L)).thenReturn(List.of(selectedCopy));
        when(loanMapper.toDto(mappedLoan)).thenReturn(expectedResult);

        // Act
        LoanDTO result = loanService.create(inputLoan);

        // Assert
        assertEquals(expectedResult, result);
        assertEquals(LocalDate.parse(returnDate), mappedLoan.getReturnDate());
        assertNotNull(mappedLoan.getLoanDate());
        assertEquals(1, mappedLoan.getCopies().size());
        assertSame(selectedCopy, mappedLoan.getCopies().getFirst());

        verify(loanMapper).toEntity(inputLoan);
        verify(loanRepository, times(2)).save(mappedLoan);
        verify(bookService).getBookById(15L, true);
        verify(bookCopyRepository).findAvailableCopiesByBookId(15L);
        verify(loanMapper).toDto(mappedLoan);
    }

    @Test
    void Given_LoanDTOWithInvalidDateFormat_When_Create_Then_ThrowsIllegalStateException() {
        // Arrange
        LoanDTO inputLoan = new LoanDTO(null, "user-1", null, List.of(15L), null, null, "31-12-2099");

        // Act
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> loanService.create(inputLoan));

        // Assert
        assertEquals("Retourdatum heeft een ongeldig formaat.", exception.getMessage());
        verifyNoInteractions(loanMapper);
        verifyNoInteractions(loanRepository);
        verifyNoInteractions(bookService);
    }

    @Test
    void Given_LoanDTOWithTodayAsReturnDate_When_Create_Then_ThrowsIllegalStateException() {
        // Arrange
        String today = LocalDate.now(ZoneId.of("Europe/Brussels")).toString();
        LoanDTO inputLoan = new LoanDTO(null, "user-1", null, List.of(15L), null, null, today);

        // Act
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> loanService.create(inputLoan));

        // Assert
        assertEquals("Retourdatum moet minimaal één dag in de toekomst liggen.", exception.getMessage());
        verifyNoInteractions(loanMapper);
        verifyNoInteractions(loanRepository);
        verifyNoInteractions(bookService);
    }
}
