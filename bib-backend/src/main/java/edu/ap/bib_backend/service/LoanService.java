package edu.ap.bib_backend.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

import org.springframework.stereotype.Service;

import edu.ap.bib_backend.dto.LoanDTO;
import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.entity.BookCopy;
import edu.ap.bib_backend.jpa.entity.Loan;
import edu.ap.bib_backend.jpa.repository.LoanRepository;
import edu.ap.bib_backend.mapper.LoanMapper;
import jakarta.transaction.Transactional;

@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final LoanMapper loanMapper;
    private final BookService bookService;

    public LoanService(LoanRepository loanRepository, LoanMapper loanMapper, BookService bookService) {
        this.loanRepository = loanRepository;
        this.loanMapper = loanMapper;
        this.bookService = bookService;
    }

    @Transactional
    public LoanDTO create(LoanDTO loanDTO) {
        LocalDate today = LocalDate.now(ZoneId.of("Europe/Brussels"));
        LocalDate parsedReturnDate;

        try {
            parsedReturnDate = LocalDate.parse(loanDTO.returnDate());
        } catch (DateTimeParseException e) {
            throw new IllegalStateException("Retourdatum heeft een ongeldig formaat.");
        }

        if (!parsedReturnDate.isAfter(today)) {
            throw new IllegalStateException("Retourdatum moet minimaal één dag in de toekomst liggen.");
        }

        Loan loan = loanMapper.toEntity(loanDTO);
        loan.setReturnDate(parsedReturnDate);

        // saving to ensure we have an ID for the loan before associating book copies
        loan = loanRepository.save(loan);

        for (var id : loanDTO.bookIds()) {

            Book book = bookService.getBookById(id, true);
            BookCopy copy = book.getBookCopies().getFirst();
            loan.getCopies().add(copy);
        }

        loan.setLoanDate(today);
        loan = loanRepository.save(loan);
        return loanMapper.toDto(loan);
    }
}