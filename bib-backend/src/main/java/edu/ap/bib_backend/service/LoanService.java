package edu.ap.bib_backend.service;


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
        try {
            Loan loan = loanMapper.toEntity(loanDTO);

            for (var id : loanDTO.bookIds()) {

                Book book = bookService.getBookById(id, true);
                BookCopy copy = book.getBookCopies().getFirst();

                loan.getCopies().add(copy);
            }

            loan = loanRepository.save(loan);
            return loanMapper.toDto(loan);
        } catch (IllegalStateException e) {
            throw new RuntimeException("Uitlening kon niet worden aangemaakt", e);
        }
    }
}